package com.niuhp.aixyz.model;

import com.niuhp.aixyz.api.TokenUsageDTO;

/**
 * 模型边界的解析结果及用量。
 *
 * @author niuhaipeng
 */
public class ExtractionResult {

    /** 结构化模型输出。 */
    private ModelExtractionDTO extraction;

    /** 上游报告用量。 */
    private TokenUsageDTO usage;

    /** 实际响应所标注的模型。 */
    private String model;

    public ModelExtractionDTO getExtraction() {
        return extraction;
    }

    public void setExtraction(ModelExtractionDTO extraction) {
        this.extraction = extraction;
    }

    public TokenUsageDTO getUsage() {
        return usage;
    }

    public void setUsage(TokenUsageDTO usage) {
        this.usage = usage;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
