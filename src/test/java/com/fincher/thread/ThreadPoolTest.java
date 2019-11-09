package com.fincher.thread;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;

/** A JUnit tester for the thread pool */
public class ThreadPoolTest {

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
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private static class TestEventRunnable implements RunnableWithIdIfc {

        private String id;

        private final BlockingQueue<String> queue;

        private final int numIterations;

        public TestEventRunnable(String id, BlockingQueue<String> queue, int numIterations) {
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

    private static class TestEventRunnableFuture implements RunnableWithIdIfc {

        private String id;

        private final BlockingQueue<String> queue;

        private final int sleepTime;

        public TestEventRunnableFuture(String id, BlockingQueue<String> queue) {
            this.id = id;
            this.queue = queue;
            sleepTime = 0;
        }

        public TestEventRunnableFuture(String id, BlockingQueue<String> queue, int sleepTime) {
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

    private void runTestWithXThreads(int numThreads) throws InterruptedException {
        ThreadPool threadPool = new ThreadPool(numThreads);

        int numSubmissions = numThreads * 2;

        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(numSubmissions);
        ArrayList<BlockingQueue<String>> queueList = new ArrayList<BlockingQueue<String>>(
                numSubmissions);

        for (int i = 0; i < numThreads * 2; i++) {
            LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
            queueList.add(queue);
            futures.add(threadPool.submit(new TestRunnable(String.valueOf(i), queue)));
        }

        try {
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        for (BlockingQueue<String> queue : queueList) {
            if (queue.size() != TestRunnable.NUM_ITERATIONS) {
                Assert.fail("size = " + queue.size());
            }
        }
        threadPool.shutdown();
    }

    /**
     * Method name is self explanatory
     * 
     * @throws InterruptedException
     */
    @Test
    public void testOneThread() throws InterruptedException {
        System.out.println("Testing with one thread");
        runTestWithXThreads(1);
    }

    /**
     * Method name is self explanatory
     * 
     * @throws InterruptedException
     */
    @Test
    public void testTwoThread() throws InterruptedException {
        System.out.println("Testing with two threads");
        runTestWithXThreads(2);
    }

    /**
     * Method name is self explanatory
     * 
     * @throws InterruptedException
     */
    @Test(timeout = 5000)
    public void testTimer() throws InterruptedException {
        ThreadPool threadPool = null;
        try {
            threadPool = new ThreadPool(10);
            System.out.println("testTimer");
            final long startTime = System.currentTimeMillis();
            System.out.println("Scheduling...");

            BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
            ScheduledFuture<?> future = threadPool.schedule(
                    new TestEventRunnable("testTimer", queue, 10), 500, TimeUnit.MILLISECONDS);

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
        } finally {
            threadPool.shutdown();
        }
    }

    /**
     * Test a timer that is cancelled before it is executed
     * 
     * @throws InterruptedException
     */
    @Test
    public void testCancelTimer() throws InterruptedException {
        System.out.println("testCancelTimer");

        ThreadPool threadPool = null;
        try {
            threadPool = new ThreadPool(10);

            BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
            ScheduledFuture<?> future = threadPool.schedule(
                    new TestEventRunnable("testTimer", queue, 10), 1000, TimeUnit.MILLISECONDS);

            future.cancel(false);

            Awaitility.await().atLeast(2, TimeUnit.SECONDS);

            assertTrue(queue.isEmpty());
        } finally {
            threadPool.shutdown();
        }
    }

    /**
     * Method name is self explanatory
     * 
     * @throws InterruptedException
     */
    @Test
    public void testFixedDelay() throws InterruptedException {
        ThreadPool threadPool = null;
        try {
            threadPool = new ThreadPool(10);

            System.out.println("testFixedDelay");

            System.out.println("Scheduling at " + System.currentTimeMillis());
            LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
            Future<?> future = threadPool.scheduleWithFixedDelay(
                    new TestEventRunnableFuture("testPeriodicTimer", queue), 500, 1000,
                    TimeUnit.MILLISECONDS);

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
        } finally {
            threadPool.shutdown();
        }
    }

    /**
     * Method name is self explanatory
     * 
     * @throws InterruptedException
     */
    @Test(timeout = 15000)
    public void testFixedRate() throws InterruptedException {
        System.out.println("Test FixedRate");

        ThreadPool threadPool = null;
        try {
            threadPool = new ThreadPool(10);

            LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();

            TestEventRunnableFuture trf = new TestEventRunnableFuture("testPeriodicTimer", queue,
                    1500);

            Future<?> future = threadPool.scheduleAtFixedRate(trf, 0, 2000, TimeUnit.MILLISECONDS);

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
        } finally {
            threadPool.shutdown();
        }
    }
}
