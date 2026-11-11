package com.niuhp.aixyz.exception;

import com.niuhp.aixyz.dto.response.ApiErrorDTO;
import com.niuhp.aixyz.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 错误响应与日志脱敏：不输出密钥、用户文本、上游响应原文。
 *
 * @author niuhaipeng
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TicketProcessingException.class)
    public ResponseEntity<ApiErrorDTO> handleProcessing(TicketProcessingException exception,
            HttpServletRequest request) {
        return error(exception.getError(), request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiErrorDTO> handleInvalidInput(HttpServletRequest request) {
        return error(ProcessingError.INVALID_REQUEST, request);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleDisconnectedClient(HttpServletRequest request) {
        // 响应已不可写，不能再向已经关闭的 SSE 连接发送 JSON 错误。
        LOGGER.info("stream_client_disconnected requestId={}",
                request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleUnexpected(Exception exception, HttpServletRequest request) {
        // 不记录异常消息或堆栈，供应商异常可能在其中包含敏感响应。
        LOGGER.error("unexpected_failure requestId={} exceptionType={}",
                request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE), exception.getClass().getSimpleName());
        return error(ProcessingError.INTERNAL_ERROR, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorDTO> handleNotFound(HttpServletRequest request) {
        return error(ProcessingError.NOT_FOUND, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorDTO> handleMethodNotAllowed(HttpServletRequest request) {
        return error(ProcessingError.METHOD_NOT_ALLOWED, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorDTO> handleUnsupportedMediaType(HttpServletRequest request) {
        return error(ProcessingError.UNSUPPORTED_MEDIA_TYPE, request);
    }

    private ResponseEntity<ApiErrorDTO> error(ProcessingError error, HttpServletRequest request) {
        ApiErrorDTO body = new ApiErrorDTO();
        body.setRequestId((String)request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
        body.setCode(error.name());
        body.setMessage(error.getMessage());
        body.setAction(error.getAction());
        LOGGER.warn("ticket_draft_failed requestId={} code={}", body.getRequestId(), body.getCode());
        return ResponseEntity.status(error.getHttpStatus()).body(body);
    }
}
