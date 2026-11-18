package com.niuhp.aixyz.model;

import com.niuhp.aixyz.dto.response.TokenUsageDTO;
import com.niuhp.aixyz.exception.ProcessingError;
import com.niuhp.aixyz.exception.TicketProcessingException;
import com.niuhp.aixyz.knowledge.CaseMatchDTO;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIInvalidDataException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * 教学版聊天模型重排；独立单次超时、不自动重试、不执行候选中的指令。
 *
 * @author niuhaipeng
 */
@Component
public class ChatCaseRerankGateway implements CaseRerankGateway {

    private static final int MAX_OUTPUT_LENGTH = 12000;
    private final ChatClient client;
    private final ModelFailureClassifier classifier;
    private final JsonMapper json;
    private final String prompt;

    public ChatCaseRerankGateway(@Qualifier("caseRerankChatClient") ChatClient client,
            ModelFailureClassifier classifier, JsonMapper json) throws IOException {
        this.client = client;
        this.classifier = classifier;
        this.json = json;
        this.prompt = new ClassPathResource("prompts/case-rerank.txt").getContentAsString(StandardCharsets.UTF_8)
                + "\nJSON Schema:\n" + new BeanOutputConverter<>(ModelRerankDTO.class).getJsonSchema();
    }

    @Override
    public RerankModelResultDTO assess(String description, List<CaseMatchDTO> candidates) {
        // 不发送原始检索分数，避免模型把 RRF 值误当成相关性概率。
        String input = json.writeValueAsString(Map.of("description", description, "candidates",
                candidates.stream().map(CaseMatchDTO::getDocument).toList()));
        ChatResponse response;
        try {
            response = client.prompt().messages(new SystemMessage(prompt), new UserMessage(input))
                    .call().chatResponse();
        } catch (OpenAIInvalidDataException exception) {
            throw new TicketProcessingException(ProcessingError.INVALID_RERANK_OUTPUT);
        } catch (OpenAIException exception) {
            throw new TicketProcessingException(classifier.classify(exception));
        } catch (NumberFormatException exception) {
            throw new TicketProcessingException(ProcessingError.MODEL_UNAVAILABLE);
        }
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null
                || !"stop".equalsIgnoreCase(response.getResult().getMetadata().getFinishReason())) {
            throw new TicketProcessingException(ProcessingError.INVALID_RERANK_OUTPUT);
        }
        String content = response.getResult().getOutput().getText();
        if (content == null || content.length() > MAX_OUTPUT_LENGTH) {
            throw new TicketProcessingException(ProcessingError.INVALID_RERANK_OUTPUT);
        }
        RerankModelResultDTO result = new RerankModelResultDTO();
        try {
            result.setAssessment(json.readValue(content, ModelRerankDTO.class));
        } catch (JacksonException exception) {
            throw new TicketProcessingException(ProcessingError.INVALID_RERANK_OUTPUT);
        }
        result.setModel(response.getMetadata().getModel());
        var usage = response.getMetadata().getUsage();
        if (usage != null && usage.getNativeUsage() != null) {
            TokenUsageDTO tokens = new TokenUsageDTO();
            tokens.setInputTokens(usage.getPromptTokens());
            tokens.setOutputTokens(usage.getCompletionTokens());
            tokens.setTotalTokens(usage.getTotalTokens());
            result.setUsage(tokens);
        }
        return result;
    }
}
