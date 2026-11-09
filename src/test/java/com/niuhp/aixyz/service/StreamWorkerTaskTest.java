package com.niuhp.aixyz.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证提交前取消和运行中取消不会泄漏、重复或提前释放并发额度。
 *
 * @author niuhaipeng
 */
class StreamWorkerTaskTest {

    @Test
    void shouldReleaseOnceWhenCancelledBeforeExecution() {
        AtomicInteger releases = new AtomicInteger();
        AtomicInteger calls = new AtomicInteger();
        StreamWorkerTask task = new StreamWorkerTask(calls::incrementAndGet, releases::incrementAndGet);
        task.cancel(true);
        task.run();
        task.cancel(true);
        assertThat(calls.get()).isZero();
        assertThat(releases.get()).isEqualTo(1);
    }

    @Test
    void shouldWaitForRunningWorkerToExitBeforeReleasing() throws Exception {
        AtomicInteger releases = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch exit = new CountDownLatch(1);
        StreamWorkerTask task = new StreamWorkerTask(() -> {
            entered.countDown();
            while (exit.getCount() > 0) {
                try {
                    exit.await();
                } catch (InterruptedException ignored) {
                    // 故意延迟退出，用于验证取消 future 不等于任务已经退出。
                }
            }
        }, releases::incrementAndGet);
        Thread worker = Thread.ofVirtual().start(task);
        try {
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            task.cancel(true);
            assertThat(releases.get()).isZero();
        } finally {
            exit.countDown();
            worker.join(5000);
        }
        assertThat(worker.isAlive()).isFalse();
        assertThat(releases.get()).isEqualTo(1);
    }
}
