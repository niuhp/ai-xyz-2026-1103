package com.niuhaipeng.aixyz.api;

import com.niuhaipeng.aixyz.domain.DraftStatus;
import com.niuhaipeng.aixyz.domain.TicketDraftDTO;
import java.util.List;

/**
 * 工单草稿生成结果。
 *
 * @author niuhaipeng
 */
public class TicketDraftResponseDTO {

    /** 本次应用请求编号。 */
    private String requestId;

    /** 由服务端计算的完整性状态。 */
    private DraftStatus status;

    /** 无关描述时为 null。 */
    private TicketDraftDTO draft;

    /** 服务端计算的缺失字段中文名称。 */
    private List<String> missingFields;

    /** 上游报告用量，非最终费用。 */
    private TokenUsageDTO usage;

    /** 上游响应模型标识。 */
    private String model;

    /** 完整处理耗时，单位毫秒。 */
    private Long elapsedMillis;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public DraftStatus getStatus() {
        return status;
    }

    public void setStatus(DraftStatus status) {
        this.status = status;
    }

    public TicketDraftDTO getDraft() {
        return draft;
    }

    public void setDraft(TicketDraftDTO draft) {
        this.draft = draft;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }

    public TokenUsageDTO getUsage() {
        return usage;
    }

    public void setUsage(TokenUsageDTO usage) {
        this.usage = usage;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Long getElapsedMillis() {
        return elapsedMillis;
    }

    public void setElapsedMillis(Long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
    }
    @Override
    public String toString() {
        return "TicketDraftResponseDTO{requestId='" + requestId + "', status=" + status
                + ", model='" + model + "', elapsedMillis=" + elapsedMillis + ", draft=[REDACTED]}";
    }
}
