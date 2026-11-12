package com.niuhp.aixyz.dto.response;

import java.util.Map;

/**
 * 同一语料、同一问题的两种检索结果。
 *
 * @author niuhaipeng
 */
public class RetrievalCompareResponseDTO {

    private String requestId;

    private String corpusVersion;

    private RetrievalResultDTO lexical;

    private RetrievalResultDTO semantic;

    private Map<String, Object> semanticIndex;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getCorpusVersion() {
        return corpusVersion;
    }

    public void setCorpusVersion(String corpusVersion) {
        this.corpusVersion = corpusVersion;
    }

    public RetrievalResultDTO getLexical() {
        return lexical;
    }

    public void setLexical(RetrievalResultDTO lexical) {
        this.lexical = lexical;
    }

    public RetrievalResultDTO getSemantic() {
        return semantic;
    }

    public void setSemantic(RetrievalResultDTO semantic) {
        this.semantic = semantic;
    }

    public Map<String, Object> getSemanticIndex() {
        return semanticIndex;
    }

    public void setSemanticIndex(Map<String, Object> semanticIndex) {
        this.semanticIndex = semanticIndex;
    }

    @Override
    public String toString() {
        return "RetrievalCompareResponseDTO{content=[REDACTED]}";
    }
}
