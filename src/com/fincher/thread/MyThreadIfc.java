package com.fincher.thread;

/** A Thread abstraction */
public interface MyThreadIfc {
	
	/** Should execution continue after an exception is encountered.  Defaults to true
	 * @param val the boolean value
	 */
	public void setContinueAfterException(boolean val);
	
	/** Terminates this thread */
	public void terminate();
	
	/** Has this thread been terminated 
	 * @return true if terminated 
	 */
	public boolean isTerminated();
	
	/** Gets the ID of this thread 
	 * @return The name of this thread
	 */
	public String getName();
	
	/** Sets a handler that will be called upon exceptions being thrown in this thread's body
	 * @param exceptionHandler The exception handler
	 */
	public void setExceptionHandler(ExceptionHandlerIfc exceptionHandler);
	
	/** See {@link Thread#start} */
	public void start();
	
	/** Waits for this thread to die.
	 * @throws InterruptedException
	 */
	public void join() throws InterruptedException;
	
	/** Waits at most millis milliseconds for this thread to die. A timeout of 0 means to wait forever.
	 * @param millis The amount of time to wait for the thread to terminate before returning
	 * @throws InterruptedException
	 */
	public void join(long millis) throws InterruptedException;
	
	/** Gets the runnable object associated with this thread
	 * 
	 * @return The runnable object associated with this thread
	 */
	public MyRunnableIfc getRunnable();

}
