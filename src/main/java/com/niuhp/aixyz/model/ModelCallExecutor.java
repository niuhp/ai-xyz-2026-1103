package com.niuhp.aixyz.model;

import com.niuhp.aixyz.exception.ProcessingError;
import com.niuhp.aixyz.exception.TicketProcessingException;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIServiceException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * 统一管理一次逻辑请求的尝试次数；SDK 本身必须关闭自动重试。
 *
 * @author niuhaipeng
 */
@Component
public class ModelCallExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelCallExecutor.class);
    private static final long MAX_WAIT_MILLIS = 2000;

    private final ModelFailureClassifier classifier;
    private final int maxRetries;

    public ModelCallExecutor(ModelFailureClassifier classifier,
            @Value("${app.ticket.model-max-retries:1}") int maxRetries) {
        Assert.isTrue(maxRetries >= 0 && maxRetries <= 1, "模型额外重试次数只能是 0 或 1");
        this.classifier = classifier;
        this.maxRetries = maxRetries;
    }

    public ChatResponse call(Supplier<ChatResponse> invocation) {
        for (int attempt = 1; ; attempt++) {
            long started = System.nanoTime();
            try {
                ChatResponse response = invocation.get();
                logAttempt(attempt, "SUCCESS", started);
                return response;
            } catch (OpenAIException exception) {
                ProcessingError error = classifier.classify(exception);
                logAttempt(attempt, error.name(), started);
                long delay = retryDelay(exception, error);
                if (attempt > maxRetries || delay < 0) {
                    throw new TicketProcessingException(error);
                }
                LOGGER.info("model_retry requestId={} nextAttempt={} delayMillis={}",
                        MDC.get("requestId"), attempt + 1, delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new TicketProcessingException(ProcessingError.MODEL_UNAVAILABLE);
                }
            } catch (NumberFormatException exception) {
                // SDK 读取异常数值响应头时可能直接抛出解析错误，不自动重复调用。
                logAttempt(attempt, ProcessingError.MODEL_UNAVAILABLE.name(), started);
                throw new TicketProcessingException(ProcessingError.MODEL_UNAVAILABLE);
            }
        }
    }

    private long retryDelay(OpenAIException exception, ProcessingError error) {
        if (error != ProcessingError.MODEL_UNAVAILABLE || !(exception instanceof OpenAIServiceException)) {
            return -1;
        }
        OpenAIServiceException serviceException = (OpenAIServiceException)exception;
        int status = serviceException.statusCode();
        if (status != HttpStatus.BAD_GATEWAY.value() && status != HttpStatus.SERVICE_UNAVAILABLE.value()) {
            return -1;
        }
        long jitter = ThreadLocalRandom.current().nextLong(250, 501);
        List<String> retryAfter = serviceException.headers().values("retry-after");
        if (retryAfter.isEmpty()) {
            return jitter;
        }
        String value = retryAfter.get(0).trim();
        long delay;
        try {
            // 先按秒数解析，HTTP 日期是另一种合法格式。
            delay = Math.multiplyExact(Long.parseLong(value), 1000L);
        } catch (NumberFormatException exception1) {
            try {
                delay = Duration.between(ZonedDateTime.now(),
                        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)).toMillis();
            } catch (DateTimeParseException exception2) {
                return -1;
            }
        } catch (ArithmeticException exception3) {
            return -1;
        }
        if (delay < 0 || delay > MAX_WAIT_MILLIS) {
            return -1;
        }
        return Math.max(jitter, delay);
    }

    private void logAttempt(int attempt, String outcome, long started) {
        LOGGER.info("model_attempt requestId={} attempt={} outcome={} elapsedMillis={}",
                MDC.get("requestId"), attempt, outcome,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
    }
}
