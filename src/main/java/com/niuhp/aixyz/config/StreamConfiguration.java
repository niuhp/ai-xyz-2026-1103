package com.niuhp.aixyz.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 有生命周期的流任务和心跳调度器，并发总量仍由业务信号量限制。
 *
 * @author niuhaipeng
 */
@Configuration
public class StreamConfiguration {

    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService streamWorkers(@Value("${app.ticket.max-concurrent-requests:2}") int limit) {
        // 先由共享信号量准入；有界队列兜住任务退出与下一次提交之间的短暂交接。
        return new ThreadPoolExecutor(limit, limit, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(limit), Thread.ofPlatform().name("sse-worker-", 0).factory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean(destroyMethod = "shutdownNow")
    public ScheduledThreadPoolExecutor streamTimer() {
        ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(2,
                Thread.ofPlatform().name("sse-timer-", 0).daemon(true).factory());
        timer.setRemoveOnCancelPolicy(true);
        return timer;
    }
}
