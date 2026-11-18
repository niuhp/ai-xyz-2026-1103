package com.niuhp.aixyz.service;

import com.niuhp.aixyz.dto.response.RerankCompareResponseDTO;
import com.niuhp.aixyz.dto.response.RerankResultDTO;
import com.niuhp.aixyz.dto.response.RetrievalResultDTO;
import com.niuhp.aixyz.exception.TicketProcessingException;
import com.niuhp.aixyz.knowledge.CaseRerankValidator;
import com.niuhp.aixyz.model.CaseRerankGateway;
import com.niuhp.aixyz.model.ModelConcurrencyLimiter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 复用一次混合检索，接受明确匹配的候选；失败时保留原结果供查看。
 *
 * @author niuhaipeng
 */
@Service
public class CaseRerankService {

    private static final String DEGRADED = "DEGRADED";

    private final RetrievalCompareService retrieval;
    private final CaseRerankGateway gateway;
    private final CaseRerankValidator validator;
    private final ModelConcurrencyLimiter limiter;

    public CaseRerankService(RetrievalCompareService retrieval, CaseRerankGateway gateway,
            CaseRerankValidator validator, ModelConcurrencyLimiter limiter) {
        this.retrieval = retrieval;
        this.gateway = gateway;
        this.validator = validator;
        this.limiter = limiter;
    }

    public RerankCompareResponseDTO compare(String description, String requestId) {
        long started = System.nanoTime();
        RerankCompareResponseDTO response = new RerankCompareResponseDTO();
        response.setRetrieval(retrieval.compare(description, requestId));
        response.setRerank(rerank(description, response.getRetrieval().getHybrid()));
        response.setElapsedMillis(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        return response;
    }

    private RerankResultDTO rerank(String description, RetrievalResultDTO source) {
        long started = System.nanoTime();
        RerankResultDTO result = new RerankResultDTO();
        result.setMatches(List.of());
        if (source.getMatches().isEmpty()) {
            result.setStatus(DEGRADED.equals(source.getStatus()) ? "RETRIEVAL_UNAVAILABLE" : "NO_CANDIDATES");
            result.setMessage("没有候选，跳过聊天模型；请结合原始检索状态判断原因。");
        } else if (DEGRADED.equals(source.getStatus())) {
            result.setStatus("SKIPPED_DEGRADED");
            result.setMessage("原检索已降级，本次不重排；原始候选仍可查看，不能视为完整结果。");
        } else {
            assess(description, source, result);
        }
        result.setElapsedMillis(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        return result;
    }

    private void assess(String description, RetrievalResultDTO source, RerankResultDTO result) {
        boolean acquired = false;
        try {
            limiter.acquirePermit();
            acquired = true;
            var output = gateway.assess(description, source.getMatches());
            validator.validate(output.getAssessment(), source.getMatches());
            Set<String> accepted = output.getAssessment().getAssessments().stream()
                    .filter(item -> item.getGrade() == 2).map(item -> item.getCaseId()).collect(Collectors.toSet());
            // 同级保留原 RRF 顺序；保留原分数含义，不伪造模型置信度。
            result.setMatches(source.getMatches().stream()
                    .filter(item -> accepted.contains(item.getDocument().getId())).toList());
            result.setAssessment(output.getAssessment());
            result.setModel(output.getModel());
            result.setUsage(output.getUsage());
            result.setStatus(accepted.isEmpty() ? "INSUFFICIENT_EVIDENCE" : "RERANKED");
            result.setMessage(accepted.isEmpty() ? "候选均缺少明确匹配依据，不输出可参考案例；不等于知识库没有答案。"
                    : "只保留模型判为 2 的候选，同级沿用 RRF 顺序；判断与摘录仍须人工核实。");
        } catch (TicketProcessingException exception) {
            result.setStatus(exception.getError().name());
            result.setMessage(exception.getError().getMessage() + "；本次重排未完成，原始候选仍可查看。");
        } finally {
            if (acquired) {
                limiter.releasePermit();
            }
        }
    }
}
