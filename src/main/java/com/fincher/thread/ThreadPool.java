package com.fincher.thread;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool extends ScheduledThreadPoolExecutor {

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
    
}
