package com.fincher.thread;

/**
 * A task to be executed in a thread or thread pool.
 * 
 * @author Brian Fincher
 *
 */
public interface Task {

    /**
     * Should this task continue to be executed.
     * 
     * @return True if execution should continue for another iteration
     */
    public boolean continueExecution();

    /**
     * Called when the parent thread is terminating to give the task an opportunity
     * to clean up.
     */
    public void terminate();

}
