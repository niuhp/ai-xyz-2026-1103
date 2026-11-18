package com.niuhp.aixyz.model;


/**
 * 模型对单个候选的判断；分级不是概率。
 *
 * @author niuhaipeng
 */
public class RerankAssessmentDTO {

    private String caseId;

    private Integer grade;

    private String reason;

    private String evidence;

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    @Override
    public String toString() {
        return "RerankAssessmentDTO{content=[REDACTED]}";
    }
}
