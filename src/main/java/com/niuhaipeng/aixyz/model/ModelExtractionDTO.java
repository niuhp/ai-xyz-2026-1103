package com.niuhaipeng.aixyz.model;

import com.niuhaipeng.aixyz.domain.TicketDraftDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 仅在模型边界使用的返回结构。
 *
 * @author niuhaipeng
 */
public class ModelExtractionDTO {

    /** 是否为软件故障、异常或改进反馈。 */
    @NotNull
    private Boolean relevant;

    /** 相关时必须有草稿，无关时为 null。 */
    @Valid
    private TicketDraftDTO draft;

    public Boolean getRelevant() {
        return relevant;
    }

    public void setRelevant(Boolean relevant) {
        this.relevant = relevant;
    }

    public TicketDraftDTO getDraft() {
        return draft;
    }

    public void setDraft(TicketDraftDTO draft) {
        this.draft = draft;
    }
    @Override
    public String toString() {
        return "ModelExtractionDTO{relevant=" + relevant + ", draft=[REDACTED]}";
    }
}
