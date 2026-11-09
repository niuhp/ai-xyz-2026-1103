package com.niuhp.aixyz;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 本机 HTTP 验证实际 SDK 的增量、完成标记、零重试及端到端时限。
 *
 * @author niuhaipeng
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.config.import=", "spring.ai.openai.api-key=test-key",
                "spring.ai.openai.timeout=1s", "app.ticket.stream-timeout=3s",
                "app.ticket.max-concurrent-requests=1",
                "spring.datasource.url=jdbc:h2:mem:streamtest;DB_CLOSE_DELAY=-1"})
class TicketDraftStreamIntegrationTest {

    private static final String SCENARIO_AUTH = "auth";
    private static final String SCENARIO_RATE = "rate";
    private static final String SCENARIO_UNAVAILABLE = "unavailable";
    private static final String SCENARIO_HOLD = "hold";
    private static final String SCENARIO_SUCCESS = "success";
    private static final String SCENARIO_MISSING = "missing";
    private static final ThreadPoolExecutor UPSTREAM_WORKERS = new ThreadPoolExecutor(0, 8, 30L,
            TimeUnit.SECONDS, new SynchronousQueue<>(),
            Thread.ofPlatform().name("stream-test-", 0).daemon(true).factory(), new ThreadPoolExecutor.AbortPolicy());
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String CONTENT = """
            {"relevant":true,"draft":{"system":"RuoYi-Vue","module":"字典管理",
            "category":"DATA_DUPLICATION","summary":"重复字典值","actualResult":"重复添加成功",
            "expectedResult":"提示已存在","occurredAtText":"","version":"3.8.9"}}
            """;
    private static final AtomicInteger CALLS = new AtomicInteger();
    private static final AtomicReference<JsonNode> LAST_REQUEST = new AtomicReference<>();
    private static final HttpServer UPSTREAM = upstream();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url", () -> "http://127.0.0.1:"
                + UPSTREAM.getAddress().getPort() + "/v1");
    }

    @AfterAll
    static void stop() {
        UPSTREAM.stop(0);
        UPSTREAM_WORKERS.shutdownNow();
    }

    @BeforeEach
    void reset() {
        CALLS.set(0);
        jdbc.update("DELETE FROM support_ticket");
        jdbc.update("DELETE FROM ticket_draft");
    }

    @Test
    void shouldEmitBeforeCompletionAndSaveOnlyValidatedDraft() throws Exception {
        long start = System.nanoTime();
        HttpResponse<InputStream> response = client.send(request("success"), HttpResponse.BodyHandlers.ofInputStream());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("text/event-stream");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            assertThat(reader.readLine()).isEqualTo("event:status");
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ticket_draft", Integer.class)).isZero();
            String rest = reader.lines().reduce("", (left, right) -> left + right + "\n");
            assertThat(rest).contains("event:delta", "event:result", "\"status\":\"READY\"", "\"totalTokens\":150")
                    .doesNotContain("event:failure");
            assertThat(rest.split("event:delta", -1).length).isGreaterThan(2);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ticket_draft", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM support_ticket", Integer.class)).isZero();
        assertThat(LAST_REQUEST.get().path("stream").asBoolean()).isTrue();
        assertThat(LAST_REQUEST.get().path("response_format").path("type").asString()).isEqualTo("json_object");
        assertThat(LAST_REQUEST.get().path("model").asString()).isEqualTo("deepseek-v4.1-flash");
        assertThat(CALLS.get()).isEqualTo(1);
    }

    @Test
    void shouldRejectIncompleteMalformedAndTruncatedOutputsWithoutSaving() throws Exception {
        for (String scenario : new String[]{"disconnect", "missing", "invalid", "length", "badfield", "oversize"}) {
            HttpResponse<String> response = client.send(request(scenario), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("event:failure").doesNotContain("event:result", "test-key");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ticket_draft", Integer.class)).isZero();
        }
    }

    @Test
    void shouldClassifyProviderErrorsWithOneAttemptEvenWithRetryAfterZero() throws Exception {
        for (String scenario : new String[]{"rate", "unavailable", "auth"}) {
            CALLS.set(0);
            HttpResponse<String> response = client.send(request(scenario), HttpResponse.BodyHandlers.ofString());
            String expected = SCENARIO_RATE.equals(scenario) ? "MODEL_RATE_LIMITED"
                    : SCENARIO_AUTH.equals(scenario) ? "MODEL_AUTH_ERROR" : "MODEL_UNAVAILABLE";
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("event:failure", expected)
                    .doesNotContain("sensitive-provider-detail", "event:result");
            assertThat(CALLS.get()).isEqualTo(1);
        }
    }

    @Test
    void shouldApplyTotalDeadlineAndReleasePermit() throws Exception {
        long start = System.nanoTime();
        String body = client.send(request("hold"), HttpResponse.BodyHandlers.ofString()).body();
        assertThat(body).contains("event:failure", "MODEL_TIMEOUT").doesNotContain("event:result");
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ticket_draft", Integer.class)).isZero();
        assertThat(client.send(request("quick"), HttpResponse.BodyHandlers.ofString()).body()).contains("event:result");
    }

    @Test
    void shouldRejectInvalidInputAndShareBusyLimitWithNormalEndpoint() throws Exception {
        HttpResponse<String> invalid = client.send(request(" "), HttpResponse.BodyHandlers.ofString());
        assertThat(invalid.statusCode()).isEqualTo(400);
        assertThat(invalid.body()).contains("INVALID_REQUEST");
        HttpResponse<InputStream> held = client.send(request("hold"), HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream stream = held.body()) {
            assertThat(stream.read()).isNotEqualTo(-1);
            HttpResponse<String> busy = client.send(request("quick"), HttpResponse.BodyHandlers.ofString());
            assertThat(busy.statusCode()).isEqualTo(503);
            assertThat(busy.body()).contains("BUSY");
            HttpRequest normal = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/ticket-drafts"))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(
                            JSON.writeValueAsString(Map.of("description", "normal")))).build();
            assertThat(client.send(normal, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(503);
            stream.readAllBytes();
        }
    }

    @Test
    void shouldNoticeDisconnectedClientAndReleaseCapacity() throws Exception {
        HttpResponse<InputStream> held = client.send(request("hold"), HttpResponse.BodyHandlers.ofInputStream());
        held.body().close();
        // heartbeat observes disconnect within two seconds; deadline is an additional bound.
        long until = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        HttpResponse<String> next;
        do {
            Thread.sleep(100);
            next = client.send(request("quick"), HttpResponse.BodyHandlers.ofString());
        } while (next.statusCode() == 503 && System.nanoTime() < until);
        assertThat(next.statusCode()).isEqualTo(200);
        assertThat(next.body()).contains("event:result");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ticket_draft", Integer.class)).isEqualTo(1);
    }

    private HttpRequest request(String scenario) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/ticket-drafts/stream"))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .header("Accept", "text/event-stream, application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(Map.of("description", scenario))))
                .build();
    }

    private static HttpServer upstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(UPSTREAM_WORKERS);
            server.createContext("/v1/chat/completions", TicketDraftStreamIntegrationTest::respond);
            server.start();
            return server;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void respond(HttpExchange exchange) throws IOException {
        try {
            CALLS.incrementAndGet();
            JsonNode request = JSON.readTree(exchange.getRequestBody().readAllBytes());
            LAST_REQUEST.set(request);
            String scenario = request.path("messages").get(1).path("content").asString();
            if (SCENARIO_RATE.equals(scenario) || SCENARIO_UNAVAILABLE.equals(scenario) || SCENARIO_AUTH.equals(scenario)) {
                int status = SCENARIO_RATE.equals(scenario) ? 429 : SCENARIO_AUTH.equals(scenario) ? 401 : 503;
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Retry-After", "0");
                exchange.sendResponseHeaders(status, 0);
                exchange.getResponseBody().write(JSON.writeValueAsBytes(Map.of("error", Map.of(
                        "message", "sensitive-provider-detail", "code", "provider_error", "type", "provider_error"))));
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            if (SCENARIO_HOLD.equals(scenario)) {
                Thread.sleep(6000);
            } else if (SCENARIO_SUCCESS.equals(scenario)) {
                Thread.sleep(1200);
            }
            String text = "invalid".equals(scenario) ? "不是JSON" : "oversize".equals(scenario)
                    ? "字".repeat(32001) : "badfield".equals(scenario)
                    ? CONTENT.replace("重复字典值", "") : CONTENT;
            int step = "oversize".equals(scenario) ? 16000 : 30;
            for (int offset = 0; offset < text.length(); offset += step) {
                event(exchange, Map.of("content", text.substring(offset, Math.min(offset + step, text.length()))), null);
                if ("disconnect".equals(scenario)) {
                    return;
                }
            }
            if (!SCENARIO_MISSING.equals(scenario)) {
                event(exchange, Map.of(), "length".equals(scenario) ? "length" : "stop");
                exchange.getResponseBody().write(("data: " + JSON.writeValueAsString(Map.of(
                        "id", "chunk", "object", "chat.completion.chunk", "created", 1, "model", "test-stream",
                        "choices", new Object[]{}, "usage", Map.of("prompt_tokens", 100,
                                "completion_tokens", 50, "total_tokens", 150))) + "\n\n")
                        .getBytes(StandardCharsets.UTF_8));
            }
            exchange.getResponseBody().write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
            exchange.getResponseBody().flush();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
            // Client cancellation is expected in the disconnect/deadline tests.
        } finally {
            exchange.close();
        }
    }

    private static void event(HttpExchange exchange, Map<String, String> delta, String reason) throws IOException {
        String finish = reason == null ? "null" : "\"" + reason + "\"";
        String data = "{\"id\":\"chunk\",\"object\":\"chat.completion.chunk\",\"created\":1,"
                + "\"model\":\"test-stream\",\"choices\":[{\"index\":0,\"delta\":"
                + JSON.writeValueAsString(delta) + ",\"finish_reason\":" + finish + "}]}";
        exchange.getResponseBody().write(("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
        exchange.getResponseBody().flush();
    }
}
