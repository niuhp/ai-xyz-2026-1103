package com.niuhp.aixyz.evaluation;


/**
 * 公开分子分母；未定义的宏指标保持为空。
 *
 * @author niuhaipeng
 */
public class EvaluationMetricDTO {

    private String method;

    private Integer total;

    private Integer completed;

    private Integer exact;

    private Integer positiveCompleted;

    private Integer top1Hits;

    private Integer falseRejections;

    private Integer negativeCompleted;

    private Integer correctAbstentions;

    private Integer nonemptyCompleted;

    private Double macroPrecision;

    private Double macroRecall;

    private Double meanElapsedMillis;

    private Integer tokenObservations;

    private Long reportedTotalTokens;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getCompleted() {
        return completed;
    }

    public void setCompleted(Integer completed) {
        this.completed = completed;
    }

    public Integer getExact() {
        return exact;
    }

    public void setExact(Integer exact) {
        this.exact = exact;
    }

    public Integer getPositiveCompleted() {
        return positiveCompleted;
    }

    public void setPositiveCompleted(Integer positiveCompleted) {
        this.positiveCompleted = positiveCompleted;
    }

    public Integer getTop1Hits() {
        return top1Hits;
    }

    public void setTop1Hits(Integer top1Hits) {
        this.top1Hits = top1Hits;
    }

    public Integer getFalseRejections() {
        return falseRejections;
    }

    public void setFalseRejections(Integer falseRejections) {
        this.falseRejections = falseRejections;
    }

    public Integer getNegativeCompleted() {
        return negativeCompleted;
    }

    public void setNegativeCompleted(Integer negativeCompleted) {
        this.negativeCompleted = negativeCompleted;
    }

    public Integer getCorrectAbstentions() {
        return correctAbstentions;
    }

    public void setCorrectAbstentions(Integer correctAbstentions) {
        this.correctAbstentions = correctAbstentions;
    }

    public Integer getNonemptyCompleted() {
        return nonemptyCompleted;
    }

    public void setNonemptyCompleted(Integer nonemptyCompleted) {
        this.nonemptyCompleted = nonemptyCompleted;
    }

    public Double getMacroPrecision() {
        return macroPrecision;
    }

    public void setMacroPrecision(Double macroPrecision) {
        this.macroPrecision = macroPrecision;
    }

    public Double getMacroRecall() {
        return macroRecall;
    }

    public void setMacroRecall(Double macroRecall) {
        this.macroRecall = macroRecall;
    }

    public Double getMeanElapsedMillis() {
        return meanElapsedMillis;
    }

    public void setMeanElapsedMillis(Double meanElapsedMillis) {
        this.meanElapsedMillis = meanElapsedMillis;
    }

    public Integer getTokenObservations() {
        return tokenObservations;
    }

    public void setTokenObservations(Integer tokenObservations) {
        this.tokenObservations = tokenObservations;
    }

    public Long getReportedTotalTokens() {
        return reportedTotalTokens;
    }

    public void setReportedTotalTokens(Long reportedTotalTokens) {
        this.reportedTotalTokens = reportedTotalTokens;
    }

    @Override
    public String toString() {
        return "EvaluationMetricDTO{content=[REDACTED]}";
    }
}
