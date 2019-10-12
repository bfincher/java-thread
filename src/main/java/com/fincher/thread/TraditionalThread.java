package com.fincher.thread;

import org.apache.log4j.Logger;

/**
 * A simple extension of the Java Thread that attempts to catch Exceptions
 * 
 * @author Brian Fincher
 *
 */
public final class TraditionalThread extends Thread {

    private static Logger logger = Logger.getLogger(TraditionalThread.class);

    private final Runnable target;

    /**
     * Constructs a new TraditionalThread
     * 
     * @param target The runnable target
     */
    public TraditionalThread(Runnable target) {
        super();
        this.target = target;
    }

    /**
     * Constructs a new TraditionalThread
     * 
     * @param target The runnable target
     * @param name   The name of the thread
     */
    public TraditionalThread(Runnable target, String name) {
        super(name);
        this.target = target;
    }

    /**
     * Constructs a new TraditionalThread
     * 
     * @param group  The Java ThreadGroup
     * @param target The runnable target
     * @param name   The name of the thread
     */
    public TraditionalThread(ThreadGroup group, Runnable target, String name) {
        super(group, name);
        this.target = target;
    }

    @Override
    public void run() {
        try {
            target.run();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }

}
