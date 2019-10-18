package com.fincher.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 *  
 * This thread wrapper provides default behaviors for killing threads and handling exceptions.
 * 
 * To implement, instantiate this class with a Runnable object.  
 * This Runnable should not contain a indefinite loop,
 * instead, it should have code processing a single iteration of it's task.  
 * <p>
 * For example:  A traditional Runnable's run method may look like:
 * <code>
 * 
 * public void run() {
 *     while(!terminate) {
 *         // some action
 *     }    
 * }
 * </code>
 * 
 * Instead the Runnable's run method should look like:
 * <code>
 * 
 * public void run() {
 *     // some action    
 * }
 * </code>
 * <p>
 * 
 * This is because this Class will handle the looping to ensure proper exception 
 * handling and shutdown procedures.
 * 
 * An optional ExceptionListener may be set to be notified of Exceptions
 * 
 * </pre>
 *
 * @author Brian Fincher
 *
 */
public class MyThread extends Thread implements Runnable, MyThreadIfc {

    private static final Logger LOG = LoggerFactory.getLogger(MyThread.class);

    /** Should this thread terminate? */
    private volatile boolean terminate = false;

    /** The user's object that will be invoked upon each thread iteration */
    private final MyRunnableIfc runnable;
    
    private final MyCallableIfc<Boolean> callable;

    /** Used to notify the user of any exceptions in the thread's body */
    private ExceptionHandlerIfc exceptionHandler = null;

    /**
     * Should execution continue after an exception is encountered. Defaults to true
     */
    private volatile boolean continueAfterException = true;

    /**
     * Constructs a new MyThread
     * 
     * @param name     The name of this thread
     * @param runnable To be invoked upon each thread iteration
     */
    public MyThread(String name, MyRunnableIfc runnable) {
        super(name);
        this.runnable = runnable;
        callable = null;
    }
    
    /**
     * Constructs a new MyThread
     * 
     * @param name     The name of this thread
     * @param runnable To be invoked upon each thread iteration
     */
    public MyThread(String name, MyCallableIfc<Boolean> callable) {
        super(name);
        this.callable = callable;
        runnable = null;
    }

    /**
     * Should execution continue after an exception is encountered. Defaults to true
     */
    public void setContinueAfterException(boolean val) {
        this.continueAfterException = val;
    }

    /**
     * Sets a handler that will be called upon exceptions being thrown in this thread's body
     */
    public void setExceptionHandler(ExceptionHandlerIfc exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    /** Should not be called directly */
    @Override
    public void run() {
        boolean continueExecution;
        do {
            try {
                if (runnable != null) {
                    runnable.run();
                } else {
                    callable.call();
                }
            } catch (Throwable t) {
                if (exceptionHandler == null) {
                    LOG.error(getName() + " " + t.getMessage(), t);
                } else {
                    exceptionHandler.onException(t);
                }

                if (!continueAfterException) {
                    LOG.error("{} Execution terminating due to exception", getName());
                    terminate = true;
                }
            }
            
            if (runnable != null) {
                continueExecution = runnable.continueExecution();
            } else {
                continueExecution = callable.continueExecution();
            }
        } while (!terminate && continueExecution);

        LOG.debug("{} terminated", getName());
    }

    /** Terminates this thread */
    public void terminate() {
        terminate = true;
        interrupt();
        
        if (runnable != null) {
            runnable.terminate();
        } else {
            callable.terminate();
        }
    }

    /** Has this thread been terminated */
    public boolean isTerminated() {
        return terminate;
    }

    /** Gets the runnable object associated with this thread */
    public MyRunnableIfc getRunnable() {
        return runnable;
    }

}
