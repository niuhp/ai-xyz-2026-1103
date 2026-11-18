package com.niuhp.aixyz.model;

import java.util.List;

/**
 * 模型必须对每个候选返回且仅返回一次判断。
 *
 * @author niuhaipeng
 */
public class ModelRerankDTO {

    private List<RerankAssessmentDTO> assessments;

    public List<RerankAssessmentDTO> getAssessments() {
        return assessments;
    }

    public void setAssessments(List<RerankAssessmentDTO> assessments) {
        this.assessments = assessments;
    }

    @Override
    public String toString() {
        return "ModelRerankDTO{content=[REDACTED]}";
    }
}
