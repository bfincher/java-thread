package com.fincher.thread;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * <pre>
 *  A task that runs forever providing default behaviors for killing tasks and handling exceptions.
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
public final class LongLivedTask<T> {

    private static final Logger LOG = LoggerFactory.getLogger(LongLivedTask.class);

    private final Task controllable;
    private final Runnable runnable;
    private Consumer<Throwable> exceptionHandler = null;
    private volatile boolean continueAfterException = true;

    private volatile State state = State.INITIAL;
    private Throwable exception = null;
    private T result = null;
    private final Lock stateLock = new ReentrantLock();
    private final Condition stateChangedCondition = stateLock.newCondition();
    private Thread thread;
    private final String name;

    private enum State {
        INITIAL, RUNNING, CANCELLED, TERMINATED;
    }

    /**
     * Creates a new LongLivedTask
     * 
     * @param name The name of the thread used to execute the task
     * @param task To be invoked upon each thread iteration
     * @return A new LongLovedTask
     */
    public static LongLivedTask<Void> create(String name, RunnableTask task) {
        return new LongLivedTask<>(name, task);
    }

    /**
     * Constructs a new LongLivedTask.
     * 
     * @param name The name of the thread used to execute the task
     * @param task To be invoked upon each thread iteration
     * @param <T> The return type of the Callable
     * @return A new LongLovedTask
     */
    public static <T> LongLivedTask<T> create(String name, CallableTask<T> task) {
        return new LongLivedTask<>(name, task);
    }

    /**
     * Constructs a new LongLivedTask
     * 
     * @param name The name of the thread used to execute the task
     * @param runnable To be invoked upon each thread iteration
     */
    private LongLivedTask(String name, RunnableTask runnable) {
        this.name = name;
        controllable = runnable;
        this.runnable = runnable;
    }

    /**
     * Constructs a new LongLivedTask.
     * 
     * @param name The name of the thread used to execute the task
     * @param callable To be invoked upon each thread iteration
     */
    private LongLivedTask(String name, CallableTask<T> callable) {
        this.name = name;
        controllable = callable;
        runnable = () -> {
            try {
                result = callable.call();
            } catch (Exception t) {
                throw new UncheckedException(t);
            }
        };
    }

    /**
     * Start the execution of this task
     * 
     * @return A future representing the task
     */
    public Future<T> start() {
        Preconditions.checkState(state == State.INITIAL, "Expected state to be INITIAL but was %s", state);
        thread = new Thread(this::run);
        thread.setName(name);
        thread.start();
        setState(State.RUNNING);
        return new LongLivedTaskFuture();
    }

    /**
     * Specifies if this task should continue after encountering an exception
     * 
     * @param val true if this task should continue after encountering an exception
     */
    public void setContinueAfterException(boolean val) {
        this.continueAfterException = val;
    }

    /**
     * Sets a handler to be notified of encountered exceptions
     * 
     * @param exceptionHandler The exception handler;
     */
    public void setExceptionHandler(Consumer<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    private void run() {

        boolean continueExecution;
        do {
            try {
                runnable.run();
            } catch (RuntimeException re) {
                if (re.getCause() == null) {
                    handleException(re);
                } else {
                    handleException(re.getCause());
                }
            } catch (Throwable t) {
                handleException(t);
            }

            continueExecution = controllable.continueExecution();
        } while (state != State.TERMINATED && state != State.CANCELLED && continueExecution);

        setState(State.TERMINATED);

        LOG.debug("{} terminated", name);
    }

    private void handleException(Throwable t) {
        if (exceptionHandler == null) {
            LOG.atError().setMessage("{} {}")
                    .addArgument(name)
                    .addArgument(t::getMessage)
                    .setCause(t);
        } else {
            exceptionHandler.accept(t);
        }

        if (!continueAfterException) {
            exception = t;
            setState(State.TERMINATED);
            LOG.error("{} Execution terminating due to exception", name);
        }
    }

    private void setState(State state) {
        stateLock.lock();
        try {
            this.state = state;
            stateChangedCondition.signalAll();
        } finally {
            stateLock.unlock();
        }
    }

    private class LongLivedTaskFuture implements Future<T> {
        @Override
        public boolean cancel(boolean interruptIfRunning) {
            if (isCancelled()) {
                if (state == State.RUNNING) {
                    thread.interrupt();
                    return true;
                }
                return false;
            } else {
                if (state == State.RUNNING) {
                    if (interruptIfRunning) {
                        thread.interrupt();
                    }

                    controllable.terminate();
                }
                setState(State.CANCELLED);

                return true;
            }
        }

        @Override
        public T get(long timeToWait, TimeUnit unit) throws InterruptedException, ExecutionException {
            Instant waitUntil = Instant.now().plus(Duration.ofNanos(unit.toNanos(timeToWait)));
            stateLock.lock();
            try {
                Instant now = Instant.now();
                Duration waitTime = Duration.between(now, waitUntil);
                while (waitTime.compareTo(Duration.ZERO) > 0
                        && state != State.TERMINATED && state != State.CANCELLED) {
                    stateChangedCondition.await(waitTime.toNanos(), TimeUnit.NANOSECONDS); // NOSONAR

                    now = Instant.now();
                    waitTime = Duration.between(now, waitUntil);
                }
            } finally {
                stateLock.unlock();
            }

            switch (state) {
            case CANCELLED:
                throw new CancellationException();

            case TERMINATED:
                if (exception == null) {
                    return result;
                } else {
                    throw new ExecutionException(exception);
                }

            default:
                throw new IllegalStateException();
            }
        }

        public T get() throws InterruptedException, ExecutionException {
            return get(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        @Override
        public boolean isCancelled() {
            return state == State.CANCELLED;
        }

        @Override
        public boolean isDone() {
            return state == State.CANCELLED || state == State.TERMINATED;
        }
    }

}
