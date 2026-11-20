package com.niuhp.aixyz.evaluation;

import java.util.List;

/**
 * 同题四种方案复用一次检索的评测行。
 *
 * @author niuhaipeng
 */
public class EvaluationRowDTO {

    private EvaluationSampleDTO sample;

    private List<EvaluationObservationDTO> observations;

    public EvaluationSampleDTO getSample() {
        return sample;
    }

    public void setSample(EvaluationSampleDTO sample) {
        this.sample = sample;
    }

    public List<EvaluationObservationDTO> getObservations() {
        return observations;
    }

    public void setObservations(List<EvaluationObservationDTO> observations) {
        this.observations = observations;
    }

    @Override
    public String toString() {
        return "EvaluationRowDTO{content=[REDACTED]}";
    }
}
