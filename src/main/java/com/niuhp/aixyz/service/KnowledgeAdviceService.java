package com.niuhp.aixyz.service;

import com.niuhp.aixyz.api.KnowledgeAdviceRequestDTO;
import com.niuhp.aixyz.api.KnowledgeAdviceResponseDTO;
import com.niuhp.aixyz.knowledge.AdviceMode;
import com.niuhp.aixyz.knowledge.AdviceOutputValidator;
import com.niuhp.aixyz.knowledge.CaseKnowledgeBase;
import com.niuhp.aixyz.knowledge.CaseMatchDTO;
import com.niuhp.aixyz.model.AdviceModelResult;
import com.niuhp.aixyz.model.KnowledgeAdviceGateway;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 先检索、再生成、最后核对来源；不写入草稿或工单，也不执行建议。
 *
 * @author niuhaipeng
 */
@Service
public class KnowledgeAdviceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeAdviceService.class);
    private final CaseKnowledgeBase knowledgeBase;
    private final KnowledgeAdviceGateway gateway;
    private final AdviceOutputValidator validator;
    private final TicketDraftService drafts;

    public KnowledgeAdviceService(CaseKnowledgeBase knowledgeBase, KnowledgeAdviceGateway gateway,
            AdviceOutputValidator validator, TicketDraftService drafts) {
        this.knowledgeBase = knowledgeBase;
        this.gateway = gateway;
        this.validator = validator;
        this.drafts = drafts;
    }

    public KnowledgeAdviceResponseDTO generate(KnowledgeAdviceRequestDTO request, String requestId) {
        long started = System.nanoTime();
        KnowledgeAdviceResponseDTO response = new KnowledgeAdviceResponseDTO();
        response.setRequestId(requestId);
        response.setMode(request.getMode());
        response.setRetrievalMethod(request.getMode() == AdviceMode.RETRIEVAL
                ? CaseKnowledgeBase.RETRIEVAL_METHOD : "NONE");
        response.setCorpusVersion(CaseKnowledgeBase.CORPUS_VERSION);
        response.setThreshold(knowledgeBase.getThreshold());
        response.setCaseCount(knowledgeBase.size());
        List<CaseMatchDTO> matches = request.getMode() == AdviceMode.RETRIEVAL
                ? knowledgeBase.search(request.getDescription()) : List.of();
        response.setSources(matches);
        if (request.getMode() == AdviceMode.RETRIEVAL && matches.isEmpty()) {
            response.setStatus("NO_MATCH");
            response.setMessage("当前词项检索规则和阈值下未找到相关教学案例，未调用生成模型；请补充具体症状或换用通用建议。");
        } else {
            drafts.acquirePermit();
            try {
                AdviceModelResult result = gateway.generate(request.getDescription(), request.getMode(), matches);
                validator.validate(result.getAdvice(), request.getMode(), matches);
                response.setAdvice(result.getAdvice());
                response.setModel(result.getModel());
                response.setUsage(result.getUsage());
                boolean insufficient = !Boolean.TRUE.equals(result.getAdvice().getSupported());
                response.setStatus(insufficient ? "INSUFFICIENT_EVIDENCE"
                        : request.getMode() == AdviceMode.RETRIEVAL ? "REFERENCE" : "BASELINE");
                response.setMessage(insufficient ? "当前资料或信息不足，暂不提供具体步骤，请补充待核实信息。"
                        : request.getMode() == AdviceMode.RETRIEVAL
                        ? "建议参考教学案例，来源和摘录已做结构校验；是否适用于当前问题仍须人工核实。"
                        : "未检索知识库，仅提供通用排查方向；没有历史案例依据。");
            } finally {
                drafts.releasePermit();
            }
        }
        response.setElapsedMillis(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        LOGGER.info("knowledge_advice requestId={} mode={} status={} sources={} elapsedMillis={}",
                requestId, request.getMode(), response.getStatus(), matches.size(), response.getElapsedMillis());
        return response;
    }
}
