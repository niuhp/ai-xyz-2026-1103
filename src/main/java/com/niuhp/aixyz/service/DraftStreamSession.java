package com.niuhp.aixyz.service;

import com.niuhp.aixyz.dto.response.ApiErrorDTO;
import com.niuhp.aixyz.dto.response.TicketDraftResponseDTO;
import com.niuhp.aixyz.exception.ProcessingError;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 串行化当前连接的发送、结束和取消；关闭后的连接不能再保存结果。
 *
 * @author niuhaipeng
 */
final class DraftStreamSession {

    private final String requestId;
    private final SseEmitter emitter;
    private boolean closed;
    private Future<?> worker;
    private Future<?> heartbeat;
    private Future<?> deadline;

    DraftStreamSession(String requestId, long timeoutMillis) {
        this.requestId = requestId;
        this.emitter = new SseEmitter(timeoutMillis);
        emitter.onCompletion(this::close);
        emitter.onError(error -> close());
        emitter.onTimeout(() -> fail(ProcessingError.MODEL_TIMEOUT));
    }

    SseEmitter getEmitter() {
        return emitter;
    }

    synchronized void attachWorker(Future<?> task) {
        worker = task;
        if (closed) {
            worker.cancel(true);
        }
    }

    synchronized void attachHeartbeat(Future<?> task) {
        heartbeat = task;
        if (closed) {
            heartbeat.cancel(false);
        }
    }

    synchronized void attachDeadline(Future<?> task) {
        deadline = task;
        if (closed) {
            deadline.cancel(false);
        }
    }

    synchronized boolean isClosed() {
        return closed;
    }

    synchronized void send(String event, Object data) {
        if (closed) {
            throw new StreamClosedException();
        }
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException exception) {
            close();
            throw new StreamClosedException();
        }
    }

    synchronized void beat() {
        if (!closed) {
            try {
                send("heartbeat", Map.of("requestId", requestId));
            } catch (StreamClosedException ignored) {
                // 客户端断开时 send 已完成清理，不再向同一连接发送异常。
            }
        }
    }

    synchronized void complete(Supplier<TicketDraftResponseDTO> persist) {
        if (closed) {
            return;
        }
        // 保存与取消串行化；若保存后才断网，客户端可能收不到已保存的草稿。
        send("result", persist.get());
        close();
        emitter.complete();
    }

    synchronized void fail(ProcessingError error) {
        if (closed) {
            return;
        }
        ApiErrorDTO body = new ApiErrorDTO();
        body.setRequestId(requestId);
        body.setCode(error.name());
        body.setMessage(error.getMessage());
        body.setAction(error.getAction());
        try {
            send("failure", body);
        } catch (StreamClosedException ignored) {
            // 连接已断开，仅清理本地任务。
        } finally {
            close();
            emitter.complete();
        }
    }

    synchronized void close() {
        closed = true;
        cancelTasks();
    }

    private void cancelTasks() {
        if (worker != null) {
            worker.cancel(true);
        }
        if (heartbeat != null) {
            heartbeat.cancel(false);
        }
        if (deadline != null) {
            deadline.cancel(false);
        }
    }

    /** 已关闭连接的本地控制信号，不携带上游异常。 */
    static class StreamClosedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
