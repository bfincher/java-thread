package com.fincher.thread;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;

import junit.framework.Assert;


/** A JUnit tester for the thread pool */
public class ThreadPoolTester {
	
	private static class TestRunnable implements RunnableWithIdIfc {
		
		public static final int NUM_ITERATIONS = 10;
		
		private String id;
		
		private final BlockingQueue<String> queue;
		
		public TestRunnable(String id, BlockingQueue<String> queue) {
			this.queue = queue;
			this.id = id;
		}
		
		@Override
		public String getId() {
			return id;
		}
		
		@Override
		public void run() {
			for (int i = 0; i < NUM_ITERATIONS; i++) {
				String str = id + " iteration " + i;
				System.out.println(str);
				queue.add(str);
				try {
				    Thread.sleep(500);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		}
	}		
	
	
	
	private void runTestWithXThreads(int numThreads) throws InterruptedException {
		ThreadPool threadPool = new ThreadPool(numThreads, "test");
		
		int numSubmissions = numThreads * 2;
		
		ArrayList<Future<?>> futures = new ArrayList<Future<?>>(numSubmissions);
		ArrayList<BlockingQueue<String>> queueList = new ArrayList<BlockingQueue<String>>(numSubmissions);
		
		for (int i = 0; i < numThreads * 2; i++) {
			LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
			queueList.add(queue);
			futures.add(threadPool.submit(new TestRunnable(String.valueOf(i), queue)));
		}
		
		try {
			for (Future<?> future: futures) {
				future.get();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		for (BlockingQueue<String> queue: queueList) {
			if (queue.size() != TestRunnable.NUM_ITERATIONS) {
				Assert.fail("size = " + queue.size());
			}
		}
		threadPool.shutdown();
	}
	
	/** Method name is self explanatory
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testOneThread() throws InterruptedException {
		System.out.println("Testing with one thread");
		runTestWithXThreads(1);
	}
	
	/** Method name is self explanatory
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testTwoThread() throws InterruptedException {
		System.out.println("Testing with two threads");
		runTestWithXThreads(2);
	}		
}
