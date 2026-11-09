package com.niuhp.aixyz.model;

import com.niuhp.aixyz.api.TokenUsageDTO;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * 流式模型边界：片段用于预览，完整文本仍需统一解析。
 *
 * @author niuhaipeng
 */
public interface TicketStreamingModelGateway {

    /**
     * 订阅一次生成，不自动重试。
     * @param description 原始描述
     * @return 上游增量响应
     */
    Flux<ChatResponse> stream(String description);

    /**
     * 校验完成原因并严格解析完整 JSON。
     * @param content 完整输出
     * @param finishReason 上游完成原因
     * @param model 上游模型标识
     * @param usage 上游报告用量，可为空
     * @return 待业务校验的结果
     */
    ExtractionResult parse(String content, String finishReason, String model, TokenUsageDTO usage);
}
