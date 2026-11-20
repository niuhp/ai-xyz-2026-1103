package com.niuhp.aixyz.evaluation;

import java.util.List;

/**
 * 一次方法观测；失败不按正常空候选评分。
 *
 * @author niuhaipeng
 */
public class EvaluationObservationDTO {

    private String method;

    private String status;

    private Boolean completed;

    private List<String> actualIds;

    private Long elapsedMillis;

    private Integer totalTokens;

    private String model;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public List<String> getActualIds() {
        return actualIds;
    }

    public void setActualIds(List<String> actualIds) {
        this.actualIds = actualIds;
    }

    public Long getElapsedMillis() {
        return elapsedMillis;
    }

    public void setElapsedMillis(Long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String toString() {
        return "EvaluationObservationDTO{content=[REDACTED]}";
    }
}
