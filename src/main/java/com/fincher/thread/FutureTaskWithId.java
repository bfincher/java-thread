package com.fincher.thread;

import java.util.concurrent.FutureTask;

/**
 * See java.util.concurrent.FutureTask
 * 
 * @author Brian Fincher
 * 
 * @see java.util.concurrent.FutureTask
 * 
 * @param <T> The type of data returned by the task
 *
 */

class FutureTaskWithId<T> extends FutureTask<T> implements IdentifiableIfc {

    private final IdentifiableIfc task;

    /**
     * Constructs a new MyFutureTask
     * 
     * @param task   The task to be executed
     * @param result This param is really meaningless but the Java FutureTask requires that a result
     *               be provided with a runnable. Recommendation is to use Boolean (true).
     */
    public FutureTaskWithId(RunnableWithIdIfc task, T result) {
        super(task, result);
        this.task = task;
    }

    /**
     * Constructs a new MyFutureTask
     * 
     * @param task The task to be executed
     */
    public FutureTaskWithId(CallableWithIdIfc<T> task) {
        super(task);
        this.task = task;
    }

    /**
     * Gets the task's ID
     *
     * @return String
     */
    @Override
    public String getId() {
        return task.getId();
    }

    /**
     * Get the the task to be executed by this future
     * 
     * @return the the task to be executed by this future
     */
    protected IdentifiableIfc getTask() {
        return task;
    }

}
