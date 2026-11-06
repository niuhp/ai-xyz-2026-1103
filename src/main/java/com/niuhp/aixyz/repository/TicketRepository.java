package com.niuhp.aixyz.repository;

import com.niuhp.aixyz.domain.TicketDraftDTO;
import com.niuhp.aixyz.service.ProcessingError;
import com.niuhp.aixyz.service.TicketProcessingException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 保存待确认草稿和已确认工单。
 *
 * @author niuhaipeng
 */
@Repository
public class TicketRepository {

    private final JdbcTemplate jdbcTemplate;

    public TicketRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveDraft(String draftId, String draftJson) {
        jdbcTemplate.update("INSERT INTO ticket_draft (draft_id, draft_json, status, created_at) VALUES (?, ?, ?, ?)",
                draftId, draftJson, "PENDING", OffsetDateTime.now());
    }

    @Transactional(rollbackFor = Exception.class)
    public String createConfirmed(String draftId, TicketDraftDTO draft) {
        String existing = findTicketId(draftId);
        if (existing != null) {
            return existing;
        }
        Integer draftCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket_draft WHERE draft_id = ?",
                Integer.class, draftId);
        if (draftCount == null || draftCount == 0) {
            throw new TicketProcessingException(ProcessingError.DRAFT_NOT_FOUND);
        }
        String ticketId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO support_ticket (ticket_id, draft_id, system_name, module_name, category, summary,
                    actual_result, expected_result, occurred_at_text, version, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, ticketId, draftId, draft.getSystem(), draft.getModule(), draft.getCategory().name(),
                draft.getSummary(), draft.getActualResult(), draft.getExpectedResult(), draft.getOccurredAtText(),
                draft.getVersion(), OffsetDateTime.now());
        jdbcTemplate.update("UPDATE ticket_draft SET status = 'CREATED' WHERE draft_id = ?", draftId);
        return ticketId;
    }

    private String findTicketId(String draftId) {
        return jdbcTemplate.query("SELECT ticket_id FROM support_ticket WHERE draft_id = ?",
                resultSet -> resultSet.next() ? resultSet.getString(1) : null, draftId);
    }
}
