package com.niuhp.aixyz.knowledge;


/**
 * 第五篇知识库接口数据，只保存显式字段。
 *
 * @author niuhaipeng
 */
public class CaseMatchDTO {

    private KnowledgeCaseDTO document;

    private Double score;

    private Integer lexicalRank;

    public Integer getLexicalRank() {
        return lexicalRank;
    }

    public void setLexicalRank(Integer lexicalRank) {
        this.lexicalRank = lexicalRank;
    }

    private Integer semanticRank;

    public Integer getSemanticRank() {
        return semanticRank;
    }

    public void setSemanticRank(Integer semanticRank) {
        this.semanticRank = semanticRank;
    }

    private Double lexicalScore;

    public Double getLexicalScore() {
        return lexicalScore;
    }

    public void setLexicalScore(Double lexicalScore) {
        this.lexicalScore = lexicalScore;
    }

    private Double semanticScore;

    public Double getSemanticScore() {
        return semanticScore;
    }

    public void setSemanticScore(Double semanticScore) {
        this.semanticScore = semanticScore;
    }

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
