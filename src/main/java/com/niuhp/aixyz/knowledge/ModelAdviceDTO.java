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
public class ModelAdviceDTO {

    @NotNull
    private Boolean supported;

    @NotBlank
    @Size(max = 600)
    private String summary;

    @NotNull
    @Size(max = 5)
    @Valid
    private List<@NotNull AdviceStepDTO> steps;

    @NotNull
    @Size(min = 1, max = 5)
    private List<@NotBlank @Size(max = 300) String> limitations;

    public Boolean getSupported() {
        return supported;
    }

    public void setSupported(Boolean supported) {
        this.supported = supported;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<@NotNull AdviceStepDTO> getSteps() {
        return steps;
    }

    public void setSteps(List<@NotNull AdviceStepDTO> steps) {
        this.steps = steps;
    }

    public List<@NotBlank @Size(max = 300) String> getLimitations() {
        return limitations;
    }

    public void setLimitations(List<@NotBlank @Size(max = 300) String> limitations) {
        this.limitations = limitations;
    }

    @Override
    public String toString() {
        return "ModelAdviceDTO{content=[REDACTED]}";
    }
}
