package com.niuhp.aixyz.model;

import com.niuhp.aixyz.dto.response.TokenUsageDTO;

/**
 * 模型建议及可选上游用量。
 *
 * @author niuhaipeng
 */
public class AdviceModelResult {

    private final ModelAdviceDTO advice;
    private final String model;
    private final TokenUsageDTO usage;

    public AdviceModelResult(ModelAdviceDTO advice, String model, TokenUsageDTO usage) {
        this.advice = advice;
        this.model = model;
        this.usage = usage;
    }

    public ModelAdviceDTO getAdvice() {
        return advice;
    }

    public String getModel() {
        return model;
    }

    public TokenUsageDTO getUsage() {
        return usage;
    }
}
