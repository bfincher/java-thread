package com.fincher.thread;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A custom thread pool that extends ScheduledThreadPoolExecutor and provides
 * additional scheduling methods with Duration parameters.
 */
public class ThreadPool extends ScheduledThreadPoolExecutor {

    /**
     * A factory for creating new threads with a custom naming pattern.
     */
    private static class ThreadPoolThreadFactory implements ThreadFactory {
        protected static AtomicInteger nextId = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "ThreadPool_" + nextId.getAndIncrement());
        }
    }

    /**
     * Creates a ThreadPool with the specified core pool size.
     *
     * @param corePoolSize the number of threads to keep in the pool, even if they
     *        are idle
     */
    public ThreadPool(int corePoolSize) {
        super(corePoolSize, new ThreadPoolThreadFactory());
    }

    /**
     * Creates a ThreadPool with the specified core pool size and a handler for
     * tasks that cannot be executed.
     *
     * @param corePoolSize the number of threads to keep in the pool, even if they
     *        are idle
     * @param handler the handler to use when execution is blocked because the
     *        thread bounds and queue capacities are reached
     */
    public ThreadPool(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, new ThreadPoolThreadFactory(), handler);
    }

    /**
     * Throws UnsupportedOperationException because the thread factory cannot be
     * changed.
     *
     * @param threadFactory the thread factory (ignored)
     * @throws UnsupportedOperationException always thrown by this method
     */
    @Override
    public void setThreadFactory(ThreadFactory threadFactory) {
        throw new UnsupportedOperationException("The thread factory cannot be changed");
    }

    /**
     * Schedules a command to be executed after the given delay.
     *
     * @param command the task to execute
     * @param delay the time from now to delay execution
     * @return a ScheduledFuture representing pending completion of the task
     */
    @SuppressWarnings("squid:S1452")
    public ScheduledFuture<?> schedule(Runnable command, Duration delay) {
        return schedule(command, delay.toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * Schedules a callable task to be executed after the given delay.
     *
     * @param <V> the type of the callable's result
     * @param callable the task to execute
     * @param delay the time from now to delay execution
     * @return a ScheduledFuture that can be used to extract result or cancel
     */
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, Duration delay) {
        return schedule(callable, delay.toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * Schedules a command to be executed periodically, starting after the given
     * initial delay, with subsequent executions having the given period.
     *
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @return a ScheduledFuture representing pending completion of the task
     */
    @SuppressWarnings("squid:S1452")
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, Duration initialDelay, Duration period) {
        return scheduleAtFixedRate(command, initialDelay.toNanos(), period.toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * Schedules a command to be executed with a fixed delay between the end of the
     * last execution and the start of the next.
     *
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param delay the delay between the termination of one execution and
     *        the commencement of the next
     * @return a ScheduledFuture representing pending completion of the task
     */
    @SuppressWarnings("squid:S1452")
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, Duration initialDelay, Duration delay) {
        return scheduleWithFixedDelay(command, initialDelay.toNanos(), delay.toNanos(), TimeUnit.NANOSECONDS);
    }
}
