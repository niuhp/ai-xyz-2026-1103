package com.niuhp.aixyz.dto.response;

/**
 * 已创建的工单摘要。
 *
 * @author niuhaipeng
 */
public class TicketCreatedDTO {

    private String ticketId;
    private String status;

    public TicketCreatedDTO(String ticketId, String status) {
        this.ticketId = ticketId;
        this.status = status;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "TicketCreatedDTO{ticketId='" + ticketId + "', status='" + status + "'}";
    }
}
