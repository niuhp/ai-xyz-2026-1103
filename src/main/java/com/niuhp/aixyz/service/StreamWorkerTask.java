package com.niuhp.aixyz.service;

import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 保证取消、执行器拒绝和正常退出均恰好释放一次额度；运行中的任务退出后才释放。
 *
 * @author niuhaipeng
 */
final class StreamWorkerTask extends FutureTask<Void> {

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean released = new AtomicBoolean();
    private final Runnable release;

    StreamWorkerTask(Runnable work, Runnable release) {
        super(work, null);
        this.release = release;
    }

    @Override
    public void run() {
        started.set(true);
        try {
            super.run();
        } finally {
            releaseOnce();
        }
    }

    @Override
    protected void done() {
        if (!started.get()) {
            releaseOnce();
        }
    }

    private void releaseOnce() {
        if (released.compareAndSet(false, true)) {
            release.run();
        }
    }
}
