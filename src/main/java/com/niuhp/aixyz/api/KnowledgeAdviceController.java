package com.niuhp.aixyz.api;

import com.niuhp.aixyz.knowledge.CaseKnowledgeBase;
import com.niuhp.aixyz.service.KnowledgeAdviceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 历史教学案例的参考建议，与创建工单接口独立。
 *
 * @author niuhaipeng
 */
@RestController
public class KnowledgeAdviceController {

    private final KnowledgeAdviceService service;
    private final CaseKnowledgeBase knowledgeBase;

    public KnowledgeAdviceController(KnowledgeAdviceService service, CaseKnowledgeBase knowledgeBase) {
        this.service = service;
        this.knowledgeBase = knowledgeBase;
    }

    @GetMapping("/api/v1/knowledge")
    public Map<String, Object> information() {
        return Map.of("caseCount", knowledgeBase.size(), "teachingOnly", true,
                "corpusVersion", CaseKnowledgeBase.CORPUS_VERSION,
                "retrievalMethod", CaseKnowledgeBase.RETRIEVAL_METHOD, "threshold", knowledgeBase.getThreshold());
    }

    @PostMapping("/api/v1/knowledge/advice")
    public KnowledgeAdviceResponseDTO generate(@Valid @RequestBody KnowledgeAdviceRequestDTO request,
            HttpServletRequest servletRequest) {
        return service.generate(request, (String)servletRequest.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
