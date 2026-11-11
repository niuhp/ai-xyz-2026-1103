package com.niuhp.aixyz.knowledge;

import com.niuhp.aixyz.service.ProcessingError;
import com.niuhp.aixyz.service.TicketProcessingException;
import jakarta.validation.Validator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 引用只能来自本次检索集合，且必须包含可在原文定位的连续摘录。
 * 结构与摘录校验不等于建议被原文语义支持，仍需人工审核。
 *
 * @author niuhaipeng
 */
@Component
public class AdviceOutputValidator {

    private final Validator validator;

    public AdviceOutputValidator(Validator validator) {
        this.validator = validator;
    }

    public void validate(ModelAdviceDTO advice, AdviceMode mode, List<CaseMatchDTO> matches) {
        if (advice == null || !validator.validate(advice).isEmpty()) {
            invalid();
        }
        boolean supported = Boolean.TRUE.equals(advice.getSupported());
        if (supported == advice.getSteps().isEmpty()) {
            invalid();
        }
        Map<String, KnowledgeCaseDTO> allowed = matches.stream().map(CaseMatchDTO::getDocument)
                .collect(Collectors.toMap(KnowledgeCaseDTO::getId, Function.identity()));
        for (AdviceStepDTO step : advice.getSteps()) {
            boolean hasEvidence = !step.getEvidence().isEmpty();
            boolean needsEvidence = mode == AdviceMode.RETRIEVAL;
            if (hasEvidence != needsEvidence) {
                invalid();
            }
            Set<String> citations = new HashSet<>();
            for (AdviceEvidenceDTO evidence : step.getEvidence()) {
                KnowledgeCaseDTO document = allowed.get(evidence.getCaseId());
                if (document == null || !citations.add(evidence.getCaseId())
                        || !quoted(document, evidence.getQuote())) {
                    invalid();
                }
            }
        }
    }

    private boolean quoted(KnowledgeCaseDTO document, String quote) {
        // 不能跨字段拼接摘录，更不能用模型生成的链接替代真实来源。
        return document.getSymptoms().contains(quote) || document.getResolution().contains(quote)
                || document.getApplicability().contains(quote);
    }

    private void invalid() {
        throw new TicketProcessingException(ProcessingError.INVALID_ADVICE_OUTPUT);
    }
}
