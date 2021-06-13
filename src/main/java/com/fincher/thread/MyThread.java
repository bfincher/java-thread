package com.fincher.thread;

import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <pre>
 *  
 * This thread wrapper provides default behaviors for killing threads and handling exceptions.
 * 
 * To implement, instantiate this class with a Runnable object.  
 * This Runnable should not contain a indefinite loop,
 * instead, it should have code processing a single iteration of it's task.  
 *
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
 * 
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

    private static final Logger LOG = LogManager.getLogger();

    /** Should this thread terminate?.*/
    private volatile boolean terminate = false;
    
    private final Runnable runnable;
    
    private final BooleanSupplier continueExecutionMethod;
    
    private final Runnable terminateMethod;
    
    private final Optional<MyRunnableIfc> userRunnable;
    private final Optional<MyCallableIfc<?>> userCallable;

    /** Used to notify the user of any exceptions in the thread's body. */
    private ExceptionHandlerIfc exceptionHandler = null;

    /**
     * Should execution continue after an exception is encountered. Defaults to true
     */
    private volatile boolean continueAfterException = true;

    /**
     * Constructs a new MyThread.
     * 
     * @param name     The name of this thread
     * @param runnable To be invoked upon each thread iteration
     */
    public MyThread(String name, MyRunnableIfc runnable) {
        super(name);
        userRunnable = Optional.of(runnable);
        userCallable = Optional.empty();
        this.runnable = runnable;
        terminateMethod = runnable::terminate;
        continueExecutionMethod = runnable::continueExecution;
    }

    /**
     * Constructs a new MyThread.
     * 
     * @param name     The name of this thread
     * @param callable To be invoked upon each thread iteration
     */
    public MyThread(String name, MyCallableIfc<?> callable) {
        super(name);
        userCallable = Optional.of(callable);
        userRunnable = Optional.empty();
        continueExecutionMethod = callable::continueExecution;
        terminateMethod = callable::terminate;
        
        runnable = () -> {
            try {
                callable.call();
            } catch (Exception e) {
                handleException(e);
            }
        };
    }

    @Override
    public void setContinueAfterException(boolean val) {
        this.continueAfterException = val;
    }

    @Override
    public void setExceptionHandler(ExceptionHandlerIfc exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    // Should not be called directly
    @Override
    public void run() {
        boolean continueExecution;
        do {
            try {
                runnable.run();
            } catch (Throwable t) {
                handleException(t);
            }

            continueExecution = continueExecutionMethod.getAsBoolean();
        } while (!terminate && continueExecution);

        LOG.debug("{} terminated", getName());
    }

    @Override
    public void terminate() {
        terminate = true;
        interrupt();

        terminateMethod.run();
    }

    @Override
    public boolean isTerminated() {
        return terminate;
    }

    @Override
    public Optional<MyRunnableIfc> getRunnable() {
        return userRunnable;
    }

    @Override
    public Optional<MyCallableIfc<?>> getCallable() {
        return userCallable;
    }

    private void handleException(Throwable t) {
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

}
