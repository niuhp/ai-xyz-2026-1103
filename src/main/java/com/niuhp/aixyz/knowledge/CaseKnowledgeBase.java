package com.niuhp.aixyz.knowledge;

import jakarta.validation.Validator;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import tools.jackson.databind.json.JsonMapper;

/**
 * 小型教学案例库：本地 TF-IDF 词项向量 + 余弦检索，不冒充语义 Embedding。
 *
 * @author niuhaipeng
 */
@Component
public class CaseKnowledgeBase {

    public static final String CORPUS_VERSION = "teaching-20261111-v1";
    public static final String RETRIEVAL_METHOD = "LOCAL_TFIDF_COSINE";
    private static final Pattern TERMS = Pattern.compile("[\\p{IsHan}]+|[a-z0-9_]+");
    private static final Pattern CASE_ID = Pattern.compile("KB-\\d{3}");
    private static final Set<String> STOP_TERMS = Set.of("系统", "问题", "出现", "希望", "现在", "教学", "正常");
    private static final int MAX_RESULTS = 3;
    private static final int MIN_TERM_LENGTH = 2;
    private static final int MIN_SHARED_TERMS = 2;
    private final List<KnowledgeCaseDTO> documents;
    private final Map<String, Double> inverseFrequency;
    private final List<Map<String, Double>> vectors;
    private final double threshold;

    public CaseKnowledgeBase(JsonMapper jsonMapper, Validator validator,
            @Value("${app.knowledge.similarity-threshold:0.16}") double threshold) throws IOException {
        Assert.isTrue(Double.isFinite(threshold) && threshold > 0 && threshold <= 1, "检索阈值必须大于 0 且不超过 1");
        this.threshold = threshold;
        try (var input = new ClassPathResource("knowledge/cases.json").getInputStream()) {
            documents = List.of(jsonMapper.readValue(input, KnowledgeCaseDTO[].class));
        }
        Set<String> ids = new HashSet<>();
        for (KnowledgeCaseDTO document : documents) {
            Assert.isTrue(validator.validate(document).isEmpty() && CASE_ID.matcher(document.getId()).matches()
                    && Boolean.TRUE.equals(document.getTeachingOnly()) && ids.add(document.getId()),
                    "案例库必须为有效、编号唯一且明确标注的教学样例");
        }
        Assert.isTrue(!documents.isEmpty() && documents.size() <= 100, "本地教学索引只接受 1 至 100 条短案例");
        List<Map<String, Integer>> counts = documents.stream().map(document -> terms(document.getTitle() + " "
                + document.getModule() + " " + document.getSymptoms())).toList();
        Map<String, Integer> frequency = new HashMap<>(256);
        counts.forEach(document -> document.keySet().forEach(term -> frequency.merge(term, 1, Integer::sum)));
        Map<String, Double> idf = new HashMap<>(frequency.size());
        frequency.forEach((term, count) -> idf.put(term, Math.log((documents.size() + 1.0) / (count + 1.0)) + 1.0));
        inverseFrequency = Map.copyOf(idf);
        vectors = counts.stream().map(this::vector).toList();
    }

    public List<CaseMatchDTO> search(String query) {
        Map<String, Double> queryVector = vector(terms(query));
        List<CaseMatchDTO> matches = new ArrayList<>(MAX_RESULTS);
        for (int index = 0; index < documents.size(); index++) {
            Map<String, Double> document = vectors.get(index);
            long shared = queryVector.keySet().stream().filter(document::containsKey).count();
            double score = queryVector.entrySet().stream().mapToDouble(entry ->
                    entry.getValue() * document.getOrDefault(entry.getKey(), 0.0)).sum();
            if (shared >= MIN_SHARED_TERMS && score >= threshold) {
                CaseMatchDTO match = new CaseMatchDTO();
                match.setDocument(documents.get(index));
                match.setScore(Math.min(1.0, score));
                matches.add(match);
            }
        }
        return matches.stream().sorted(Comparator.comparingDouble(CaseMatchDTO::getScore).reversed()
                .thenComparing(match -> match.getDocument().getId())).limit(MAX_RESULTS).toList();
    }

    public int size() {
        return documents.size();
    }

    public double getThreshold() {
        return threshold;
    }

    private Map<String, Integer> terms(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        Matcher matcher = TERMS.matcher(normalized);
        Map<String, Integer> counts = new HashMap<>(128);
        while (matcher.find()) {
            String token = matcher.group();
            if (Character.UnicodeScript.of(token.codePointAt(0)) == Character.UnicodeScript.HAN) {
                int[] points = token.codePoints().toArray();
                for (int index = 0; index + 1 < points.length; index++) {
                    add(counts, new String(points, index, MIN_TERM_LENGTH));
                }
            } else {
                add(counts, token);
            }
        }
        return counts;
    }

    private void add(Map<String, Integer> counts, String term) {
        if (term.length() >= MIN_TERM_LENGTH && !STOP_TERMS.contains(term)) {
            counts.merge(term, 1, Integer::sum);
        }
    }

    private Map<String, Double> vector(Map<String, Integer> counts) {
        Map<String, Double> result = new HashMap<>(counts.size());
        // 查询和文档使用同一词表/IDF；OOV 词无维度，词表变化须重建整个索引。
        counts.forEach((term, count) -> {
            Double idf = inverseFrequency.get(term);
            if (idf != null) {
                result.put(term, (1 + Math.log(count)) * idf);
            }
        });
        double norm = Math.sqrt(result.values().stream().mapToDouble(value -> value * value).sum());
        if (norm > 0) {
            result.replaceAll((term, value) -> value / norm);
        }
        return Map.copyOf(result);
    }
}
