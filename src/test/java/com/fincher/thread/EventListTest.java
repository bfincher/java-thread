package com.fincher.thread;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Test the EventList */
public class EventListTest {

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
            System.out.println("adding to queue");
            queue.add(id + " executing " + System.currentTimeMillis());
            if (sleepTime > 0) {
                Awaitility.await().atLeast(Duration.ofMillis(sleepTime));
            }
        }
    }

    private ThreadPool threadPool;

    @Before
    public void setUp() {
        threadPool = new ThreadPool(10, "defaultThreadPool");
    }

    @After
    public void tearDown() {
        threadPool.shutdown();
        threadPool = null;
    }

    /**
     * Method name is self explanatory
     * 
     * @throws InterruptedException
     */
    @Test(timeout = 5000)
    public void testTimer() throws InterruptedException {
        System.out.println("testTimer");
        final long startTime = System.currentTimeMillis();
        System.out.println("Scheduling...");

        BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
        ScheduledFuture<?> future = threadPool.schedule(new TestRunnable("testTimer", queue, 10),
                500);

        try {
            System.out.println(future.get());
        } catch (ExecutionException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        Awaitility.await().until(() -> queue.size() == 10);

        long endTime = System.currentTimeMillis();

        long delta = endTime - startTime;
        System.out.println("Finished after " + delta + " millis");
    }

    /**
     * Test a timer that is cancelled before it is executed
     * 
     * @throws InterruptedException
     */
    @Test
    public void testCancelTimer() throws InterruptedException {
        System.out.println("testCancelTimer");

        BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
        ScheduledFuture<?> future = threadPool.schedule(new TestRunnable("testTimer", queue, 10),
                1000);

        future.cancel(false);

        Awaitility.await().atLeast(2, TimeUnit.SECONDS);

        assertTrue(queue.isEmpty());
    }

    /**
     * Method name is self explanatory
     * 
     * @throws InterruptedException
     */
    @Test
    public void testFixedDelay() throws InterruptedException {
        System.out.println("testFixedDelay");

        System.out.println("Scheduling at " + System.currentTimeMillis());
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
        Future<?> future = threadPool.scheduleWithFixedDelay(
                new TestRunnableFuture("testPeriodicTimer", queue), 500, 1000);

        Thread.sleep(10000);
        future.cancel(false);

        try {
            future.get();
        } catch (CancellationException ce) {
            // this is expected
        } catch (ExecutionException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        
        assertEquals(10, queue.size());
    }

    /**
     * Method name is self explanatory
     * 
     * @throws InterruptedException
     */
    @Test(timeout = 15000)
    public void testFixedRate() throws InterruptedException {
        System.out.println("Test FixedRate");

        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();

        TestRunnableFuture trf = new TestRunnableFuture("testPeriodicTimer", queue, 1500);

        Future<?> future = threadPool.scheduleAtFixedRate(trf, 0, 2000);

        Awaitility.await().until(() -> queue.size() >= 5);

        future.cancel(false);

        try {
            future.get();
        } catch (CancellationException ce) {
            // this is expected
        } catch (ExecutionException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        assertEquals(5, queue.size());
    }

}
