package com.fincher.thread;

import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

class ThreadPoolRunnable implements MyRunnableIfc {

    private static Logger logger = Logger.getLogger(ThreadPoolRunnable.class);

    private final BlockingQueue<FutureTaskWithId<?>> queue;

    private String origThreadName = null;

    public ThreadPoolRunnable(BlockingQueue<FutureTaskWithId<?>> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        FutureTaskWithId<?> future;
        try {
            future = queue.take();
        } catch (InterruptedException ie) {
            logger.info("If this occurs at shutdown, ignore", ie);
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Begin execution of task: " + future.getId());
        }

        try {
            future.run();

            // call get to determine if an exception occurred in the call
            future.get();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);

            // since runnable.run can't throw an Exception, wrap the exception in an Error
            // and throw
            throw new Error(t);
        } finally {
            if (origThreadName != null) {
                Thread.currentThread().setName(origThreadName);
                origThreadName = null;
            }

            if (logger.isTraceEnabled()) {
                logger.trace("Completed execution of task: " + future.getId());
            }
        }
    }

    @Override
    public boolean continueExecution() {
        return true;
    }

    @Override
    public void terminate() {

    }

}
