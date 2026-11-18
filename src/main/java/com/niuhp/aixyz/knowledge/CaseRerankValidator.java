package com.niuhp.aixyz.knowledge;

import com.niuhp.aixyz.exception.ProcessingError;
import com.niuhp.aixyz.exception.TicketProcessingException;
import com.niuhp.aixyz.model.ModelRerankDTO;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 核对候选白名单、覆盖率、分级及摘录；不能证明模型判断本身正确。
 *
 * @author niuhaipeng
 */
@Component
public class CaseRerankValidator {

    public void validate(ModelRerankDTO output, List<CaseMatchDTO> candidates) {
        if (output == null || output.getAssessments() == null
                || output.getAssessments().size() != candidates.size()) {
            fail();
        }
        Map<String, KnowledgeCaseDTO> allowed = new HashMap<>(candidates.size());
        for (CaseMatchDTO candidate : candidates) {
            allowed.put(candidate.getDocument().getId(), candidate.getDocument());
        }
        Set<String> seen = new HashSet<>(candidates.size());
        for (var item : output.getAssessments()) {
            if (item == null || !allowed.containsKey(item.getCaseId()) || !seen.add(item.getCaseId())
                    || item.getGrade() == null || item.getGrade() < 0 || item.getGrade() > 2
                    || !StringUtils.hasText(item.getReason()) || item.getReason().length() > 160
                    || item.getEvidence() == null || item.getEvidence().length() > 240) {
                fail();
            }
            KnowledgeCaseDTO document = allowed.get(item.getCaseId());
            String quote = item.getEvidence();
            if (item.getGrade() > 0 && !StringUtils.hasText(quote)) {
                fail();
            }
            if (StringUtils.hasText(quote) && !document.getSymptoms().contains(quote)
                    && !document.getApplicability().contains(quote)) {
                fail();
            }
        }
    }

    private void fail() {
        throw new TicketProcessingException(ProcessingError.INVALID_RERANK_OUTPUT);
    }
}
