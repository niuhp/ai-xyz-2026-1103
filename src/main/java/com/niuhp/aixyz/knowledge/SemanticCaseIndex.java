package com.niuhp.aixyz.knowledge;

import com.niuhp.aixyz.config.EmbeddingProperties;
import com.niuhp.aixyz.exception.ProcessingError;
import com.niuhp.aixyz.exception.TicketProcessingException;
import com.niuhp.aixyz.model.CaseEmbeddingGateway;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import tools.jackson.databind.json.JsonMapper;

/**
 * 小语料的内存向量索引；首次使用时构建，重启重建，不持久化敏感向量。
 *
 * @author niuhaipeng
 */
@Component
public class SemanticCaseIndex {

    public static final String METHOD = "EMBEDDING_COSINE";
    private static final int MAX_RESULTS = 3;
    private static final int MAX_DIMENSIONS = 16384;
    private final CaseKnowledgeBase knowledgeBase;
    private final CaseEmbeddingGateway gateway;
    private final EmbeddingProperties properties;
    private final String fingerprint;
    private final ReentrantLock buildLock = new ReentrantLock();
    private volatile List<float[]> vectors = List.of();

    public SemanticCaseIndex(CaseKnowledgeBase knowledgeBase, CaseEmbeddingGateway gateway,
            EmbeddingProperties properties, JsonMapper jsonMapper) {
        this.knowledgeBase = knowledgeBase;
        this.gateway = gateway;
        this.properties = properties;
        Assert.isTrue(Double.isFinite(properties.getThreshold()) && properties.getThreshold() > 0
                && properties.getThreshold() <= 1, "语义检索阈值必须大于 0 且不超过 1");
        fingerprint = digest(jsonMapper.writeValueAsString(List.of(CaseKnowledgeBase.CORPUS_VERSION,
                "title-module-symptoms-nfkc-v1", properties.getBaseUrl(), properties.getModel(),
                properties.getQueryPrefix(), properties.getDocumentPrefix(), knowledgeBase.getDocuments())));
    }

    public List<CaseMatchDTO> search(String query) {
        if (!properties.getEnabled()) {
            throw new TicketProcessingException(ProcessingError.EMBEDDING_DISABLED);
        }
        List<float[]> index = index();
        float[] queryVector = normalized(gateway.embed(List.of(properties.getQueryPrefix() + clean(query))).getFirst(),
                index.getFirst().length);
        List<CaseMatchDTO> matches = new ArrayList<>(MAX_RESULTS);
        for (int position = 0; position < index.size(); position++) {
            float[] document = index.get(position);
            double score = 0;
            for (int dimension = 0; dimension < document.length; dimension++) {
                score += (double)queryVector[dimension] * document[dimension];
            }
            if (score >= properties.getThreshold()) {
                CaseMatchDTO match = new CaseMatchDTO();
                match.setDocument(knowledgeBase.getDocuments().get(position));
                match.setScore(Math.min(1.0, score));
                matches.add(match);
            }
        }
        return matches.stream().sorted(Comparator.comparingDouble(CaseMatchDTO::getScore).reversed()
                .thenComparing(match -> match.getDocument().getId())).limit(MAX_RESULTS).toList();
    }

    public Map<String, Object> information() {
        List<float[]> current = vectors;
        return Map.of("enabled", properties.getEnabled(), "state", !properties.getEnabled() ? "DISABLED"
                : current.isEmpty() ? "NOT_BUILT" : "READY", "model", properties.getModel(),
                "dimensions", current.isEmpty() ? 0 : current.get(0).length, "fingerprint", fingerprint,
                "threshold", properties.getThreshold(), "method", METHOD);
    }

    public double getThreshold() {
        return properties.getThreshold();
    }

    private List<float[]> index() {
        if (!vectors.isEmpty()) {
            return vectors;
        }
        if (!buildLock.tryLock()) {
            throw new TicketProcessingException(ProcessingError.BUSY);
        }
        try {
            if (vectors.isEmpty()) {
                List<String> texts = knowledgeBase.getDocuments().stream().map(document ->
                        properties.getDocumentPrefix() + clean(document.getTitle() + "\n" + document.getModule()
                                + "\n" + document.getSymptoms())).toList();
                List<float[]> received = gateway.embed(texts);
                int dimensions = received.getFirst().length;
                // 整批成功才发布，失败保留空索引，后续可由用户主动重试。
                vectors = received.stream().map(vector -> normalized(vector, dimensions)).toList();
            }
            return vectors;
        } finally {
            buildLock.unlock();
        }
    }

    private float[] normalized(float[] vector, int dimensions) {
        if (vector == null || dimensions == 0 || dimensions > MAX_DIMENSIONS || vector.length != dimensions) {
            throw new TicketProcessingException(ProcessingError.INVALID_EMBEDDING_OUTPUT);
        }
        double squared = 0;
        for (float value : vector) {
            if (!Float.isFinite(value)) {
                throw new TicketProcessingException(ProcessingError.INVALID_EMBEDDING_OUTPUT);
            }
            squared += (double)value * value;
        }
        if (squared == 0) {
            throw new TicketProcessingException(ProcessingError.INVALID_EMBEDDING_OUTPUT);
        }
        double norm = Math.sqrt(squared);
        float[] unit = new float[vector.length];
        for (int dimension = 0; dimension < vector.length; dimension++) {
            unit[dimension] = (float)(vector[dimension] / norm);
        }
        return unit;
    }

    private String clean(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC).strip();
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行环境缺少 SHA-256", exception);
        }
    }
}
