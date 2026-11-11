package com.niuhp.aixyz.service;

import com.niuhp.aixyz.exception.ProcessingError;
import com.niuhp.aixyz.exception.TicketProcessingException;
import com.niuhp.aixyz.model.ExtractionResult;
import com.niuhp.aixyz.model.ModelConcurrencyLimiter;
import com.niuhp.aixyz.model.ModelFailureClassifier;
import com.niuhp.aixyz.model.StreamingExtraction;
import com.niuhp.aixyz.model.TicketStreamingModelGateway;
import com.openai.errors.OpenAIException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * MVC SSE 接口，先预览，完整验证后才保存；不自动重试流式生成。
 *
 * @author niuhaipeng
 */
@Service
public class TicketDraftStreamService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketDraftStreamService.class);
    private static final int HEARTBEAT_SECONDS = 2;
    private static final int MAX_CAUSE_DEPTH = 32;
    private final TicketDraftService drafts;
    private final ModelConcurrencyLimiter limiter;
    private final TicketStreamingModelGateway gateway;
    private final ModelFailureClassifier classifier;
    private final ExecutorService streamWorkers;
    private final ScheduledThreadPoolExecutor streamTimer;
    private final Duration timeout;

    public TicketDraftStreamService(TicketDraftService drafts, TicketStreamingModelGateway gateway,
            ModelFailureClassifier classifier, ModelConcurrencyLimiter limiter, ExecutorService streamWorkers,
            ScheduledThreadPoolExecutor streamTimer, @Value("${app.ticket.stream-timeout:45s}") Duration timeout) {
        Assert.isTrue(!timeout.isNegative() && !timeout.isZero(), "流式时限必须为正数");
        this.drafts = drafts;
        this.limiter = limiter;
        this.gateway = gateway;
        this.classifier = classifier;
        this.streamWorkers = streamWorkers;
        this.streamTimer = streamTimer;
        this.timeout = timeout;
    }

    public SseEmitter generate(String description, String requestId) {
        // 进入异步发送前取得额度，BUSY 仍以普通 HTTP 错误响应返回。
        limiter.acquirePermit();
        DraftStreamSession session = new DraftStreamSession(requestId, timeout.toMillis());
        StreamWorkerTask worker = new StreamWorkerTask(() -> run(description, requestId, session),
                limiter::releasePermit);
        session.attachWorker(worker);
        try {
            session.send("status", Map.of("requestId", requestId, "stage", "accepted",
                    "message", "请求已接收，正在连接模型"));
            session.attachHeartbeat(streamTimer.scheduleAtFixedRate(session::beat,
                    HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS));
            session.attachDeadline(streamTimer.schedule(() -> session.fail(ProcessingError.MODEL_TIMEOUT),
                    timeout.toMillis(), TimeUnit.MILLISECONDS));
            streamWorkers.execute(worker);
            return session.getEmitter();
        } catch (RuntimeException exception) {
            session.close();
            throw exception;
        }
    }

    private void run(String description, String requestId, DraftStreamSession session) {
        long startedAt = System.nanoTime();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", requestId)) {
            if (session.isClosed()) {
                return;
            }
            StreamingExtraction extraction = new StreamingExtraction();
            session.send("status", Map.of("requestId", requestId, "stage", "generating",
                    "message", "正在等待模型片段"));
            gateway.stream(description).doOnNext(chunk -> {
                String delta = extraction.append(chunk);
                if (!delta.isEmpty()) {
                    session.send("delta", Map.of("text", delta));
                }
            }).blockLast();
            session.send("status", Map.of("requestId", requestId, "stage", "validating",
                    "message", "模型输出结束，正在解析和校验"));
            ExtractionResult result = extraction.finish(gateway);
            session.complete(() -> drafts.complete(result, requestId, startedAt));
        } catch (RuntimeException exception) {
            if (!session.isClosed()) {
                ProcessingError error = classify(exception);
                LOGGER.warn("model_stream_failed requestId={} code={}", requestId, error.name());
                session.fail(error);
            }
        } finally {
            session.close();
            LOGGER.info("model_stream_closed requestId={} elapsedMillis={}", requestId,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
        }
    }

    private ProcessingError classify(Throwable exception) {
        Throwable current = exception;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (current instanceof TicketProcessingException) {
                return ((TicketProcessingException)current).getError();
            }
            if (current instanceof OpenAIException) {
                return classifier.classify((OpenAIException)current);
            }
            current = current.getCause();
        }
        return ProcessingError.STREAM_INCOMPLETE;
    }
}
