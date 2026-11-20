package com.niuhp.aixyz.evaluation;

import com.niuhp.aixyz.config.EmbeddingProperties;
import com.niuhp.aixyz.dto.response.RerankResultDTO;
import com.niuhp.aixyz.dto.response.RetrievalResultDTO;
import com.niuhp.aixyz.knowledge.CaseKnowledgeBase;
import com.niuhp.aixyz.service.CaseRerankService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import tools.jackson.databind.json.JsonMapper;

/**
 * 仅显式开启时批量评测，无HTTP批量入口；顺序调用，不自动重跑失败样本。
 *
 * @author niuhaipeng
 */
@Component
@ConditionalOnProperty(name = "app.evaluation.enabled", havingValue = "true")
public class EvaluationRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationRunner.class);
    private final EvaluationDatasetLoader loader;
    private final CaseRerankService service;
    private final EvaluationReportWriter writer;
    private final CaseKnowledgeBase corpus;
    private final EmbeddingProperties embedding;
    private final Environment environment;
    private final JsonMapper json;

    public EvaluationRunner(EvaluationDatasetLoader loader, CaseRerankService service, EvaluationReportWriter writer,
            CaseKnowledgeBase corpus, EmbeddingProperties embedding, Environment environment, JsonMapper json) {
        this.loader = loader;
        this.service = service;
        this.writer = writer;
        this.corpus = corpus;
        this.embedding = embedding;
        this.environment = environment;
        this.json = json;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String location = environment.getProperty("app.evaluation.dataset",
                "classpath:evaluation/retrieval-benchmark.json");
        EvaluationDatasetDTO dataset = loader.load(location);
        String split = environment.getProperty("app.evaluation.split", "tune");
        Assert.isTrue(List.of("tune", "regression").contains(split), "只允许 tune 或 regression 划分");
        List<EvaluationSampleDTO> samples = dataset.getSamples().stream()
                .filter(sample -> split.equals(sample.getSplit())).toList();
        Assert.isTrue(!samples.isEmpty(), "所选划分没有样本");
        EvaluationReportDTO report = new EvaluationReportDTO();
        report.setRunLabel(environment.getProperty("app.evaluation.label", "local-evaluation"));
        report.setCreatedAt(OffsetDateTime.now().toString());
        report.setDatasetVersion(dataset.getVersion());
        report.setDatasetSha256(digest(json.writeValueAsString(dataset)));
        report.setCorpusVersion(CaseKnowledgeBase.CORPUS_VERSION);
        report.setCorpusSha256(digest(json.writeValueAsString(corpus.getDocuments())));
        report.setSplit(split);
        report.setConfiguration(configuration());
        report.setConfigurationSha256(digest(json.writeValueAsString(report.getConfiguration())));
        Path output = Path.of(environment.getProperty("app.evaluation.output", ".local/evaluation/latest"));
        Assert.isTrue(!Files.exists(output.resolve("report.json")) && !Files.exists(output.resolve("report.html")),
                "输出目录已有报告，请选择新目录以保留原记录");
        String baseline = environment.getProperty("app.evaluation.baseline", "");
        EvaluationReportDTO previous = baseline.isBlank() ? null
                : json.readValue(Files.readString(Path.of(baseline)), EvaluationReportDTO.class);
        if (previous != null) {
            Assert.isTrue(previous.getDatasetSha256().equals(report.getDatasetSha256())
                    && previous.getCorpusSha256().equals(report.getCorpusSha256())
                    && previous.getSplit().equals(report.getSplit()), "基线数据、语料或划分不同，停止调用模型");
        }
        List<EvaluationRowDTO> rows = new ArrayList<>(samples.size());
        for (EvaluationSampleDTO sample : samples) {
            var result = service.compare(sample.getDescription(), "eval-" + sample.getId());
            EvaluationRowDTO row = new EvaluationRowDTO();
            row.setSample(sample);
            var rank = result.getRerank();
            List<EvaluationObservationDTO> observations = new ArrayList<>(EvaluationScorer.METHODS.size());
            observations.add(observe("TFIDF", result.getRetrieval().getLexical()));
            observations.add(observe("SEMANTIC", result.getRetrieval().getSemantic()));
            observations.add(observe("HYBRID", result.getRetrieval().getHybrid()));
            observations.add(observe(rank, result.getElapsedMillis()));
            row.setObservations(observations);
            rows.add(row);
            LOGGER.info("evaluation_sample id={} rerankStatus={}", sample.getId(), rank.getStatus());
        }
        report.setRows(rows);
        report.setMetrics(EvaluationScorer.summarize(rows));
        report.setChanges(List.of());
        report.setComparisonNote("未指定基线。本报告比较同次调用的四个方法；没有进行提示词 A/B 实验。");
        if (previous != null) {
            report.setChanges(EvaluationComparison.compare(previous, report));
            report.setComparisonNote(previous.getConfigurationSha256().equals(report.getConfigurationSha256())
                    ? "配置指纹相同，逐题比较两次运行，差异也可能来自模型或上游波动。"
                    : "配置指纹不同，需核对模型、提示词和参数；不可把变化直接归因于某一项改动。");
        }
        writer.write(report, output);
        LOGGER.info("evaluation_complete split={} samples={} output={}", split, rows.size(), output.toAbsolutePath());
    }

    private EvaluationObservationDTO observe(String method, RetrievalResultDTO source) {
        EvaluationObservationDTO result = new EvaluationObservationDTO();
        result.setMethod(method);
        result.setStatus(source.getStatus());
        result.setCompleted(List.of("MATCHED", "NO_MATCH").contains(source.getStatus()));
        result.setActualIds(source.getMatches().stream().map(item -> item.getDocument().getId()).toList());
        result.setElapsedMillis(source.getElapsedMillis());
        result.setModel("SEMANTIC".equals(method) ? embedding.getModel() : "local");
        return result;
    }

    private EvaluationObservationDTO observe(RerankResultDTO source, long totalMillis) {
        EvaluationObservationDTO result = new EvaluationObservationDTO();
        result.setMethod("RERANK");
        result.setStatus(source.getStatus());
        result.setCompleted(List.of("RERANKED", "INSUFFICIENT_EVIDENCE", "NO_CANDIDATES")
                .contains(source.getStatus()));
        result.setActualIds(source.getMatches().stream().map(item -> item.getDocument().getId()).toList());
        result.setElapsedMillis(totalMillis);
        result.setModel(source.getModel());
        result.setTotalTokens(source.getUsage() == null ? null : source.getUsage().getTotalTokens());
        return result;
    }

    private Map<String, Object> configuration() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", "LIVE_SERVICES");
        result.put("sourceRevision", environment.getProperty("app.evaluation.revision", "unspecified"));
        result.put("rerankMaxRetries", 0);
        result.put("gradeRetained", 2);
        result.put("chatModel", environment.getProperty("spring.ai.openai.chat.model", ""));
        result.put("chatBaseUrl", environment.getProperty("spring.ai.openai.base-url", ""));
        result.put("embeddingModel", embedding.getModel());
        result.put("embeddingBaseUrl", embedding.getBaseUrl());
        result.put("embeddingEnabled", embedding.getEnabled());
        result.put("embeddingTimeout", embedding.getTimeout().toString());
        result.put("queryPrefix", embedding.getQueryPrefix());
        result.put("documentPrefix", embedding.getDocumentPrefix());
        result.put("lexicalThreshold", corpus.getThreshold());
        result.put("semanticThreshold", embedding.getThreshold());
        result.put("rerankTimeout", environment.getProperty("app.knowledge.rerank-timeout", "8s"));
        result.put("rrfK", 60);
        result.put("perRouteLimit", 3);
        result.put("finalLimit", 3);
        result.put("rerankPromptSha256", digest(new ClassPathResource("prompts/case-rerank.txt")
                .getContentAsString(StandardCharsets.UTF_8)));
        result.put("implementation", "evaluation-v1-grade2-filter");
        return result;
    }

    private String digest(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
