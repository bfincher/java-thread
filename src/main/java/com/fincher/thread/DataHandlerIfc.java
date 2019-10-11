package com.fincher.thread;

/** Used as a callback mechanism to notify message listeners of available data.
 * 
 * @author Brian Fincher
 *
 * @param <T> The type of data
 */

public interface DataHandlerIfc<T> {

    /** A message has arrived that should be processed.
     * 
     * @param data The message to be processed
     */
    public void handleMessage(T data);

}
