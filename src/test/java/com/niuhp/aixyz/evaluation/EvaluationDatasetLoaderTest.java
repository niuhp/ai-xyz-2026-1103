package com.niuhp.aixyz.evaluation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 阻止错标、重复标签和跨集重复问题，并验证发布的冻结数据集。
 *
 * @author niuhaipeng
 */
class EvaluationDatasetLoaderTest {

    private static final int CORPUS_SIZE = 16;

    private static final String FIRST = "KB-001";

    @Test
    void shouldRejectUnknownOrDuplicateLabels() {
        var sample = EvaluationScorerTest.row("id", List.of("KB-999"), List.of(), true).getSample();
        var dataset = dataset(List.of(sample));
        assertThatThrownBy(() -> EvaluationDatasetLoader.validate(dataset, Set.of(FIRST)))
                .isInstanceOf(IllegalArgumentException.class);
        sample.setExpectedIds(List.of(FIRST, FIRST));
        assertThatThrownBy(() -> EvaluationDatasetLoader.validate(dataset, Set.of(FIRST)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectDuplicateNormalizedQuestionsAcrossSplits() {
        var a = EvaluationScorerTest.row("a", List.of(FIRST), List.of(), true).getSample();
        var b = EvaluationScorerTest.row("b", List.of(FIRST), List.of(), true).getSample();
        a.setDescription("登录\t后４０１");
        b.setDescription("登录后401");
        a.setSplit("tune");
        var dataset = dataset(List.of(a, b));
        assertThatThrownBy(() -> EvaluationDatasetLoader.validate(dataset, Set.of(FIRST)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldLoadFrozenTeachingSetWithSeparateSplitsAndMultiLabelQuestions() throws IOException {
        var json = JsonMapper.builder().build();
        EvaluationDatasetDTO dataset = json.readValue(new ClassPathResource("evaluation/retrieval-benchmark.json")
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8), EvaluationDatasetDTO.class);
        List<String> ids = new ArrayList<>(CORPUS_SIZE);
        for (int index = 1; index <= CORPUS_SIZE; index++) {
            ids.add(String.format(java.util.Locale.ROOT, "KB-%03d", index));
        }
        EvaluationDatasetLoader.validate(dataset, Set.copyOf(ids));
        assertThat(dataset.getSamples()).hasSize(18);
        assertThat(dataset.getSamples().stream().filter(item -> "tune".equals(item.getSplit()))).hasSize(6);
        assertThat(dataset.getSamples().stream().filter(item -> item.getExpectedIds().size() > 1)).hasSize(2);
    }

    private EvaluationDatasetDTO dataset(List<EvaluationSampleDTO> samples) {
        EvaluationDatasetDTO dataset = new EvaluationDatasetDTO();
        dataset.setVersion("test-v1");
        dataset.setLabelPolicy("测试标注口径");
        dataset.setSamples(samples);
        return dataset;
    }
}
