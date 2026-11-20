package com.niuhp.aixyz.evaluation;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

/**
 * 手算非对称样例验证指标、失败分母及退步判定，不调用模型。
 *
 * @author niuhaipeng
 */
class EvaluationScorerTest {

    private static final String FIRST = "KB-001";
    private static final String SECOND = "KB-002";
    private static final String THIRD = "KB-003";
    private static final String RERANK = "RERANK";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void shouldUseDocumentedDenominatorsAndKeepFailureOutOfQuality() {
        var rows = List.of(row("a", List.of(FIRST, SECOND), List.of(FIRST, THIRD), true),
                row("b", List.of(FIRST), List.of(), true),
                row("c", List.of(), List.of(THIRD), true),
                row("d", List.of(), List.of(), true),
                row("e", List.of(FIRST), List.of(), false));
        var metric = EvaluationScorer.summarize(rows).getLast();
        assertThat(metric.getTotal()).isEqualTo(5);
        assertThat(metric.getCompleted()).isEqualTo(4);
        assertThat(metric.getPositiveCompleted()).isEqualTo(2);
        assertThat(metric.getTop1Hits()).isEqualTo(1);
        assertThat(metric.getFalseRejections()).isEqualTo(1);
        assertThat(metric.getNegativeCompleted()).isEqualTo(2);
        assertThat(metric.getCorrectAbstentions()).isEqualTo(1);
        assertThat(metric.getExact()).isEqualTo(1);
        assertThat(metric.getNonemptyCompleted()).isEqualTo(2);
        assertThat(metric.getMacroPrecision()).isCloseTo(0.25, offset(0.000001));
        assertThat(metric.getMacroRecall()).isCloseTo(0.25, offset(0.000001));
        assertThat(metric.getMeanElapsedMillis()).isEqualTo(100.0);
        assertThat(metric.getReportedTotalTokens()).isZero();
        assertThat(metric.getTokenObservations()).isZero();
    }

    @Test
    void shouldKeepUndefinedValuesNullInsteadOfInventingPerfectScores() {
        var metric = EvaluationScorer.summarize(List.of(row("empty", List.of(), List.of(), true))).getLast();
        assertThat(metric.getMacroPrecision()).isNull();
        assertThat(metric.getMacroRecall()).isNull();
        assertThat(metric.getExact()).isEqualTo(1);
    }

    @Test
    void shouldDetectLostRelevantCaseEvenWhenOtherInterferenceWasRemoved() {
        var before = report(List.of(row("regression", List.of(FIRST), List.of(FIRST), true),
                row("improvement", List.of(FIRST), List.of(FIRST, SECOND), true)));
        var after = report(List.of(row("regression", List.of(FIRST), List.of(), true),
                row("improvement", List.of(FIRST), List.of(FIRST), true)));
        var changes = EvaluationComparison.compare(before, after).stream()
                .filter(item -> RERANK.equals(item.getMethod())).toList();
        assertThat(changes).extracting(EvaluationChangeDTO::getKind).containsExactly("REGRESSION", "IMPROVED");
    }

    @Test
    void shouldTreatTimeoutAsAvailabilityRegressionRatherThanFalseRejection() {
        var before = report(List.of(row("timeout", List.of(FIRST), List.of(FIRST), true)));
        var after = report(List.of(row("timeout", List.of(FIRST), List.of(), false)));
        assertThat(EvaluationComparison.compare(before, after)).extracting(EvaluationChangeDTO::getKind)
                .containsOnly("AVAILABILITY_REGRESSION");
        after.setDatasetSha256("different");
        assertThatThrownBy(() -> EvaluationComparison.compare(before, after)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectChangedLabelsAndMissingRows() {
        var before = report(List.of(row("label", List.of(FIRST), List.of(FIRST), true)));
        var after = report(List.of(row("label", List.of(SECOND), List.of(FIRST), true)));
        assertThatThrownBy(() -> EvaluationComparison.compare(before, after)).isInstanceOf(IllegalArgumentException.class);
        after.setRows(List.of());
        assertThatThrownBy(() -> EvaluationComparison.compare(before, after)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldEscapeDescriptionsInHtmlAndShowIncompleteRatherThanScoreThem() {
        var row = row("html", List.of(FIRST), List.of(), false);
        row.getSample().setDescription("<script>alert('x')</script>");
        var report = report(List.of(row));
        String html = new EvaluationReportWriter(JSON).render(report);
        assertThat(html).contains("&lt;script&gt;", "未完成，不评分", "未定义").doesNotContain("<script>");
    }

    static EvaluationRowDTO row(String id, List<String> expected, List<String> actual, boolean complete) {
        EvaluationSampleDTO sample = new EvaluationSampleDTO();
        sample.setId(id);
        sample.setDescription("description-" + id);
        sample.setExpectedIds(expected);
        sample.setRationale("按症状标注");
        sample.setSplit("regression");
        EvaluationRowDTO row = new EvaluationRowDTO();
        row.setSample(sample);
        row.setObservations(EvaluationScorer.METHODS.stream().map(method -> {
            EvaluationObservationDTO observation = new EvaluationObservationDTO();
            observation.setMethod(method);
            observation.setCompleted(complete);
            observation.setStatus(complete ? "RERANKED" : "MODEL_TIMEOUT");
            observation.setActualIds(actual);
            observation.setElapsedMillis(100L);
            return observation;
        }).toList());
        return row;
    }

    static EvaluationReportDTO report(List<EvaluationRowDTO> rows) {
        EvaluationReportDTO report = new EvaluationReportDTO();
        report.setDatasetSha256("same-dataset");
        report.setCorpusSha256("same-corpus");
        report.setSplit("regression");
        report.setRows(rows);
        report.setMetrics(EvaluationScorer.summarize(rows));
        report.setChanges(List.of());
        report.setConfiguration(Map.of());
        return report;
    }
}
