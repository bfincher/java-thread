package com.fincher.thread;

abstract class RepeatingEventWrapper extends EventWrapper {

    /** The interval in milliseconds that this event will repeat */
    protected final long interval;

    /** Used to re-schedule events */
    private final EventList parentEventList;

    /**
     * Creates a new RepeatingEventWrapper
     * 
     * @param task              The body of the event
     * @param nextExecutionTime The time at which this event should execute (millis since 1970)
     * @param interval          The interval in milliseconds for subsequent event executions
     * @param parentEventList   Used to re-schedule events
     */
    public RepeatingEventWrapper(RunnableWithIdIfc task, long nextExecutionTime, long interval,
            EventList parentEventList) {
        super(task, nextExecutionTime);
        this.interval = interval;
        this.parentEventList = parentEventList;
    }

    @Override
    protected void postExecute() {
        synchronized (stateEnumSynchronizer) {
            if (!isCancelled()) {
                nextExecutionTime = getNextExecutionTime();
                parentEventList.addToEventList(this);
                setState(StateEnum.PENDING);
            }
        }
    }

    @Override
    public boolean isPeriodic() {
        return true;
    }

    /**
     * Get the time at which this event should be executed next
     * 
     * @return the time at which this event should be executed next
     */
    protected abstract long getNextExecutionTime();

}
