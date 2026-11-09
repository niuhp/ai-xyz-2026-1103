package com.niuhp.aixyz.model;

import com.niuhp.aixyz.api.TokenUsageDTO;
import com.niuhp.aixyz.service.ProcessingError;
import com.niuhp.aixyz.service.TicketProcessingException;
import reactor.core.publisher.Flux;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI 的 OpenAI 兼容接口适配器。
 *
 * @author niuhaipeng
 */
@Component
public class SpringAiTicketModelGateway implements TicketModelGateway, TicketStreamingModelGateway {

    private static final String FINISH_REASON_STOP = "stop";

    /** 模型客户端。 */
    private final ChatClient chatClient;

    /** 不包含用户文本的固定系统提示。 */
    private final String systemPrompt;

    /** 严格读取整个 JSON，拒绝多余内容和未知字段。 */
    private final JsonMapper jsonMapper;

    private final ModelCallExecutor modelCallExecutor;

    private final ChatClient streamingChatClient;

    public SpringAiTicketModelGateway(ChatClient ticketChatClient, String ticketSystemPrompt, JsonMapper jsonMapper,
            ModelCallExecutor modelCallExecutor) {
        this(ticketChatClient, ticketChatClient, ticketSystemPrompt, jsonMapper, modelCallExecutor);
    }

    @Autowired
    public SpringAiTicketModelGateway(ChatClient ticketChatClient,
            @Qualifier("ticketStreamChatClient") ChatClient streamingChatClient, String ticketSystemPrompt,
            JsonMapper jsonMapper, ModelCallExecutor modelCallExecutor) {
        this.streamingChatClient = streamingChatClient;
        this.modelCallExecutor = modelCallExecutor;
        this.chatClient = ticketChatClient;
        this.jsonMapper = jsonMapper;
        this.systemPrompt = ticketSystemPrompt + "\n输出结构（JSON Schema）：\n"
                + new BeanOutputConverter<>(ModelExtractionDTO.class).getJsonSchema();
    }

    @Override
    public ExtractionResult extract(String description) {
        // 只对模型生成阶段使用重试，工单写入不经过这层。
        ChatResponse response = modelCallExecutor.call(() -> chatClient.prompt()
                .messages(new SystemMessage(systemPrompt), new UserMessage(description))
                .call().chatResponse());
        if (response == null || response.getResult() == null) {
            throw new TicketProcessingException(ProcessingError.INVALID_MODEL_OUTPUT);
        }
        String finishReason = response.getResult().getMetadata().getFinishReason();
        String content = response.getResult().getOutput().getText();
        return parse(content, finishReason, response.getMetadata().getModel(),
                toUsage(response.getMetadata().getUsage()));
    }

    @Override
    public Flux<ChatResponse> stream(String description) {
        // 流式请求不经过 ModelCallExecutor，也不挂载 retry 操作符。
        return streamingChatClient.prompt()
                .messages(new SystemMessage(systemPrompt), new UserMessage(description))
                .stream().chatResponse();
    }

    @Override
    public ExtractionResult parse(String content, String finishReason, String model, TokenUsageDTO usage) {
        if (!FINISH_REASON_STOP.equalsIgnoreCase(finishReason) || !StringUtils.hasText(content)) {
            throw new TicketProcessingException(ProcessingError.INVALID_MODEL_OUTPUT);
        }
        ModelExtractionDTO extraction;
        try {
            extraction = jsonMapper.readValue(content, ModelExtractionDTO.class);
        } catch (JacksonException exception) {
            throw new TicketProcessingException(ProcessingError.INVALID_MODEL_OUTPUT);
        }
        ExtractionResult result = new ExtractionResult();
        result.setExtraction(extraction);
        result.setModel(model);
        result.setUsage(usage);
        return result;
    }

    private TokenUsageDTO toUsage(Usage usage) {
        if (usage == null || usage.getNativeUsage() == null) {
            return null;
        }
        TokenUsageDTO result = new TokenUsageDTO();
        result.setInputTokens(usage.getPromptTokens());
        result.setOutputTokens(usage.getCompletionTokens());
        result.setTotalTokens(usage.getTotalTokens());
        return result;
    }

}
