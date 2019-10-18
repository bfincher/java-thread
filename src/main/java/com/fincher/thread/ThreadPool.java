package com.fincher.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Multiple threads that are used to perform various tasks
 * 
 * @author Brian Fincher
 *
 */
public class ThreadPool {

    private static Logger LOG = LoggerFactory.getLogger(ThreadPool.class);

    private static final String IS_SHUTDOWN_MSG = "shutdown";

    private final List<MyThread> threadList;

    private final LinkedBlockingQueue<FutureTaskWithId<?>> runnableQueue = new LinkedBlockingQueue<FutureTaskWithId<?>>();

    private boolean isShutdown = false;

    private final EventList eventList;

    private final String threadPoolName;

    private final int queueSizeWarningThreshold;

    /**
     * Constructs a new ThreadPool.
     * 
     * @param numThreads     The maximum number of threads in the thread pool
     * @param threadPoolName The name of this thread pool
     */
    public ThreadPool(int numThreads, String threadPoolName) {
        threadList = new ArrayList<MyThread>(numThreads);

        this.threadPoolName = threadPoolName;
        queueSizeWarningThreshold = Math.max(
                new Integer(System.getProperty("min.thread.pool.size.warning", "250")),
                numThreads * 2);

        for (int i = 0; i < numThreads; i++) {
            String id = "ThreadPool_" + this.threadPoolName + "_" + i;

            MyThread thread = new MyThread(id, new ThreadPoolRunnable(runnableQueue));

            thread.start();
            threadList.add(thread);
        }

        eventList = new EventList(this);
    }

    /**
     * Set a handler to be notified upon occurrence of exceptions.
     * 
     * @param exceptionHandler The exception handler
     */
    public void setExceptionHandler(ExceptionHandlerIfc exceptionHandler) {
        for (MyThread thread : threadList) {
            thread.setExceptionHandler(exceptionHandler);
        }
    }

    private void submit(FutureTaskWithId<?> future) throws InterruptedException {
        if (runnableQueue.size() > queueSizeWarningThreshold) {
            LOG.warn("{} Warning.  Thread pool queue size = {}", threadPoolName,
                    runnableQueue.size());
        }

        runnableQueue.put(future);
    }

    /**
     * Submit a task to be executed on the thread pool.
     * 
     * @param task The task to be executed
     * @return a Future representing pending completion of the task
     */
    public Future<Boolean> submit(RunnableWithIdIfc task) throws InterruptedException {
        if (isShutdown) {
            throw new IllegalStateException(IS_SHUTDOWN_MSG);
        }

        FutureTaskWithId<Boolean> future = new FutureTaskWithId<Boolean>(task, true);
        submit(future);
        return future;
    }

    /**
     * Submit a task to be executed on the thread pool.
     * 
     * @param task The task to be executed
     * @param      <T> The type of data
     * @return a Future representing pending completion of the task
     */
    public <T> Future<T> submit(CallableWithIdIfc<T> task) throws InterruptedException {
        if (isShutdown) {
            throw new IllegalStateException(IS_SHUTDOWN_MSG);
        }

        FutureTaskWithId<T> future = new FutureTaskWithId<T>(task);
        submit(future);
        return future;
    }

    /**
     * Creates and executes a one-shot action that becomes enabled after the given delay.
     *
     * @param command the task to execute
     * @param delay   the time from now to delay execution
     * @return A Future task used to control the event
     */
    public MyRunnableScheduledFuture<Boolean> schedule(RunnableWithIdIfc command, long delay) {

        if (isShutdown) {
            throw new IllegalStateException(IS_SHUTDOWN_MSG);
        }

        return eventList.schedule(command, delay);
    }

    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial
     * delay, and subsequently with the given delay between the termination of one execution and the
     * commencement of the next.
     * 
     * @param command      The task to execute
     * @param initialDelay The time to delay first execution
     * @param period       The period between executions
     * @return A Future task used to control the event
     */
    public MyRunnableScheduledFuture<Boolean> scheduleWithFixedDelay(RunnableWithIdIfc command,
            long initialDelay, long period) {

        if (isShutdown) {
            throw new IllegalStateException(IS_SHUTDOWN_MSG);
        }

        return eventList.scheduleWithFixedDelay(command, initialDelay, period);
    }

    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial
     * delay, and subsequently with the given delay between the start of one execution and the
     * commencement of the next (however, subsequent executions will not begin until previous
     * executions have completed).
     * 
     * @param command      The task to execute
     * @param initialDelay The time to delay first execution
     * @param period       The period between executions
     * @return A Future task used to control the event
     */
    public MyRunnableScheduledFuture<Boolean> scheduleAtFixedRate(RunnableWithIdIfc command,
            long initialDelay, long period) {

        if (isShutdown) {
            throw new IllegalStateException(IS_SHUTDOWN_MSG);
        }

        return eventList.scheduleAtFixedRate(command, initialDelay, period);
    }

    /** Shutdown this ThreadPool. */
    public final void shutdown() {
        isShutdown = true;

        eventList.shutdown();

        for (MyThread thread : threadList) {
            thread.terminate();
        }

        runnableQueue.clear();
        threadList.clear();
    }

    public LinkedBlockingQueue<FutureTaskWithId<?>> getQueue() {
        return runnableQueue;
    }
}
