package com.niuhp.aixyz.api;

import com.niuhp.aixyz.service.TicketDraftService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户描述转工单草稿接口。
 *
 * @author niuhaipeng
 */
@RestController
@RequestMapping("/api/v1/ticket-drafts")
public class TicketDraftController {

    /** 工单草稿应用服务。 */
    private final TicketDraftService ticketDraftService;

    public TicketDraftController(TicketDraftService ticketDraftService) {
        this.ticketDraftService = ticketDraftService;
    }

    @PostMapping
    public TicketDraftResponseDTO generate(@Valid @RequestBody TicketDraftRequestDTO request,
            HttpServletRequest servletRequest) {
        return ticketDraftService.generate(request.getDescription(),
                (String)servletRequest.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketCreatedDTO create(@Valid @RequestBody CreateTicketRequestDTO request) {
        return ticketDraftService.create(request);
    }
}
