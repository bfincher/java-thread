package com.fincher.thread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ThreadPoolRunnable implements MyCallableIfc<Boolean> {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadPoolRunnable.class);

    private final BlockingQueue<FutureTaskWithId<?>> queue;

    private String origThreadName = null;

    public ThreadPoolRunnable(BlockingQueue<FutureTaskWithId<?>> queue) {
        this.queue = queue;
    }

    @Override
    public Boolean call() throws InterruptedException, ExecutionException {
        FutureTaskWithId<?> future = queue.take();

        if (LOG.isTraceEnabled()) {
            LOG.trace("Begin execution of task: {}", future.getId());
        }

        try {
            future.run();

            // call get to determine if an exception occurred in the call
            future.get();
            return true;
        } finally {
            if (origThreadName != null) {
                Thread.currentThread().setName(origThreadName);
                origThreadName = null;
            }

            if (LOG.isTraceEnabled()) {
                LOG.trace("Completed execution of task: {}", future.getId());
            }
        }
    }

    @Override
    public boolean continueExecution() {
        return true;
    }

    @Override
    public void terminate() {
        // no action necessary
    }

}
