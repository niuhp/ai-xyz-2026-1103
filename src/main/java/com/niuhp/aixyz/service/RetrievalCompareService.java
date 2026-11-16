package com.niuhp.aixyz.service;

import com.niuhp.aixyz.dto.response.RetrievalCompareResponseDTO;
import com.niuhp.aixyz.dto.response.RetrievalResultDTO;
import com.niuhp.aixyz.exception.TicketProcessingException;
import com.niuhp.aixyz.knowledge.CaseKnowledgeBase;
import com.niuhp.aixyz.knowledge.CaseMatchDTO;
import com.niuhp.aixyz.knowledge.SemanticCaseIndex;
import com.niuhp.aixyz.knowledge.ReciprocalRankFusion;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * 比较两路召回并融合排名，不混入生成质量，失败显式标记降级。
 *
 * @author niuhaipeng
 */
@Service
public class RetrievalCompareService {

    private final CaseKnowledgeBase lexical;
    private final SemanticCaseIndex semantic;
    private final ReciprocalRankFusion fusion;

    public RetrievalCompareService(CaseKnowledgeBase lexical, SemanticCaseIndex semantic, ReciprocalRankFusion fusion) {
        this.lexical = lexical;
        this.semantic = semantic;
        this.fusion = fusion;
    }

    public RetrievalCompareResponseDTO compare(String description, String requestId) {
        long started = System.nanoTime();
        RetrievalCompareResponseDTO response = new RetrievalCompareResponseDTO();
        response.setRequestId(requestId);
        response.setCorpusVersion(CaseKnowledgeBase.CORPUS_VERSION);
        response.setLexical(retrieve(CaseKnowledgeBase.RETRIEVAL_METHOD, lexical.getThreshold(),
                () -> lexical.search(description)));
        response.setSemantic(retrieve(SemanticCaseIndex.METHOD, semantic.getThreshold(),
                () -> semantic.search(description)));
        response.setHybrid(combine(response.getLexical(), response.getSemantic(), started));
        response.setSemanticIndex(semantic.information());
        return response;
    }

    private RetrievalResultDTO combine(RetrievalResultDTO lexicalResult, RetrievalResultDTO semanticResult, long started) {
        RetrievalResultDTO result = new RetrievalResultDTO();
        result.setMethod(ReciprocalRankFusion.METHOD);
        result.setRrfK(ReciprocalRankFusion.RRF_K);
        result.setLexicalStatus(lexicalResult.getStatus());
        result.setSemanticStatus(semanticResult.getStatus());
        result.setMatches(fusion.fuse(lexicalResult.getMatches(), semanticResult.getMatches()));
        boolean complete = completed(lexicalResult) && completed(semanticResult);
        result.setStatus(!complete ? "DEGRADED" : result.getMatches().isEmpty() ? "NO_MATCH" : "MATCHED");
        result.setMessage(!complete ? "检索已降级，仅融合可用路径；请查看两路状态，当前结果不代表完整混合检索。"
                : result.getMatches().isEmpty() ? "两路均未召回，不产生候选"
                : "按等权 RRF 排名融合；分数不是相似度或概率，同分按案例编号排序。");
        result.setElapsedMillis(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        return result;
    }

    private boolean completed(RetrievalResultDTO result) {
        return "MATCHED".equals(result.getStatus()) || "NO_MATCH".equals(result.getStatus());
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
