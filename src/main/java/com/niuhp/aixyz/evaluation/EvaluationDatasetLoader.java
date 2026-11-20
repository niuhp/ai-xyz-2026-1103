package com.niuhp.aixyz.evaluation;

import com.niuhp.aixyz.knowledge.CaseKnowledgeBase;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * 先校验标签和数据划分，失败即停止，避免错误标注污染指标。
 *
 * @author niuhaipeng
 */
@Component
public class EvaluationDatasetLoader {

    private final ResourceLoader resources;
    private final JsonMapper json;
    private final CaseKnowledgeBase corpus;

    public EvaluationDatasetLoader(ResourceLoader resources, JsonMapper json, CaseKnowledgeBase corpus) {
        this.resources = resources;
        this.json = json;
        this.corpus = corpus;
    }

    public EvaluationDatasetDTO load(String location) throws IOException {
        String content = resources.getResource(location).getContentAsString(StandardCharsets.UTF_8);
        EvaluationDatasetDTO dataset = json.readValue(content, EvaluationDatasetDTO.class);
        validate(dataset, corpus.getDocuments().stream().map(item -> item.getId()).collect(Collectors.toSet()));
        return dataset;
    }

    public static void validate(EvaluationDatasetDTO dataset, Set<String> corpusIds) {
        Assert.isTrue(dataset != null && StringUtils.hasText(dataset.getVersion())
                && StringUtils.hasText(dataset.getLabelPolicy()) && dataset.getSamples() != null
                && !dataset.getSamples().isEmpty(), "评测集版本、标注口径与样本不能为空");
        Set<String> ids = new HashSet<>(dataset.getSamples().size());
        Set<String> descriptions = new HashSet<>(dataset.getSamples().size());
        for (EvaluationSampleDTO sample : dataset.getSamples()) {
            Assert.isTrue(sample != null && StringUtils.hasText(sample.getId()) && ids.add(sample.getId())
                    && List.of("tune", "regression").contains(sample.getSplit())
                    && StringUtils.hasText(sample.getDescription()) && sample.getDescription().length() <= 4000
                    && StringUtils.hasText(sample.getRationale()) && sample.getExpectedIds() != null,
                    "样本编号必须唯一，划分、描述、标签与理由必须合法");
            String normalized = Normalizer.normalize(sample.getDescription(), Normalizer.Form.NFKC)
                    .replaceAll("\\s+", "");
            Assert.isTrue(descriptions.add(normalized), "归一化描述不能重复或跨划分复用");
            Assert.isTrue(new HashSet<>(sample.getExpectedIds()).size() == sample.getExpectedIds().size()
                    && corpusIds.containsAll(sample.getExpectedIds()), "标签不能重复或引用不存在的案例");
        }
    }
}
