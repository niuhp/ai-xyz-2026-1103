package com.niuhp.aixyz.evaluation;

import java.util.List;

/**
 * 不含密钥的可复查报告；哈希约束数据、语料和运行配置。
 *
 * @author niuhaipeng
 */
public class EvaluationReportDTO {

    private String runLabel;

    private String createdAt;

    private String datasetVersion;

    private String datasetSha256;

    private String corpusVersion;

    private String corpusSha256;

    private String split;

    private String configurationSha256;

    private java.util.Map<String, Object> configuration;

    private List<EvaluationRowDTO> rows;

    private List<EvaluationMetricDTO> metrics;

    private List<EvaluationChangeDTO> changes;

    private String comparisonNote;

    public String getRunLabel() {
        return runLabel;
    }

    public void setRunLabel(String runLabel) {
        this.runLabel = runLabel;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(String datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    public String getDatasetSha256() {
        return datasetSha256;
    }

    public void setDatasetSha256(String datasetSha256) {
        this.datasetSha256 = datasetSha256;
    }

    public String getCorpusVersion() {
        return corpusVersion;
    }

    public void setCorpusVersion(String corpusVersion) {
        this.corpusVersion = corpusVersion;
    }

    public String getCorpusSha256() {
        return corpusSha256;
    }

    public void setCorpusSha256(String corpusSha256) {
        this.corpusSha256 = corpusSha256;
    }

    public String getSplit() {
        return split;
    }

    public void setSplit(String split) {
        this.split = split;
    }

    public String getConfigurationSha256() {
        return configurationSha256;
    }

    public void setConfigurationSha256(String configurationSha256) {
        this.configurationSha256 = configurationSha256;
    }

    public java.util.Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(java.util.Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public List<EvaluationRowDTO> getRows() {
        return rows;
    }

    public void setRows(List<EvaluationRowDTO> rows) {
        this.rows = rows;
    }

    public List<EvaluationMetricDTO> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<EvaluationMetricDTO> metrics) {
        this.metrics = metrics;
    }

    public List<EvaluationChangeDTO> getChanges() {
        return changes;
    }

    public void setChanges(List<EvaluationChangeDTO> changes) {
        this.changes = changes;
    }

    public String getComparisonNote() {
        return comparisonNote;
    }

    public void setComparisonNote(String comparisonNote) {
        this.comparisonNote = comparisonNote;
    }

    @Override
    public String toString() {
        return "EvaluationReportDTO{content=[REDACTED]}";
    }
}
