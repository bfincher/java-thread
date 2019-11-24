package com.fincher.thread;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool extends ScheduledThreadPoolExecutor {

    private static final ChronoUnit NANOS = ChronoUnit.NANOS;

    private static class ThreadPoolThreadFactory implements ThreadFactory {
        protected static AtomicInteger nextId = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "ThreadPool_" + nextId.getAndIncrement());
        }
    }

    public ThreadPool(int corePoolSize) {
        super(corePoolSize, new ThreadPoolThreadFactory());
    }

    public ThreadPool(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, new ThreadPoolThreadFactory(), handler);
    }

    @Override
    public void setThreadFactory(ThreadFactory threadFactory) {
        throw new UnsupportedOperationException("The thread factory cannot be changed");
    }

    public ScheduledFuture<?> schedule(Runnable command, Duration delay) {
        return schedule(command, delay.get(NANOS), TimeUnit.NANOSECONDS);
    }

    public <V> ScheduledFuture<V> schedule(Callable<V> callable, Duration delay) {
        return schedule(callable, delay.get(NANOS), TimeUnit.NANOSECONDS);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, Duration initialDelay,
            Duration period) {
        return scheduleAtFixedRate(command, initialDelay.get(NANOS), period.get(NANOS),
                TimeUnit.NANOSECONDS);
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, Duration initialDelay,
            Duration delay) {
        return scheduleWithFixedDelay(command, initialDelay.get(NANOS), delay.get(NANOS),
                TimeUnit.NANOSECONDS);
    }

}
