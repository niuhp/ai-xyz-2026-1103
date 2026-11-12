package com.niuhp.aixyz.knowledge;

import com.niuhp.aixyz.config.EmbeddingProperties;
import com.niuhp.aixyz.config.AiConfiguration;
import com.niuhp.aixyz.dto.request.KnowledgeAdviceRequestDTO;
import com.niuhp.aixyz.exception.ProcessingError;
import com.niuhp.aixyz.exception.TicketProcessingException;
import com.niuhp.aixyz.model.CaseEmbeddingGateway;
import com.niuhp.aixyz.model.KnowledgeAdviceGateway;
import com.niuhp.aixyz.model.ModelConcurrencyLimiter;
import com.niuhp.aixyz.service.KnowledgeAdviceService;
import com.niuhp.aixyz.service.RetrievalCompareService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;

/**
 * 使用本地真实 HTTP 验证 SDK 协议与索引边界；人工向量不证明语义效果。
 *
 * @author niuhaipeng
 */
class SemanticCaseIndexTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final AtomicInteger CALLS = new AtomicInteger();
    private static final AtomicInteger STATUS = new AtomicInteger(200);
    private static final AtomicReference<String> FAULT = new AtomicReference<>("");
    private static final AtomicReference<JsonNode> LAST = new AtomicReference<>();
    private static HttpServer upstream;
    private CaseKnowledgeBase corpus;
    private EmbeddingProperties properties;
    private ModelConcurrencyLimiter limiter;

    @BeforeAll
    static void start() throws IOException {
        upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        upstream.createContext("/v1/embeddings", SemanticCaseIndexTest::respond);
        upstream.start();
    }

    @AfterAll
    static void stop() {
        upstream.stop(0);
        FACTORY.close();
    }

    @BeforeEach
    void reset() throws IOException {
        CALLS.set(0);
        STATUS.set(200);
        FAULT.set("");
        corpus = new CaseKnowledgeBase(JSON, FACTORY.getValidator(), 0.16);
        properties = new EmbeddingProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://127.0.0.1:" + upstream.getAddress().getPort() + "/v1");
        properties.setApiKey("test-embedding-key");
        properties.setModel("fixed-vector-protocol-only");
        properties.setTimeout(Duration.ofSeconds(1));
        properties.setQueryPrefix("query: ");
        properties.setDocumentPrefix("passage: ");
        limiter = new ModelConcurrencyLimiter(2);
    }

    @Test
    void shouldBatchOnceRestoreResponseOrderAndReuseIndex() {
        SemanticCaseIndex index = index();
        assertThat(index.information().get("state")).isEqualTo("NOT_BUILT");
        assertThat(CALLS.get()).isZero();
        var first = index.search("换一种表达也使用同一个向量空间");
        assertThat(first).hasSize(1);
        assertThat(first.get(0).getDocument().getId()).isEqualTo("KB-001");
        assertThat(first.get(0).getScore()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.00001));
        assertThat(CALLS.get()).isEqualTo(2);
        assertThat(LAST.get().path("input").get(0).asString()).startsWith("query: ");
        assertThat(LAST.get().path("model").asString()).isEqualTo(properties.getModel());
        assertThat(index.information().get("dimensions")).isEqualTo(17);
        assertThat(index.information().get("state")).isEqualTo("READY");
        index.search("第二次仅编码查询");
        assertThat(CALLS.get()).isEqualTo(3);
    }

    @Test
    void shouldRejectMalformedBatchesAndPublishOnlyAfterEntireBatchValidates() {
        for (String fault : new String[]{"duplicate", "missing", "wrong_dimension", "zero"}) {
            FAULT.set(fault);
            SemanticCaseIndex index = index();
            assertThatThrownBy(() -> index.search("问题"))
                    .as(fault).isInstanceOfSatisfying(TicketProcessingException.class,
                            exception -> assertThat(exception.getError()).isEqualTo(ProcessingError.INVALID_EMBEDDING_OUTPUT));
            assertThat(index.information().get("state")).isEqualTo("NOT_BUILT");
            FAULT.set("");
            assertThat(index.search("重试")).hasSize(1);
        }
    }

    @Test
    void shouldRejectQueryDimensionsWithoutDiscardingValidCorpus() {
        SemanticCaseIndex index = index();
        index.search("建立索引");
        FAULT.set("query_dimension");
        assertThatThrownBy(() -> index.search("维度错误"))
                .isInstanceOfSatisfying(TicketProcessingException.class,
                        exception -> assertThat(exception.getError()).isEqualTo(ProcessingError.INVALID_EMBEDDING_OUTPUT));
        assertThat(index.information().get("state")).isEqualTo("READY");
        FAULT.set("");
        assertThat(index.search("恢复")).hasSize(1);
        assertThat(CALLS.get()).isEqualTo(4);
    }

    @Test
    void shouldAbstainWithoutChatWhenEmbeddingHasNoMatch() {
        FAULT.set("unrelated");
        SemanticCaseIndex index = index();
        KnowledgeAdviceGateway gateway = mock(KnowledgeAdviceGateway.class);
        KnowledgeAdviceService service = new KnowledgeAdviceService(corpus, gateway,
                new AdviceOutputValidator(FACTORY.getValidator()), limiter, index);
        KnowledgeAdviceRequestDTO request = new KnowledgeAdviceRequestDTO();
        request.setDescription("无关问题");
        request.setMode(AdviceMode.RETRIEVAL);
        request.setMethod(RetrievalMethod.SEMANTIC);
        var response = service.generate(request, "request-test");
        assertThat(response.getStatus()).isEqualTo("NO_MATCH");
        assertThat(response.getRetrievalMethod()).isEqualTo(SemanticCaseIndex.METHOD);
        assertThat(response.getSources()).isEmpty();
        verifyNoInteractions(gateway);
        assertThat(CALLS.get()).isEqualTo(2);
    }

    @Test
    void shouldKeepLexicalResultsWhenDisabledOrUnavailableWithoutAutomaticReplay() {
        properties.setEnabled(false);
        var disabled = new RetrievalCompareService(corpus, index()).compare("导出 Excel 选择全年数据提示超时", "test");
        assertThat(disabled.getLexical().getMatches()).isNotEmpty();
        assertThat(disabled.getSemantic().getStatus()).isEqualTo("EMBEDDING_DISABLED");
        assertThat(CALLS.get()).isZero();
        properties.setEnabled(true);
        STATUS.set(503);
        SemanticCaseIndex failed = index();
        var response = new RetrievalCompareService(corpus, failed).compare("导出 Excel 选择全年数据提示超时", "test");
        assertThat(response.getSemantic().getStatus()).isEqualTo("EMBEDDING_UNAVAILABLE");
        assertThat(response.getSemantic().getMessage()).doesNotContain("private-provider-detail", "test-embedding-key");
        assertThat(CALLS.get()).isEqualTo(1);
        STATUS.set(200);
        assertThat(failed.search("恢复")).hasSize(1);
    }

    @Test
    void shouldUseSharedCapacityAndFingerprintConfigurationChanges() {
        SemanticCaseIndex index = index();
        limiter.acquirePermit();
        limiter.acquirePermit();
        try {
            assertThatThrownBy(() -> index.search("问题"))
                    .isInstanceOfSatisfying(TicketProcessingException.class,
                            exception -> assertThat(exception.getError()).isEqualTo(ProcessingError.BUSY));
            assertThat(CALLS.get()).isZero();
        } finally {
            limiter.releasePermit();
            limiter.releasePermit();
        }
        Object original = index.information().get("fingerprint");
        properties.setModel("another-model");
        assertThat(index().information().get("fingerprint")).isNotEqualTo(original);
        assertThat(properties.toString()).doesNotContain("test-embedding-key");
    }

    @Test
    void shouldRejectNonFiniteOrExcessiveVectorsBeforePublishingIndex() {
        for (float[] invalid : List.of(new float[]{Float.NaN}, new float[]{Float.POSITIVE_INFINITY},
                new float[16385], new float[0])) {
            CaseEmbeddingGateway gateway = mock(CaseEmbeddingGateway.class);
            when(gateway.embed(anyList())).thenReturn(Collections.nCopies(corpus.size(), invalid));
            SemanticCaseIndex index = new SemanticCaseIndex(corpus, gateway, properties, JSON);
            assertThatThrownBy(() -> index.search("无效向量"))
                    .isInstanceOfSatisfying(TicketProcessingException.class,
                            exception -> assertThat(exception.getError()).isEqualTo(ProcessingError.INVALID_EMBEDDING_OUTPUT));
            assertThat(index.information().get("state")).isEqualTo("NOT_BUILT");
        }
    }

    private SemanticCaseIndex index() {
        return new SemanticCaseIndex(corpus, new CaseEmbeddingGateway(properties, limiter, new AiConfiguration().disableImplicitRequestReplay()),
                properties, JSON);
    }

    private static void respond(HttpExchange exchange) throws IOException {
        try {
            CALLS.incrementAndGet();
            JsonNode request = JSON.readTree(exchange.getRequestBody().readAllBytes());
            LAST.set(request);
            int size = request.path("input").size();
            String fault = FAULT.get();
            List<Object> data = new ArrayList<>(size);
            for (int position = 0; position < size; position++) {
                int dimensions = "wrong_dimension".equals(fault) && position == 1
                        || "query_dimension".equals(fault) && size == 1 ? 2 : 17;
                List<Double> vector = new ArrayList<>(Collections.nCopies(dimensions, 0.0));
                int selected = size > 1 ? position % dimensions : "unrelated".equals(fault) ? 16 : 0;
                if (!"zero".equals(fault)) {
                    vector.set(selected, 2.0);
                }
                int responseIndex = "duplicate".equals(fault) ? 0 : position;
                if (!"missing".equals(fault) || position > 0) {
                    data.add(Map.of("object", "embedding", "index", responseIndex, "embedding", vector));
                }
            }
            Collections.reverse(data);
            Object payload = STATUS.get() == 200 ? Map.of("object", "list", "model", "fixed-vector-protocol-only",
                    "data", data, "usage", Map.of("prompt_tokens", 0, "total_tokens", 0))
                    : Map.of("error", Map.of("message", "private-provider-detail", "type", "server_error"));
            byte[] bytes = JSON.writeValueAsBytes(payload);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Retry-After", "0");
            exchange.sendResponseHeaders(STATUS.get(), bytes.length);
            exchange.getResponseBody().write(bytes);
        } finally {
            exchange.close();
        }
    }
}
