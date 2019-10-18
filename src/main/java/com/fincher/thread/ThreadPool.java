package com.fincher.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

/**
 * Multiple threads that are used to perform various tasks
 * 
 * @author Brian Fincher
 *
 */
public class ThreadPool {

    private static Logger logger = Logger.getLogger(ThreadPool.class);

    private static ThreadPool DEFAULT_THREAD_POOL = null;

    private final List<MyThread> threadList;

    private final LinkedBlockingQueue<FutureTaskWithId<?>> runnableQueue = 
            new LinkedBlockingQueue<FutureTaskWithId<?>>();

    private boolean isShutdown = false;

    private final EventList eventList;

    private final String threadPoolName;

    private final int queueSizeWarningThreshold;

    /**
     * Gets the default thread pool for a JVM.
     * 
     * @return the default thread pool for a JVM
     */
    public static synchronized ThreadPool getDefaultThreadPool() {
        if (DEFAULT_THREAD_POOL == null) {
            DEFAULT_THREAD_POOL = new ThreadPool(
                    new Integer(System.getProperty("thread.pool.default.num.threads", "10")),
                    "Default");
        }
        return DEFAULT_THREAD_POOL;
    }


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
        boolean finished = false;
        do {
            if (runnableQueue.size() > queueSizeWarningThreshold) {
                logger.warn(threadPoolName + " Warning.  Thread pool queue size = "
                        + runnableQueue.size());
            }

            runnableQueue.put(future);
            finished = true;
        } while (!finished);
    }

    /**
     * Submit a task to be executed on the thread pool.
     * 
     * @param task The task to be executed
     * @return a Future representing pending completion of the task
     */
    public Future<Boolean> submit(RunnableWithIdIfc task) throws InterruptedException {
        if (isShutdown) {
            throw new IllegalStateException("shutdown");
        }

        FutureTaskWithId<Boolean> future = new FutureTaskWithId<Boolean>(task, true);
        submit(future);
        return future;
    }

    /**
     * Submit a task to be executed on the thread pool.
     * 
     * @param task The task to be executed
     * @param <T> The type of data
     * @return a Future representing pending completion of the task
     */
    public <T> Future<T> submit(CallableWithIdIfc<T> task) throws InterruptedException {
        if (isShutdown) {
            throw new IllegalStateException("shutdown");
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
            throw new IllegalStateException("shutdown");
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
            throw new IllegalStateException("shutdown");
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
            throw new IllegalStateException("shutdown");
        }

        return eventList.scheduleAtFixedRate(command, initialDelay, period);
    }

    /** Shutdown this ThreadPool. */
    public final void shutdown() {
        if (DEFAULT_THREAD_POOL != null && this == DEFAULT_THREAD_POOL) {
            DEFAULT_THREAD_POOL = null;
        }

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
