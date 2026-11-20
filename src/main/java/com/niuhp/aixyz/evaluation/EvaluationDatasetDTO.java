package com.niuhp.aixyz.evaluation;

import java.util.List;

/**
 * 带版本号的固定教学评测集。
 *
 * @author niuhaipeng
 */
public class EvaluationDatasetDTO {

    private String version;

    private String labelPolicy;

    private List<EvaluationSampleDTO> samples;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLabelPolicy() {
        return labelPolicy;
    }

    public void setLabelPolicy(String labelPolicy) {
        this.labelPolicy = labelPolicy;
    }

    public List<EvaluationSampleDTO> getSamples() {
        return samples;
    }

    public void setSamples(List<EvaluationSampleDTO> samples) {
        this.samples = samples;
    }

    @Override
    public String toString() {
        return "EvaluationDatasetDTO{content=[REDACTED]}";
    }
}
