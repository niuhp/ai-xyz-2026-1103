package com.niuhp.aixyz.evaluation;

import java.util.List;

/**
 * 同一冻结样本集两次报告的逐条差异，不把故障解释成质量变化。
 *
 * @author niuhaipeng
 */
public class EvaluationChangeDTO {

    private String sampleId;

    private String method;

    private String kind;

    private List<String> before;

    private List<String> after;

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<String> getBefore() {
        return before;
    }

    public void setBefore(List<String> before) {
        this.before = before;
    }

    public List<String> getAfter() {
        return after;
    }

    public void setAfter(List<String> after) {
        this.after = after;
    }

    @Override
    public String toString() {
        return "EvaluationChangeDTO{content=[REDACTED]}";
    }
}
