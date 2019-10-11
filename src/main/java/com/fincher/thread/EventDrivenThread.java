package com.fincher.thread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

/**
 * A Thread that takes action upon receipt of data. The thread sleep / wait when no data is
 * available and data will be processed in a FIFO manner.
 * 
 * @author Brian Fincher
 *
 * @param <T>
 */

public class EventDrivenThread<T> extends MyThread {

    private static final Logger logger = Logger.getLogger(EventDrivenThread.class);

    /** The queue used to feed data to the Thread */
    private final BlockingQueue<T> queue;

    private static class Executor<T> implements MyRunnableIfc {

        private final BlockingQueue<T> queue;

        private final DataHandlerIfc<T> messageHandler;

        public Executor(BlockingQueue<T> queue, DataHandlerIfc<T> messageHandler) {
            this.queue = queue;
            this.messageHandler = messageHandler;
        }

        @Override
        public void run() {
            try {
                T data = queue.take();
                messageHandler.handleMessage(data);
            } catch (InterruptedException e) {
                logger.info(e.getMessage(), e);
            }
        }

        @Override
        public boolean continueExecution() {
            return true;
        }

        @Override
        public void terminate() {
        }
    }

    /**
     * Get a new EventDrivenThread
     * 
     * @param                <T>
     * @param id             The ID of this EventDrivenThread
     * @param messageHandler The object that will handle messages
     * @return The newly created EventDrivenThread
     */
    public static <T> EventDrivenThread<T> getEventDrivenThread(String id,
            DataHandlerIfc<T> messageHandler) {
        BlockingQueue<T> queue = new LinkedBlockingQueue<T>();
        return new EventDrivenThread<T>(id, queue, new Executor<T>(queue, messageHandler));
    }

    /**
     * Get a new EventDrivenThread
     * 
     * @param                <T>
     * @param id             The ID of this EventDrivenThread
     * @param messageHandler The object that will handle messages
     * @return The newly created EventDrivenThread
     */
    public static <T> EventDrivenThread<T> getEventDrivenThread(String id,
            DataHandlerIfc<T> messageHandler, BlockingQueue<T> queue) {
        return new EventDrivenThread<T>(id, queue, new Executor<T>(queue, messageHandler));
    }

    private EventDrivenThread(String id, BlockingQueue<T> queue, Executor<T> executor) {
        super(id, executor);
        this.queue = queue;
    }

    /**
     * Add a new message to the queue to be processed
     * 
     * @param data The message to be added to the queue
     */
    public void submit(T data) {
        queue.add(data);
    }

    /**
     * Gets the queue used to feed this EventDrivenThread. Note that the get methods should not be
     * called on this queue
     * 
     * @return The queue used to feed this EventDrivenThread
     */
    public BlockingQueue<T> getQueue() {
        return queue;
    }
}
