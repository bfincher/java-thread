package com.fincher.thread;

import java.util.concurrent.RunnableScheduledFuture;

/**
 * Represents a pending event
 * 
 * @author Brian Fincher
 *
 * @param <T>
 */
public interface MyRunnableScheduledFuture<T> extends RunnableScheduledFuture<T> {

    /**
     * Get the underlying runnable to be executed via this event
     * 
     * @return the underlying runnable to be executed via this event
     */
    public RunnableWithIdIfc getTask();

}
