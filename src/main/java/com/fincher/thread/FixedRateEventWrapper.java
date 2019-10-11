package com.fincher.thread;

class FixedRateEventWrapper extends RepeatingEventWrapper {

    /**
     * Creates a new RepeatingEventWrapper
     * 
     * @param task              The body of the event
     * @param nextExecutionTime The time at which this event should execute (in millis since 1970)
     * @param interval          The interval in milliseconds for subsequent event executions
     * @param parentEventList   Used to re-schedule events
     */
    public FixedRateEventWrapper(RunnableWithIdIfc task, long nextExecutionTime, long interval,
            EventList parentEventList) {
        super(task, nextExecutionTime, interval, parentEventList);
    }

    @Override
    protected long getNextExecutionTime() {
        return getBeginExecutionTime() + interval;
    }

}
