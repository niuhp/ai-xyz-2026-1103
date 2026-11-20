package com.niuhp.aixyz.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.util.Assert;

/**
 * 仅比较同一数据、语料与划分；配置差异由报告公开，逐题报告改善和退步。
 *
 * @author niuhaipeng
 */
public final class EvaluationComparison {

    private EvaluationComparison() {
    }

    public static List<EvaluationChangeDTO> compare(EvaluationReportDTO before, EvaluationReportDTO after) {
        Assert.isTrue(Objects.equals(before.getDatasetSha256(), after.getDatasetSha256())
                && Objects.equals(before.getCorpusSha256(), after.getCorpusSha256())
                && Objects.equals(before.getSplit(), after.getSplit()), "数据、语料或划分不同，禁止比较");
        Map<String, EvaluationRowDTO> previous = new HashMap<>(before.getRows().size());
        for (EvaluationRowDTO row : before.getRows()) {
            Assert.isTrue(previous.put(row.getSample().getId(), row) == null, "基线样本编号重复");
        }
        Assert.isTrue(previous.size() == after.getRows().size(), "两次报告样本数量不同");
        List<EvaluationChangeDTO> changes = new ArrayList<>();
        for (EvaluationRowDTO row : after.getRows()) {
            EvaluationRowDTO old = previous.remove(row.getSample().getId());
            Assert.isTrue(old != null && Objects.equals(old.getSample().getDescription(), row.getSample().getDescription())
                    && Objects.equals(old.getSample().getExpectedIds(), row.getSample().getExpectedIds()),
                    "报告样本或标注不一致");
            for (String method : EvaluationScorer.METHODS) {
                var a = EvaluationScorer.find(old, method);
                var b = EvaluationScorer.find(row, method);
                boolean ac = Boolean.TRUE.equals(a.getCompleted());
                boolean bc = Boolean.TRUE.equals(b.getCompleted());
                String kind;
                if (!ac || !bc) {
                    kind = ac && !bc ? "AVAILABILITY_REGRESSION" : !ac && bc ? "RECOVERED" : "BOTH_INCOMPLETE";
                } else {
                    int oldErrors = difference(row.getSample().getExpectedIds(), a.getActualIds());
                    int newErrors = difference(row.getSample().getExpectedIds(), b.getActualIds());
                    kind = newErrors > oldErrors ? "REGRESSION" : newErrors < oldErrors ? "IMPROVED"
                            : a.getActualIds().equals(b.getActualIds()) ? "UNCHANGED" : "CHANGED_REVIEW";
                }
                EvaluationChangeDTO change = new EvaluationChangeDTO();
                change.setSampleId(row.getSample().getId());
                change.setMethod(method);
                change.setKind(kind);
                change.setBefore(a.getActualIds());
                change.setAfter(b.getActualIds());
                changes.add(change);
            }
        }
        return changes;
    }

    private static int difference(List<String> expected, List<String> actual) {
        return (int)(expected.stream().filter(id -> !actual.contains(id)).count()
                + actual.stream().filter(id -> !expected.contains(id)).count());
    }
}
