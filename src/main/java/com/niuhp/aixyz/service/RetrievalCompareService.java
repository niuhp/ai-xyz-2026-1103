package com.niuhp.aixyz.service;

import com.niuhp.aixyz.dto.response.RetrievalCompareResponseDTO;
import com.niuhp.aixyz.dto.response.RetrievalResultDTO;
import com.niuhp.aixyz.exception.TicketProcessingException;
import com.niuhp.aixyz.knowledge.CaseKnowledgeBase;
import com.niuhp.aixyz.knowledge.CaseMatchDTO;
import com.niuhp.aixyz.knowledge.SemanticCaseIndex;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * 只比较召回，不混入生成质量，也不静默回退。
 *
 * @author niuhaipeng
 */
@Service
public class RetrievalCompareService {

    private final CaseKnowledgeBase lexical;
    private final SemanticCaseIndex semantic;

    public RetrievalCompareService(CaseKnowledgeBase lexical, SemanticCaseIndex semantic) {
        this.lexical = lexical;
        this.semantic = semantic;
    }

    public RetrievalCompareResponseDTO compare(String description, String requestId) {
        RetrievalCompareResponseDTO response = new RetrievalCompareResponseDTO();
        response.setRequestId(requestId);
        response.setCorpusVersion(CaseKnowledgeBase.CORPUS_VERSION);
        response.setLexical(retrieve(CaseKnowledgeBase.RETRIEVAL_METHOD, lexical.getThreshold(),
                () -> lexical.search(description)));
        response.setSemantic(retrieve(SemanticCaseIndex.METHOD, semantic.getThreshold(),
                () -> semantic.search(description)));
        response.setSemanticIndex(semantic.information());
        return response;
    }

    private RetrievalResultDTO retrieve(String method, double threshold, Supplier<List<CaseMatchDTO>> search) {
        long started = System.nanoTime();
        RetrievalResultDTO result = new RetrievalResultDTO();
        result.setMethod(method);
        result.setThreshold(threshold);
        result.setMatches(List.of());
        try {
            result.setMatches(search.get());
            result.setStatus(result.getMatches().isEmpty() ? "NO_MATCH" : "MATCHED");
            result.setMessage(result.getMatches().isEmpty() ? "当前规则和阈值下无匹配" : "相似案例须人工核实，分数不是正确率");
        } catch (TicketProcessingException exception) {
            result.setStatus(exception.getError().name());
            result.setMessage(exception.getError().getMessage());
        }
        result.setElapsedMillis(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        return result;
    }
}
