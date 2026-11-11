package com.niuhp.aixyz.model;

import com.niuhp.aixyz.exception.ProcessingError;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIServiceException;
import java.io.InterruptedIOException;
import java.net.http.HttpTimeoutException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 只读取上游状态和结构化错误码，不把供应商原文传给用户。
 *
 * @author niuhaipeng
 */
@Component
public class ModelFailureClassifier {

    private static final int MAX_CAUSE_DEPTH = 32;

    private static final Set<String> QUOTA_CODES = Set.of(
            "insufficient_quota", "billing_hard_limit_reached", "quota_exceeded");

    public ProcessingError classify(OpenAIException exception) {
        Throwable current = exception;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (current instanceof InterruptedIOException || current instanceof HttpTimeoutException) {
                return ProcessingError.MODEL_TIMEOUT;
            }
            current = current.getCause();
        }
        if (!(exception instanceof OpenAIServiceException)) {
            return ProcessingError.MODEL_NETWORK_ERROR;
        }
        OpenAIServiceException serviceException = (OpenAIServiceException)exception;
        int status = serviceException.statusCode();
        if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
            return ProcessingError.MODEL_AUTH_ERROR;
        }
        if (status == HttpStatus.PAYMENT_REQUIRED.value() || QUOTA_CODES.contains(serviceException.code().orElse(""))
                || QUOTA_CODES.contains(serviceException.type().orElse(""))) {
            return ProcessingError.MODEL_QUOTA_EXHAUSTED;
        }
        if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return ProcessingError.MODEL_RATE_LIMITED;
        }
        if (status == HttpStatus.BAD_REQUEST.value() || status == HttpStatus.NOT_FOUND.value()
                || status == HttpStatus.UNPROCESSABLE_CONTENT.value()) {
            return ProcessingError.MODEL_REQUEST_REJECTED;
        }
        return ProcessingError.MODEL_UNAVAILABLE;
    }
}
