package com.niuhp.aixyz.dto.response;

import com.niuhp.aixyz.knowledge.CaseMatchDTO;
import java.util.List;

/**
 * 单一路径的召回结果；错误时保留另一检索路径的结果。
 *
 * @author niuhaipeng
 */
public class RetrievalResultDTO {

    private String method;

    private String status;

    private String message;

    private Double threshold;

    private List<CaseMatchDTO> matches;

    private Long elapsedMillis;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
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

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public List<CaseMatchDTO> getMatches() {
        return matches;
    }

    public void setMatches(List<CaseMatchDTO> matches) {
        this.matches = matches;
    }

    public Long getElapsedMillis() {
        return elapsedMillis;
    }

    public void setElapsedMillis(Long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
    }

    @Override
    public String toString() {
        return "RetrievalResultDTO{content=[REDACTED]}";
    }
}
