package com.niuhp.aixyz.controller;

import com.niuhp.aixyz.dto.request.RetrievalCompareRequestDTO;
import com.niuhp.aixyz.dto.response.RerankCompareResponseDTO;
import com.niuhp.aixyz.filter.RequestIdFilter;
import com.niuhp.aixyz.service.CaseRerankService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 独立重排对照入口，会调用聊天模型，不生成建议或写入工单。
 *
 * @author niuhaipeng
 */
@RestController
public class CaseRerankController {

    private final CaseRerankService service;

    public CaseRerankController(CaseRerankService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/knowledge/rerank")
    public RerankCompareResponseDTO compare(@Valid @RequestBody RetrievalCompareRequestDTO request,
            HttpServletRequest servletRequest) {
        return service.compare(request.getDescription(),
                (String)servletRequest.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
