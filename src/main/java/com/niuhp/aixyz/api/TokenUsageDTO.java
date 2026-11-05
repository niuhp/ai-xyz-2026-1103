package com.niuhp.aixyz.api;


/**
 * 上游报告的 Token 用量，未提供时为 null；不是平台费用账单。
 *
 * @author niuhaipeng
 */
public class TokenUsageDTO {

    /** 输入 Token。 */
    private Integer inputTokens;

    /** 输出 Token。 */
    private Integer outputTokens;

    /** 合计 Token。 */
    private Integer totalTokens;

    public Integer getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }
    @Override
    public String toString() {
        return "TokenUsageDTO{inputTokens=" + inputTokens + ", outputTokens=" + outputTokens
                + ", totalTokens=" + totalTokens + "}";
    }
}
