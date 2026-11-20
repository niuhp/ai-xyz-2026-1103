package com.niuhp.aixyz.evaluation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 独立统计成功覆盖与条件质量；所有方法均返回最多三条候选。
 *
 * @author niuhaipeng
 */
public final class EvaluationScorer {

    public static final List<String> METHODS = List.of("TFIDF", "SEMANTIC", "HYBRID", "RERANK");

    private EvaluationScorer() {
    }

    public static List<EvaluationMetricDTO> summarize(List<EvaluationRowDTO> rows) {
        List<EvaluationMetricDTO> result = new ArrayList<>(METHODS.size());
        for (String method : METHODS) {
            result.add(score(rows, method));
        }
        return result;
    }

    private static EvaluationMetricDTO score(List<EvaluationRowDTO> rows, String method) {
        int completed = 0;
        int exact = 0;
        int positives = 0;
        int hits = 0;
        int falseRejections = 0;
        int negatives = 0;
        int abstentions = 0;
        int nonempty = 0;
        int tokenObservations = 0;
        long tokens = 0;
        long elapsed = 0;
        double precision = 0;
        double recall = 0;
        for (EvaluationRowDTO row : rows) {
            EvaluationObservationDTO observation = find(row, method);
            elapsed += observation.getElapsedMillis();
            if (observation.getTotalTokens() != null) {
                tokenObservations++;
                tokens += observation.getTotalTokens();
            }
            if (!Boolean.TRUE.equals(observation.getCompleted())) {
                continue;
            }
            completed++;
            Set<String> expected = new HashSet<>(row.getSample().getExpectedIds());
            List<String> actual = observation.getActualIds();
            long overlap = actual.stream().filter(expected::contains).count();
            if (new HashSet<>(actual).equals(expected)) {
                exact++;
            }
            if (!actual.isEmpty()) {
                nonempty++;
                precision += (double)overlap / actual.size();
            }
            if (expected.isEmpty()) {
                negatives++;
                if (actual.isEmpty()) {
                    abstentions++;
                }
            } else {
                positives++;
                recall += (double)overlap / expected.size();
                if (!actual.isEmpty() && expected.contains(actual.getFirst())) {
                    hits++;
                }
                if (actual.isEmpty()) {
                    falseRejections++;
                }
            }
        }
        EvaluationMetricDTO metric = new EvaluationMetricDTO();
        metric.setMethod(method);
        metric.setTotal(rows.size());
        metric.setCompleted(completed);
        metric.setExact(exact);
        metric.setPositiveCompleted(positives);
        metric.setTop1Hits(hits);
        metric.setFalseRejections(falseRejections);
        metric.setNegativeCompleted(negatives);
        metric.setCorrectAbstentions(abstentions);
        metric.setNonemptyCompleted(nonempty);
        metric.setMacroPrecision(nonempty == 0 ? null : precision / nonempty);
        metric.setMacroRecall(positives == 0 ? null : recall / positives);
        metric.setMeanElapsedMillis(rows.isEmpty() ? null : (double)elapsed / rows.size());
        metric.setTokenObservations(tokenObservations);
        metric.setReportedTotalTokens(tokens);
        return metric;
    }

    public static EvaluationObservationDTO find(EvaluationRowDTO row, String method) {
        return row.getObservations().stream().filter(item -> method.equals(item.getMethod())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("报告缺少方法观测"));
    }
}
