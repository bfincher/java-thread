package com.fincher.thread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to schedule events to execute after a delay.
 *
 * @author Brian Fincher
 *
 */
public class EventList {

    private static final Logger LOG = LoggerFactory.getLogger(EventList.class);
    
    private static final String IS_TERMINATED_MSG = "terminated";

    private class EventListRunnable implements MyCallableIfc<> {
        /**
         * Performs maintenance on the EventMap. Events that should be executed now are removed from
         * the map and executed. Otherwise, this thread sleeps until the next execution time
         */
        @Override
        public Boolean call() throws InterruptedException {
            synchronized (eventList) {
                while (eventList.isEmpty() && !terminated) {
                    try {
                        eventList.wait();
                    } catch (InterruptedException ie) {
                        LOG.info(ie.getMessage(), ie);
                        Thread.currentThread().interrupt();
                    }
                }

                if (terminated) {
                    return false;
                }

                EventWrapper event = eventList.peek();
                long delayMillis = event.getDelay(TimeUnit.MILLISECONDS);
                while (delayMillis > 0) {
                    eventList.wait(delayMillis);
                    delayMillis = event.getDelay(TimeUnit.MILLISECONDS);
                }

                eventList.remove();

                if (LOG.isTraceEnabled()) {
                    LOG.trace("EventList:  Submitting event {} for execution",
                            event.getTask().getId());
                }

                threadPool.submit(event);
            }
            return true;
        }

        /**
         * Should this thread continue to execute
         * 
         * @return true if this thread should continue to execute
         */
        @Override
        public boolean continueExecution() {
            return !terminated;
        }

        /**
         * Terminates this EventList. This method should not be called directly. It will be called
         * by the parent thread
         */
        @Override
        public void terminate() {
            terminated = true;
        }
    }

    /** A list used to hold events to be executed in the future */
    private BlockingQueue<EventWrapper> eventList = new PriorityBlockingQueue<EventWrapper>();

    /** The thread in which this EventList's maintenance thread is executed */
    private MyThread thread;

    /** Has this EventList been terminated */
    private volatile boolean terminated = false;

    private final ThreadPool threadPool;

    private final EventListRunnable runnable = new EventListRunnable();

    private static List<EventList> instances = Collections
            .synchronizedList(new ArrayList<EventList>());

    /** Creates a new EventList instance */
    protected EventList(ThreadPool threadPool) {
        this.threadPool = threadPool;
        thread = new MyThread("EventList", runnable);
        thread.start();
        instances.add(this);
    }

    /** Shutdown the EventList */
    public void shutdown() {
        terminated = true;
        thread.terminate();
        thread = null;
        eventList.clear();
        instances.remove(this);
    }

    /**
     * Creates and executes a one-shot action that becomes enabled after the given delay.
     *
     * @param command the task to execute
     * @param delay   the time from now to delay execution
     * @return A Future task used to control the event
     */
    public MyRunnableScheduledFuture<Boolean> schedule(RunnableWithIdIfc command, long delay) {

        if (terminated) {
            throw new IllegalStateException(IS_TERMINATED_MSG);
        }

        EventWrapper event = new EventWrapper(command, System.currentTimeMillis() + delay);
        addToEventList(event);
        return event;
    }

    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial
     * delay, and subsequently with the given delay between the termination of one execution and the
     * commencement of the next.
     * 
     * @param command      The task to execute
     * @param initialDelay The time to delay first execution
     * @param period       The period between executions
     * @return A Future task used to control the event
     */
    public MyRunnableScheduledFuture<Boolean> scheduleWithFixedDelay(RunnableWithIdIfc command,
            long initialDelay, long period) {

        if (terminated) {
            throw new IllegalStateException(IS_TERMINATED_MSG);
        }

        EventWrapper event = new FixedDelayEventWrapper(command,
                System.currentTimeMillis() + initialDelay, period, this);
        addToEventList(event);
        return event;
    }

    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial
     * delay, and subsequently with the given delay between the start of one execution and the
     * commencement of the next (however, subsequent executions will not begin until previous
     * executions have completed).
     * 
     * @param command      The task to execute
     * @param initialDelay The time to delay first execution
     * @param period       The period between executions
     * @return A Future task used to control the event
     */
    public MyRunnableScheduledFuture<Boolean> scheduleAtFixedRate(RunnableWithIdIfc command,
            long initialDelay, long period) {

        if (terminated) {
            throw new IllegalStateException(IS_TERMINATED_MSG);
        }

        EventWrapper event = new FixedRateEventWrapper(command,
                System.currentTimeMillis() + initialDelay, period, this);
        addToEventList(event);
        return event;
    }

    /**
     * Adds an event to the eventMap
     * 
     * @param time    The time at which the event should be executed (in millis since 1970)
     * @param wrapper The EventWrapper
     */
    protected final void addToEventList(EventWrapper wrapper) {
        synchronized (eventList) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("EventList:  Scheduling event: {}", wrapper.getTask().getId());
            }

            eventList.add(wrapper);
            eventList.notifyAll();
        }
    }
}
