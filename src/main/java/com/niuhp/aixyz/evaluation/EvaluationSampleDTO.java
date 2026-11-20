package com.niuhp.aixyz.evaluation;

import java.util.List;

/**
 * 人工标注样本；空期望集合表示暂不选参考案例，不是知识库不存在答案。
 *
 * @author niuhaipeng
 */
public class EvaluationSampleDTO {

    private String id;

    private String split;

    private String description;

    private List<String> expectedIds;

    private String rationale;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSplit() {
        return split;
    }

    public void setSplit(String split) {
        this.split = split;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getExpectedIds() {
        return expectedIds;
    }

    public void setExpectedIds(List<String> expectedIds) {
        this.expectedIds = expectedIds;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    @Override
    public String toString() {
        return "EvaluationSampleDTO{content=[REDACTED]}";
    }
}
