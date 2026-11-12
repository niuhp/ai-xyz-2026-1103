package com.niuhp.aixyz.controller;

import com.niuhp.aixyz.dto.request.RetrievalCompareRequestDTO;
import com.niuhp.aixyz.dto.response.RetrievalCompareResponseDTO;
import com.niuhp.aixyz.filter.RequestIdFilter;
import com.niuhp.aixyz.knowledge.SemanticCaseIndex;
import com.niuhp.aixyz.service.RetrievalCompareService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第六篇的独立检索对照接口，不产生建议或工单。
 *
 * @author niuhaipeng
 */
@RestController
public class RetrievalCompareController {

    private final RetrievalCompareService service;
    private final SemanticCaseIndex index;

    public RetrievalCompareController(RetrievalCompareService service, SemanticCaseIndex index) {
        this.service = service;
        this.index = index;
    }

    @GetMapping("/api/v1/knowledge/semantic")
    public Map<String, Object> information() {
        return index.information();
    }

    @PostMapping("/api/v1/knowledge/compare")
    public RetrievalCompareResponseDTO compare(@Valid @RequestBody RetrievalCompareRequestDTO request,
            HttpServletRequest servletRequest) {
        return service.compare(request.getDescription(),
                (String)servletRequest.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
