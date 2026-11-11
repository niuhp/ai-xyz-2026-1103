package com.niuhp.aixyz.service;

import com.niuhp.aixyz.domain.DraftStatus;
import com.niuhp.aixyz.domain.TicketDraftDTO;
import com.niuhp.aixyz.dto.request.CreateTicketRequestDTO;
import com.niuhp.aixyz.dto.response.TicketCreatedDTO;
import com.niuhp.aixyz.dto.response.TicketDraftResponseDTO;
import com.niuhp.aixyz.exception.ProcessingError;
import com.niuhp.aixyz.exception.TicketProcessingException;
import com.niuhp.aixyz.model.ExtractionResult;
import com.niuhp.aixyz.model.ModelConcurrencyLimiter;
import com.niuhp.aixyz.model.ModelExtractionDTO;
import com.niuhp.aixyz.model.TicketModelGateway;
import com.niuhp.aixyz.repository.TicketRepository;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * 校验并保存待审核草稿；只有人工确认接口才能创建本地工单。
 *
 * @author niuhaipeng
 */
@Service
public class TicketDraftService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketDraftService.class);

    /** 外部模型边界。 */
    private final TicketModelGateway modelGateway;

    /** Bean Validation 校验器。 */
    private final Validator validator;

    private final TicketRepository ticketRepository;

    private final JsonMapper jsonMapper;

    /** 保护本地演示服务的并发额度。 */
    private final ModelConcurrencyLimiter limiter;

    public TicketDraftService(TicketModelGateway modelGateway, Validator validator, TicketRepository ticketRepository,
            JsonMapper jsonMapper, ModelConcurrencyLimiter limiter) {
        this.modelGateway = modelGateway;
        this.validator = validator;
        this.ticketRepository = ticketRepository;
        this.jsonMapper = jsonMapper;
        this.limiter = limiter;
    }

    public TicketDraftResponseDTO generate(String description, String requestId) {
        limiter.acquirePermit();
        long startedAt = System.nanoTime();
        try {
            return complete(modelGateway.extract(description), requestId, startedAt);
        } finally {
            limiter.releasePermit();
        }
    }

    TicketDraftResponseDTO complete(ExtractionResult result, String requestId, long startedAt) {
        validate(result);
        TicketDraftResponseDTO response = toResponse(result, requestId);
        if (response.getDraft() != null) {
            String draftId = UUID.randomUUID().toString();
            response.setDraftId(draftId);
            try {
                ticketRepository.saveDraft(draftId, jsonMapper.writeValueAsString(response.getDraft()));
            } catch (JacksonException exception) {
                throw new TicketProcessingException(ProcessingError.INTERNAL_ERROR);
            }
        }
        response.setElapsedMillis(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
        LOGGER.info("ticket_draft requestId={} status={} elapsedMillis={}",
                requestId, response.getStatus(), response.getElapsedMillis());
        return response;
    }

    public TicketCreatedDTO create(CreateTicketRequestDTO request) {
        TicketDraftDTO draft = request.getDraft();
        if (!validator.validate(draft).isEmpty() || !StringUtils.hasText(draft.getSystem())
                || !StringUtils.hasText(draft.getModule()) || !StringUtils.hasText(draft.getActualResult())
                || !StringUtils.hasText(draft.getExpectedResult()) || !StringUtils.hasText(draft.getVersion())) {
            throw new TicketProcessingException(ProcessingError.DRAFT_INCOMPLETE);
        }
        String ticketId = ticketRepository.createConfirmed(request.getDraftId(), draft);
        return new TicketCreatedDTO(ticketId, "CREATED");
    }

    private void validate(ExtractionResult result) {
        if (result == null || result.getExtraction() == null) {
            throw new TicketProcessingException(ProcessingError.INVALID_MODEL_OUTPUT);
        }
        ModelExtractionDTO extraction = result.getExtraction();
        if (!validator.validate(extraction).isEmpty()) {
            throw new TicketProcessingException(ProcessingError.INVALID_MODEL_OUTPUT);
        }
        boolean relevant = Boolean.TRUE.equals(extraction.getRelevant());
        if (relevant == (extraction.getDraft() == null)) {
            throw new TicketProcessingException(ProcessingError.INVALID_MODEL_OUTPUT);
        }
    }

    private TicketDraftResponseDTO toResponse(ExtractionResult result, String requestId) {
        ModelExtractionDTO extraction = result.getExtraction();
        TicketDraftResponseDTO response = new TicketDraftResponseDTO();
        response.setRequestId(requestId);
        response.setUsage(result.getUsage());
        response.setModel(result.getModel());
        if (!Boolean.TRUE.equals(extraction.getRelevant())) {
            response.setStatus(DraftStatus.UNSUPPORTED);
            response.setMissingFields(List.of());
            return response;
        }
        TicketDraftDTO draft = extraction.getDraft();
        List<String> missingFields = missingFields(draft);
        response.setDraft(draft);
        response.setMissingFields(missingFields);
        response.setStatus(missingFields.isEmpty() ? DraftStatus.READY : DraftStatus.NEEDS_INFO);
        return response;
    }

    private List<String> missingFields(TicketDraftDTO draft) {
        List<String> missing = new ArrayList<>(5);
        addIfMissing(missing, draft.getSystem(), "系统名称");
        addIfMissing(missing, draft.getModule(), "功能模块");
        addIfMissing(missing, draft.getActualResult(), "实际现象");
        addIfMissing(missing, draft.getExpectedResult(), "预期结果");
        addIfMissing(missing, draft.getVersion(), "系统版本");
        return missing;
    }

    private void addIfMissing(List<String> missing, String value, String label) {
        if (!StringUtils.hasText(value)) {
            missing.add(label);
        }
    }
}
