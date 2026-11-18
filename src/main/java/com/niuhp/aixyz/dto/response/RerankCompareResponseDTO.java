package com.niuhp.aixyz.dto.response;


/**
 * 保留同次原始召回与重排结果，不生成建议。
 *
 * @author niuhaipeng
 */
public class RerankCompareResponseDTO {

    private RetrievalCompareResponseDTO retrieval;

    private RerankResultDTO rerank;

    private Long elapsedMillis;

    public RetrievalCompareResponseDTO getRetrieval() {
        return retrieval;
    }

    public void setRetrieval(RetrievalCompareResponseDTO retrieval) {
        this.retrieval = retrieval;
    }

    public RerankResultDTO getRerank() {
        return rerank;
    }

    public void setRerank(RerankResultDTO rerank) {
        this.rerank = rerank;
    }

    public Long getElapsedMillis() {
        return elapsedMillis;
    }

    public void setElapsedMillis(Long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
    }

    @Override
    public String toString() {
        return "RerankCompareResponseDTO{content=[REDACTED]}";
    }
}
