package com.niuhp.aixyz.api;

import java.util.List;
import com.niuhp.aixyz.knowledge.AdviceMode;
import com.niuhp.aixyz.knowledge.ModelAdviceDTO;
import com.niuhp.aixyz.knowledge.CaseMatchDTO;

/**
 * 第五篇知识库接口数据，只保存显式字段。
 *
 * @author niuhaipeng
 */
public class KnowledgeAdviceResponseDTO {

    private String requestId;

    private String status;

    private String message;

    private AdviceMode mode;

    private String retrievalMethod;

    private String corpusVersion;

    private Double threshold;

    private Integer caseCount;

    private ModelAdviceDTO advice;

    private List<CaseMatchDTO> sources;

    private String model;

    private TokenUsageDTO usage;

    private Long elapsedMillis;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AdviceMode getMode() {
        return mode;
    }

    public void setMode(AdviceMode mode) {
        this.mode = mode;
    }

    public String getRetrievalMethod() {
        return retrievalMethod;
    }

    public void setRetrievalMethod(String retrievalMethod) {
        this.retrievalMethod = retrievalMethod;
    }

    public String getCorpusVersion() {
        return corpusVersion;
    }

    public void setCorpusVersion(String corpusVersion) {
        this.corpusVersion = corpusVersion;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public Integer getCaseCount() {
        return caseCount;
    }

    public void setCaseCount(Integer caseCount) {
        this.caseCount = caseCount;
    }

    public ModelAdviceDTO getAdvice() {
        return advice;
    }

    public void setAdvice(ModelAdviceDTO advice) {
        this.advice = advice;
    }

    public List<CaseMatchDTO> getSources() {
        return sources;
    }

    public void setSources(List<CaseMatchDTO> sources) {
        this.sources = sources;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public TokenUsageDTO getUsage() {
        return usage;
    }

    public void setUsage(TokenUsageDTO usage) {
        this.usage = usage;
    }

    public Long getElapsedMillis() {
        return elapsedMillis;
    }

    public void setElapsedMillis(Long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
    }

    @Override
    public String toString() {
        return "KnowledgeAdviceResponseDTO{content=[REDACTED]}";
    }
}
