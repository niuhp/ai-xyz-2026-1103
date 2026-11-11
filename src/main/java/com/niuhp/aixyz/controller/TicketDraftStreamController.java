package com.niuhp.aixyz.controller;

import com.niuhp.aixyz.dto.request.TicketDraftRequestDTO;
import com.niuhp.aixyz.filter.RequestIdFilter;
import com.niuhp.aixyz.service.TicketDraftStreamService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * POST + fetch 传递描述，避免把输入放入 URL；输出使用 SSE 帧。
 *
 * @author niuhaipeng
 */
@RestController
public class TicketDraftStreamController {

    private final TicketDraftStreamService service;

    public TicketDraftStreamController(TicketDraftStreamService service) {
        this.service = service;
    }

    @PostMapping(value = "/api/v1/ticket-drafts/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody TicketDraftRequestDTO request,
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        servletResponse.setHeader("Cache-Control", "no-cache, no-transform");
        servletResponse.setHeader("X-Accel-Buffering", "no");
        return service.generate(request.getDescription(),
                (String)servletRequest.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }
}
