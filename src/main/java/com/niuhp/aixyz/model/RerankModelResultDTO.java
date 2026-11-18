package com.niuhp.aixyz.model;

import com.niuhp.aixyz.model.ModelRerankDTO;

/**
 * 封装模型判断与实际返回的用量。
 *
 * @author niuhaipeng
 */
public class RerankModelResultDTO {

    private ModelRerankDTO assessment;

    private String model;

    private com.niuhp.aixyz.dto.response.TokenUsageDTO usage;

    public ModelRerankDTO getAssessment() {
        return assessment;
    }

    public void setAssessment(ModelRerankDTO assessment) {
        this.assessment = assessment;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public com.niuhp.aixyz.dto.response.TokenUsageDTO getUsage() {
        return usage;
    }

    public void setUsage(com.niuhp.aixyz.dto.response.TokenUsageDTO usage) {
        this.usage = usage;
    }

    @Override
    public String toString() {
        return "RerankModelResultDTO{content=[REDACTED]}";
    }
}
