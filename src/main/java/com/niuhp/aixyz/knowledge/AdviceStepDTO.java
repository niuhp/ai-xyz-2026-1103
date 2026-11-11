package com.niuhp.aixyz.knowledge;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 第五篇知识库接口数据，只保存显式字段。
 *
 * @author niuhaipeng
 */
public class AdviceStepDTO {

    @NotBlank
    @Size(max = 500)
    private String text;

    @NotNull
    @Size(max = 3)
    @Valid
    private List<@NotNull AdviceEvidenceDTO> evidence;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<@NotNull AdviceEvidenceDTO> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<@NotNull AdviceEvidenceDTO> evidence) {
        this.evidence = evidence;
    }

    @Override
    public String toString() {
        return "AdviceStepDTO{content=[REDACTED]}";
    }
}
