package com.niuhp.aixyz.model;

import com.niuhp.aixyz.api.TokenUsageDTO;
import com.niuhp.aixyz.service.ProcessingError;
import com.niuhp.aixyz.service.TicketProcessingException;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.util.StringUtils;

/**
 * 每次请求独享的增量聚合器，不把半截 JSON 当作工单。
 *
 * @author niuhaipeng
 */
public class StreamingExtraction {

    private static final int MAX_OUTPUT_CHARACTERS = 32000;
    private final StringBuilder content = new StringBuilder();
    private String finishReason;
    private String model;
    private TokenUsageDTO usage;

    public String append(ChatResponse response) {
        if (response == null) {
            return "";
        }
        if (StringUtils.hasText(response.getMetadata().getModel())) {
            model = response.getMetadata().getModel();
        }
        if (response.getMetadata().getUsage() != null
                && response.getMetadata().getUsage().getNativeUsage() != null) {
            usage = new TokenUsageDTO();
            usage.setInputTokens(response.getMetadata().getUsage().getPromptTokens());
            usage.setOutputTokens(response.getMetadata().getUsage().getCompletionTokens());
            usage.setTotalTokens(response.getMetadata().getUsage().getTotalTokens());
        }
        if (response.getResult() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        String reason = response.getResult().getMetadata().getFinishReason();
        if (text != null && !text.isEmpty()) {
            if (finishReason != null || content.length() + text.length() > MAX_OUTPUT_CHARACTERS) {
                throw new TicketProcessingException(ProcessingError.INVALID_MODEL_OUTPUT);
            }
            content.append(text);
        }
        if (StringUtils.hasText(reason)) {
            finishReason = reason;
        }
        return text == null ? "" : text;
    }

    public ExtractionResult finish(TicketStreamingModelGateway gateway) {
        if (!StringUtils.hasText(finishReason)) {
            throw new TicketProcessingException(ProcessingError.STREAM_INCOMPLETE);
        }
        return gateway.parse(content.toString(), finishReason, model, usage);
    }
}
