package com.niuhp.aixyz.knowledge;


/**
 * 第五篇知识库接口数据，只保存显式字段。
 *
 * @author niuhaipeng
 */
public class CaseMatchDTO {

    private KnowledgeCaseDTO document;

    private Double score;

    public KnowledgeCaseDTO getDocument() {
        return document;
    }

    public void setDocument(KnowledgeCaseDTO document) {
        this.document = document;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "CaseMatchDTO{content=[REDACTED]}";
    }
}
