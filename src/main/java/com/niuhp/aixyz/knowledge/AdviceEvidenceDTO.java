package com.niuhp.aixyz.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 第五篇知识库接口数据，只保存显式字段。
 *
 * @author niuhaipeng
 */
public class AdviceEvidenceDTO {

    @NotBlank
    @Size(max = 40)
    private String caseId;

    @NotBlank
    @Size(min = 8, max = 400)
    private String quote;

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    @Override
    public String toString() {
        return "AdviceEvidenceDTO{content=[REDACTED]}";
    }
}
