package com.niuhp.aixyz.model;

import com.niuhp.aixyz.exception.ProcessingError;
import com.niuhp.aixyz.exception.TicketProcessingException;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * 普通生成、SSE、知识库建议与向量调用共享的单实例模型并发额度。
 * 取得额度的调用方负责在处理真正结束后释放；满额立即拒绝，不排队。
 *
 * @author niuhaipeng
 */
@Component
public class ModelConcurrencyLimiter {

    private final Semaphore permits;

    public ModelConcurrencyLimiter(@Value("${app.ticket.max-concurrent-requests:2}") int maxConcurrentRequests) {
        Assert.isTrue(maxConcurrentRequests > 0, "最大并发数必须大于零");
        this.permits = new Semaphore(maxConcurrentRequests);
    }

    public void acquirePermit() {
        if (!permits.tryAcquire()) {
            throw new TicketProcessingException(ProcessingError.BUSY);
        }
    }

    public void releasePermit() {
        permits.release();
    }
}
