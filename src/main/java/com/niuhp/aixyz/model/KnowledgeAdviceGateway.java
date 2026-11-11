package com.niuhp.aixyz.model;

import com.niuhp.aixyz.dto.response.TokenUsageDTO;
import com.niuhp.aixyz.exception.ProcessingError;
import com.niuhp.aixyz.exception.TicketProcessingException;
import com.niuhp.aixyz.knowledge.AdviceMode;
import com.niuhp.aixyz.knowledge.CaseMatchDTO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * 将召回资料作为数据传递，复用既有超时、故障分类与有界重试策略。
 *
 * @author niuhaipeng
 */
@Component
public class KnowledgeAdviceGateway {

    private static final int MAX_OUTPUT_LENGTH = 12000;
    private final ChatClient chatClient;
    private final ModelCallExecutor executor;
    private final JsonMapper jsonMapper;
    private final String systemPrompt;

    public KnowledgeAdviceGateway(ChatClient ticketChatClient, ModelCallExecutor executor,
            JsonMapper jsonMapper) throws IOException {
        this.chatClient = ticketChatClient;
        this.executor = executor;
        this.jsonMapper = jsonMapper;
        this.systemPrompt = new ClassPathResource("prompts/knowledge-advice.txt")
                .getContentAsString(StandardCharsets.UTF_8) + "\nJSON Schema:\n"
                + new BeanOutputConverter<>(ModelAdviceDTO.class).getJsonSchema();
    }

    public AdviceModelResult generate(String description, AdviceMode mode, List<CaseMatchDTO> matches) {
        String input = jsonMapper.writeValueAsString(Map.of("task", "knowledge-advice", "mode", mode,
                "description", description, "documents", matches.stream().map(CaseMatchDTO::getDocument).toList()));
        ChatResponse response = executor.call(() -> chatClient.prompt()
                .messages(new SystemMessage(systemPrompt), new UserMessage(input)).call().chatResponse());
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null
                || !"stop".equalsIgnoreCase(response.getResult().getMetadata().getFinishReason())) {
            throw new TicketProcessingException(ProcessingError.INVALID_ADVICE_OUTPUT);
        }
        String content = response.getResult().getOutput().getText();
        if (content == null || content.length() > MAX_OUTPUT_LENGTH) {
            throw new TicketProcessingException(ProcessingError.INVALID_ADVICE_OUTPUT);
        }
        ModelAdviceDTO advice;
        try {
            advice = jsonMapper.readValue(content, ModelAdviceDTO.class);
        } catch (JacksonException exception) {
            throw new TicketProcessingException(ProcessingError.INVALID_ADVICE_OUTPUT);
        }
        TokenUsageDTO usageDto = null;
        Usage usage = response.getMetadata().getUsage();
        if (usage != null && usage.getNativeUsage() != null) {
            usageDto = new TokenUsageDTO();
            usageDto.setInputTokens(usage.getPromptTokens());
            usageDto.setOutputTokens(usage.getCompletionTokens());
            usageDto.setTotalTokens(usage.getTotalTokens());
        }
        return new AdviceModelResult(advice, response.getMetadata().getModel(), usageDto);
    }
}
