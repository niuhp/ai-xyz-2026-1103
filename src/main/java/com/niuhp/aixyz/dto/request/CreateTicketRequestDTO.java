package com.niuhp.aixyz.dto.request;

import com.niuhp.aixyz.domain.TicketDraftDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 用户确认后的工单创建请求。 */
/**
 * 用户确认后的工单创建请求。
 *
 * @author niuhaipeng
 */
public class CreateTicketRequestDTO {

    @NotBlank
    @Pattern(regexp = "[0-9a-fA-F-]{36}")
    private String draftId;

    @NotNull
    @Valid
    private TicketDraftDTO draft;

    public String getDraftId() {
        return draftId;
    }

    public void setDraftId(String draftId) {
        this.draftId = draftId;
    }

    public TicketDraftDTO getDraft() {
        return draft;
    }

    public void setDraft(TicketDraftDTO draft) {
        this.draft = draft;
    }

    @Override
    public String toString() {
        return "CreateTicketRequestDTO{draftId='" + draftId + "', draft=[REDACTED]}";
    }
}
