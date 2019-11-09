package com.fincher.thread;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadPool extends ScheduledThreadPoolExecutor {
    
    private static final Logger LOG = LoggerFactory.getLogger(ThreadPool.class);
    
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
    
    
    protected void beforeExecute(Thread t, Runnable r) {
        if (r instanceof RunnableWithIdIfc) {
            LOG.info("Begin executing {}", ((RunnableWithIdIfc)r).getId());
        }
    }
    
    protected void afterExecute(Runnable r, Throwable t) {
        String id = null;
        if (r instanceof RunnableWithIdIfc) {
            id = ((RunnableWithIdIfc)r).getId();
            LOG.info("Finished executing {}", id);
        }
        
        if (t != null) {
            StringBuilder sb = new StringBuilder();
            if (id != null) {
                sb.append(id + " ");
            }
            sb.append("exception thrown by event: ");
            LOG.error(sb.toString(), t);
        }
    }

}
