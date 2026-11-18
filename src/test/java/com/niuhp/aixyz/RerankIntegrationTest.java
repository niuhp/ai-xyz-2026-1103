package com.niuhp.aixyz;

import com.niuhp.aixyz.dto.response.RetrievalCompareResponseDTO;
import com.niuhp.aixyz.dto.response.RetrievalResultDTO;
import com.niuhp.aixyz.knowledge.CaseMatchDTO;
import com.niuhp.aixyz.knowledge.KnowledgeCaseDTO;
import com.niuhp.aixyz.model.ModelConcurrencyLimiter;
import com.niuhp.aixyz.service.RetrievalCompareService;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 经真实 HTTP 与 SDK 校验重排白名单、空结果、故障及额度释放，不证明模型语义效果。
 *
 * @author niuhaipeng
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.config.import=", "app.knowledge.embedding.enabled=false", "spring.ai.openai.api-key=test",
                "app.knowledge.rerank-timeout=200ms", "app.ticket.max-concurrent-requests=1",
                "spring.datasource.url=jdbc:h2:mem:rerank;DB_CLOSE_DELAY=-1"})
class RerankIntegrationTest {

    private static final String GRADE_TWO_JSON = "\"grade\":2";
    private static final String GRADE_STRING_JSON = "\"grade\":\"2\"";
    private static final String BAD_JSON = "not-json";
    private static final String EMPTY_JSON = "{}";
    private static final ThreadPoolExecutor UPSTREAM_EXECUTOR = new ThreadPoolExecutor(2, 2, 0L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(16), runnable -> new Thread(runnable, "rerank-test"),
            new ThreadPoolExecutor.AbortPolicy());
    private static final String FIRST = "KB-001";
    private static final String SECOND = "KB-002";
    private static final String QUESTION = "全年报表一直转圈，只查一周正常";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final AtomicReference<String> CONTENT = new AtomicReference<>();
    private static final AtomicReference<String> FINISH = new AtomicReference<>("stop");
    private static final AtomicInteger STATUS = new AtomicInteger(200);
    private static final AtomicInteger CALLS = new AtomicInteger();
    private static final AtomicInteger DELAY = new AtomicInteger();
    private static final AtomicReference<JsonNode> LAST = new AtomicReference<>();
    private static final HttpServer UPSTREAM = upstream();
    private final HttpClient client = HttpClient.newHttpClient();
    @LocalServerPort
    private int port;
    @MockitoBean
    private RetrievalCompareService retrieval;
    @Autowired
    private ModelConcurrencyLimiter limiter;
    private RetrievalResultDTO source;
    private CaseMatchDTO first;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url", () -> "http://127.0.0.1:" + UPSTREAM.getAddress().getPort() + "/v1");
    }

    @AfterAll
    static void stop() {
        UPSTREAM.stop(0);
        UPSTREAM_EXECUTOR.shutdownNow();
    }

    @BeforeEach
    void reset() {
        CALLS.set(0);
        STATUS.set(200);
        DELAY.set(0);
        FINISH.set("stop");
        first = match(FIRST, "选择全年导出时超时");
        source = new RetrievalResultDTO();
        source.setStatus("MATCHED");
        source.setMatches(List.of(first, match(SECOND, "文件已经下载但部分列为空")));
        RetrievalCompareResponseDTO response = new RetrievalCompareResponseDTO();
        response.setHybrid(source);
        response.setRequestId("test");
        when(retrieval.compare(anyString(), anyString())).thenReturn(response);
        output(List.of(item(FIRST, 2, "全年导出时超时"), item(SECOND, 0, "")));
    }

    @Test
    void shouldKeepOnlyAcceptedCandidatesAndPreserveOriginalScores() throws Exception {
        JsonNode result = post(200).path("rerank");
        assertThat(result.path("status").asString()).isEqualTo("RERANKED");
        assertThat(result.path("matches").size()).isEqualTo(1);
        assertThat(result.path("matches").get(0).path("document").path("id").asString()).isEqualTo(FIRST);
        assertThat(first.getScore()).isEqualTo(0.03);
        assertThat(source.getMatches()).hasSize(2);
        assertThat(CALLS.get()).isEqualTo(1);
        assertThat(LAST.get().path("messages").get(1).path("content").asString()).contains(QUESTION, FIRST, SECOND)
                .doesNotContain("lexicalScore", "semanticScore");
    }

    @Test
    void shouldRejectUnknownDuplicateMissingAndForgedEvidence() throws Exception {
        for (List<Map<String, Object>> invalid : List.of(
                List.of(item("KB-999", 2, "全文"), item(SECOND, 0, "")),
                List.of(item(FIRST, 2, "全年导出时超时"), item(FIRST, 0, "")),
                List.of(item(FIRST, 2, "全年导出时超时")),
                List.of(item(FIRST, 2, "编造的摘录"), item(SECOND, 0, "")),
                List.of(item(FIRST, 3, "全年导出时超时"), item(SECOND, 0, "")),
                List.of(item(FIRST, 1, ""), item(SECOND, 0, "")))) {
            output(invalid);
            assertThat(post(200).path("rerank").path("status").asString()).isEqualTo("INVALID_RERANK_OUTPUT");
        }
        // 多轮失败后仍能取得唯一额度。
        limiter.acquirePermit();
        limiter.releasePermit();
    }

    @Test
    void shouldRejectInvalidJsonTypesUnknownFieldsAndTruncatedOutput() throws Exception {
        String valid = CONTENT.get();
        for (String invalid : List.of(BAD_JSON, valid + EMPTY_JSON, valid.replace(GRADE_TWO_JSON, GRADE_STRING_JSON),
                valid.replace(GRADE_TWO_JSON, "\"grade\":2,\"extra\":true"), "null")) {
            CONTENT.set(invalid);
            assertThat(post(200).path("rerank").path("status").asString()).isEqualTo("INVALID_RERANK_OUTPUT");
        }
        CONTENT.set(valid);
        FINISH.set("length");
        assertThat(post(200).path("rerank").path("status").asString()).isEqualTo("INVALID_RERANK_OUTPUT");
    }

    @Test
    void shouldAllowAbstentionAndPreserveRrfOrderWithinGrade() throws Exception {
        output(List.of(item(FIRST, 1, "全年导出时超时"), item(SECOND, 0, "")));
        assertThat(post(200).path("rerank").path("status").asString()).isEqualTo("INSUFFICIENT_EVIDENCE");
        output(List.of(item(SECOND, 2, "部分列为空"), item(FIRST, 2, "全年导出时超时")));
        JsonNode matches = post(200).path("rerank").path("matches");
        assertThat(matches.get(0).path("document").path("id").asString()).isEqualTo(FIRST);
        assertThat(matches.get(1).path("document").path("id").asString()).isEqualTo(SECOND);
    }

    @Test
    void shouldSkipEmptyAndDegradedRetrievalWithoutModelCalls() throws Exception {
        source.setStatus("DEGRADED");
        assertThat(post(200).path("rerank").path("status").asString()).isEqualTo("SKIPPED_DEGRADED");
        source.setMatches(List.of());
        assertThat(post(200).path("rerank").path("status").asString()).isEqualTo("RETRIEVAL_UNAVAILABLE");
        source.setStatus("NO_MATCH");
        assertThat(post(200).path("rerank").path("status").asString()).isEqualTo("NO_CANDIDATES");
        assertThat(CALLS.get()).isZero();
    }

    @Test
    void shouldExposeFailuresWithoutRetryOrPretendingAbstention() throws Exception {
        STATUS.set(503);
        assertThat(post(200).path("rerank").path("status").asString()).isEqualTo("MODEL_UNAVAILABLE");
        assertThat(CALLS.get()).isEqualTo(1);
        STATUS.set(200);
        DELAY.set(500);
        assertThat(post(200).path("rerank").path("status").asString()).isEqualTo("MODEL_TIMEOUT");
        assertThat(CALLS.get()).isEqualTo(2);
        limiter.acquirePermit();
        try {
            assertThat(post(200).path("rerank").path("status").asString()).isEqualTo("BUSY");
        } finally {
            limiter.releasePermit();
        }
        assertThat(CALLS.get()).isEqualTo(2);
    }

    @Test
    void shouldValidateHttpInputBeforeRetrieval() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/knowledge/rerank"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{\"description\":\" \"}"))
                .build();
        assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(400);
        assertThat(CALLS.get()).isZero();
    }

    private JsonNode post(int status) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/knowledge/rerank"))
                .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(Map.of("description", QUESTION)))).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(status);
        return JSON.readTree(response.body());
    }

    private static CaseMatchDTO match(String id, String symptoms) {
        KnowledgeCaseDTO document = new KnowledgeCaseDTO();
        document.setId(id);
        document.setSymptoms(symptoms);
        document.setApplicability("教学环境");
        CaseMatchDTO result = new CaseMatchDTO();
        result.setDocument(document);
        result.setScore(0.03);
        return result;
    }

    private static Map<String, Object> item(String id, int grade, String quote) {
        return Map.of("caseId", id, "grade", grade, "reason", "判断关键症状是否匹配", "evidence", quote);
    }

    private void output(List<Map<String, Object>> items) {
        CONTENT.set(JSON.writeValueAsString(Map.of("assessments", items)));
    }

    private static HttpServer upstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(UPSTREAM_EXECUTOR);
            server.createContext("/v1/chat/completions", exchange -> {
                try {
                    CALLS.incrementAndGet();
                    LAST.set(JSON.readTree(exchange.getRequestBody().readAllBytes()));
                    String content = CONTENT.get();
                    String finish = FINISH.get();
                    int status = STATUS.get();
                    Thread.sleep(DELAY.get());
                    Object payload = status == 200 ? Map.of("id", "rerank-test", "object", "chat.completion", "created", 1,
                            "model", "local-test", "choices", List.of(Map.of("index", 0, "finish_reason", finish,
                            "message", Map.of("role", "assistant", "content", content))))
                            : Map.of("error", Map.of("message", "private-upstream-message", "type", "server_error", "code", "server_error"));
                    byte[] bytes = JSON.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.getResponseHeaders().set("Retry-After", "0");
                    exchange.sendResponseHeaders(status, bytes.length);
                    exchange.getResponseBody().write(bytes);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } finally {
                    exchange.close();
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
