package com.niuhp.aixyz.dto.response;

import java.util.List;
import com.niuhp.aixyz.knowledge.CaseMatchDTO;
import com.niuhp.aixyz.model.ModelRerankDTO;

/**
 * 重排结果；失败不会伪装成没有相关候选。
 *
 * @author niuhaipeng
 */
public class RerankResultDTO {

    private String status;

    private String message;

    private List<CaseMatchDTO> matches;

    private ModelRerankDTO assessment;

    private String model;

    private TokenUsageDTO usage;

    private Long elapsedMillis;

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

    public List<CaseMatchDTO> getMatches() {
        return matches;
    }

    public void setMatches(List<CaseMatchDTO> matches) {
        this.matches = matches;
    }

    public ModelRerankDTO getAssessment() {
        return assessment;
    }

    public void setAssessment(ModelRerankDTO assessment) {
        this.assessment = assessment;
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
        return "RerankResultDTO{content=[REDACTED]}";
    }
}
