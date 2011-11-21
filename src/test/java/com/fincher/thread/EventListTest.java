package com.fincher.thread;

import static junit.framework.Assert.fail;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;

import org.junit.Test;

/** Test the EventList */
public class EventListTest {
	
//	@Override
//	public void setUp() {
//		Logger.getRootLogger().setLevel(Level.TRACE);
//	}
	
	private static class TestRunnable implements RunnableWithIdIfc {			
		
		private String id;
		
		private final BlockingQueue<String> queue;
		
		private final int numIterations;
		
		public TestRunnable(String id, BlockingQueue<String> queue, int numIterations) {
			this.queue = queue;
			this.id = id;
			this.numIterations = numIterations;
		}
		
		@Override
		public String getId() {
			return id;
		}
		
		@Override
		public void run() {
			for (int i = 0; i < numIterations; i++) {
				String str = id + " iteration " + i;
				System.out.println(str);
				queue.add(str);				
			}
			
		}
	}
	
	private static class TestRunnableFuture implements RunnableWithIdIfc {
		
		private String id;
		
		private final BlockingQueue<String> queue;
		
		private final int sleepTime;
		
		public TestRunnableFuture(String id, BlockingQueue<String> queue) {
			this.id = id;
			this.queue = queue;
			sleepTime = 0;
		}
		
		public TestRunnableFuture(String id, BlockingQueue<String> queue, int sleepTime) {
			this.id = id;
			this.queue = queue;
			this.sleepTime = sleepTime;
		}
		
		@Override
		public String getId() {
			return id;
		}
		
		@Override
		public void run() {
			queue.add(id + " executing " + System.currentTimeMillis());
			if (sleepTime > 0)
				try {
					Thread.sleep(sleepTime);
				}
				catch (InterruptedException ie) {
					ie.printStackTrace();
				}
		}
	}
	
	/** Method name is self explanatory
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testTimer() throws InterruptedException {
		System.out.println("testTimer");
		ThreadPool threadPool = ThreadPool.getDefaultThreadPool();
		long startTime = System.currentTimeMillis();
		System.out.println("Scheduling...");
		
		BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
		ScheduledFuture<?> future = threadPool.schedule(new TestRunnable("testTimer", queue, 10), 500);				
		
		try {
			System.out.println(future.get());
		}
		catch (ExecutionException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		Thread.sleep(100);
		int size = queue.size();
		if (size != 10) {
			System.out.println("size = " + size);
			fail("size = " + size);
		}
		
		long endTime = System.currentTimeMillis();
		
		long delta = endTime - startTime;
		System.out.println("Finished after " + delta + " millis");
	}
	
	/** Test a timer that is cancelled before it is executed 
	 * @throws InterruptedException
	 */
	@Test
	public void testCancelTimer() throws InterruptedException {
		System.out.println("testCancelTimer");
		ThreadPool threadPool = ThreadPool.getDefaultThreadPool();
		
		BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
		ScheduledFuture<?> future = threadPool.schedule(new TestRunnable("testTimer", queue, 10), 1000);
		
		future.cancel(false);
		
		Thread.sleep(1500);
		
		int size = queue.size();
		if (size != 0) {
			System.out.println(size);
			fail();
		}				
	}
	
	/** Method name is self explanatory
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testFixedDelay() throws InterruptedException {
		System.out.println("testFixedDelay");
		ThreadPool threadPool = ThreadPool.getDefaultThreadPool();
		
		System.out.println("Scheduling at " + System.currentTimeMillis());
		LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
		Future<?> future = threadPool.scheduleWithFixedDelay(new TestRunnableFuture("testPeriodicTimer", queue), 500, 1000);
		
		Thread.sleep(10000);
		future.cancel(false);
		
		try {
			future.get();
		}
		catch (CancellationException ce) {
			// this is expected
		}
		catch (ExecutionException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		int size = queue.size();
		if (size != 10) {
			System.out.println(size);
			fail();
		}
	}
	
	/** Method name is self explanatory
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testFixedRate() throws InterruptedException {
		System.out.println("Test FixedRate");
				
		ThreadPool threadPool = ThreadPool.getDefaultThreadPool();
		LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
		
		TestRunnableFuture trf = new TestRunnableFuture("testPeriodicTimer", queue, 1500);
		
		Future<?> future = threadPool.scheduleAtFixedRate(trf, 0, 2000);
		
		Thread.sleep(10000);
		future.cancel(false);
		
		try {
			future.get();
		}
		catch (CancellationException ce) {
			// this is expected
		}
		catch (ExecutionException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		int size = queue.size();
		if (size != 5) {
			System.out.println(size);
			fail();
		}
	}


}
