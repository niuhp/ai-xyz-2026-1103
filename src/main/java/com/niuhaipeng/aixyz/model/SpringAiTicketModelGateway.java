package com.niuhaipeng.aixyz.model;

import com.niuhaipeng.aixyz.api.TokenUsageDTO;
import com.niuhaipeng.aixyz.service.ProcessingError;
import com.niuhaipeng.aixyz.service.TicketProcessingException;
import com.openai.errors.OpenAIException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
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
public class SpringAiTicketModelGateway implements TicketModelGateway {

    private static final String FINISH_REASON_STOP = "stop";

    /** 模型客户端。 */
    private final ChatClient chatClient;

    /** 不包含用户文本的固定系统提示。 */
    private final String systemPrompt;

    /** 严格读取整个 JSON，拒绝多余内容和未知字段。 */
    private final JsonMapper jsonMapper;

    public SpringAiTicketModelGateway(ChatClient ticketChatClient, String ticketSystemPrompt, JsonMapper jsonMapper) {
        this.chatClient = ticketChatClient;
        this.jsonMapper = jsonMapper;
        this.systemPrompt = ticketSystemPrompt + "\n输出结构（JSON Schema）：\n"
                + new BeanOutputConverter<>(ModelExtractionDTO.class).getJsonSchema();
    }

    @Override
    public ExtractionResult extract(String description) {
        ChatResponse response;
        try {
            // 使用消息对象直接传递原文，避免描述中的花括号被模板引擎解释。
            response = chatClient.prompt()
                    .messages(new SystemMessage(systemPrompt), new UserMessage(description))
                    .call().chatResponse();
        } catch (OpenAIException exception) {
            throw new TicketProcessingException(isTimeout(exception)
                    ? ProcessingError.MODEL_TIMEOUT : ProcessingError.MODEL_UNAVAILABLE);
        }
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new TicketProcessingException(ProcessingError.INVALID_MODEL_OUTPUT);
        }
        String finishReason = response.getResult().getMetadata().getFinishReason();
        String content = response.getResult().getOutput().getText();
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
        result.setModel(response.getMetadata().getModel());
        result.setUsage(toUsage(response.getMetadata().getUsage()));
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

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
