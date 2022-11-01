package com.fincher.thread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class ThreadPoolTest {

    private static class TestRunnable implements Runnable {

        public static final int NUM_ITERATIONS = 10;

        private String id;

        private final BlockingQueue<String> queue;

        public TestRunnable(String id, BlockingQueue<String> queue) {
            this.queue = queue;
            this.id = id;
        }

        @Override
        public void run() {
            for (int i = 0; i < NUM_ITERATIONS; i++) {
                String str = id + " iteration " + i;
                System.out.println(str);
                queue.add(str);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private static class TestEventRunnable implements Runnable {

        private String id;

        private final BlockingQueue<String> queue;

        private final int numIterations;

        public TestEventRunnable(String id, BlockingQueue<String> queue, int numIterations) {
            this.queue = queue;
            this.id = id;
            this.numIterations = numIterations;
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

    private static class TestEventCallable implements Callable<Boolean> {

        private String id;

        private final BlockingQueue<String> queue;

        private final int numIterations;

        public TestEventCallable(String id, BlockingQueue<String> queue, int numIterations) {
            this.queue = queue;
            this.id = id;
            this.numIterations = numIterations;
        }

        @Override
        public Boolean call() {
            for (int i = 0; i < numIterations; i++) {
                String str = id + " iteration " + i;
                System.out.println(str);
                queue.add(str);
            }
            return Boolean.TRUE;
        }
    }

    private static class TestEventRunnableFuture implements Runnable {

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
        public void run() {
            System.out.println("adding to queue");
            queue.add(id + " executing " + System.currentTimeMillis());
            if (sleepTime > 0) {
                Awaitility.await().atLeast(Duration.ofMillis(sleepTime));
            }
        }
    }

    private void runTestWithXThreads(int numThreads) throws InterruptedException, ExecutionException {
        ThreadPool threadPool = new ThreadPool(numThreads);

        int numSubmissions = numThreads * 2;

        ArrayList<Future<?>> futures = new ArrayList<Future<?>>(numSubmissions);
        ArrayList<BlockingQueue<String>> queueList = new ArrayList<BlockingQueue<String>>(numSubmissions);

        for (int i = 0; i < numThreads * 2; i++) {
            LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
            queueList.add(queue);
            futures.add(threadPool.submit(new TestRunnable(String.valueOf(i), queue)));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        for (BlockingQueue<String> queue : queueList) {
            assertEquals(TestRunnable.NUM_ITERATIONS, queue.size());
        }
        threadPool.shutdown();
    }

    @Test
    public void testOneThread() throws Exception {
        System.out.println("Testing with one thread");
        runTestWithXThreads(1);
    }

    @Test
    public void testTwoThread() throws Exception {
        System.out.println("Testing with two threads");
        runTestWithXThreads(2);
    }

    @Test
    @Timeout(value = 5)
    public void testTimer() throws InterruptedException, ExecutionException {
        ThreadPool threadPool = null;
        try {
            threadPool = new ThreadPool(10);
            System.out.println("testTimer");
            final long startTime = System.currentTimeMillis();
            System.out.println("Scheduling...");

            BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
            ScheduledFuture<?> future = threadPool.schedule(new TestEventRunnable("testTimer", queue, 10),
                    Duration.ofMillis(500));

            System.out.println(future.get());

            Awaitility.await().until(() -> queue.size() == 10);

            long endTime = System.currentTimeMillis();

            long delta = endTime - startTime;
            System.out.println("Finished after " + delta + " millis");
        } finally {
            threadPool.shutdown();
        }
    }

    @Test
    public void testCancelTimer() throws InterruptedException {
        System.out.println("testCancelTimer");

        ThreadPool threadPool = null;
        try {
            threadPool = new ThreadPool(10);

            BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
            ScheduledFuture<?> future = threadPool.schedule(new TestEventCallable("testTimer", queue, 10),
                    Duration.ofSeconds(1000));

            future.cancel(false);

            Awaitility.await().atLeast(Duration.ofSeconds(2));

            assertTrue(queue.isEmpty());
        } finally {
            threadPool.shutdown();
        }
    }

    @Test
    public void testFixedDelay() throws InterruptedException, ExecutionException {
        ThreadPool threadPool = new ThreadPool(10);
        try {
            System.out.println("testFixedDelay");

            LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
            Future<?> future = threadPool.scheduleWithFixedDelay(
                    new TestEventRunnableFuture("testPeriodicTimer", queue), Duration.ofMillis(25),
                    Duration.ofMillis(50));

            Thread.sleep(100);
            future.cancel(false);

            try {
                future.get();
            } catch (CancellationException ce) {
                // this is expected
            }

            Awaitility.await().atMost(Duration.ofSeconds(2)).until(() -> {
                System.out.println(queue.size());
                return queue.size() >= 2;
            });
        } finally {
            threadPool.shutdown();
        }
    }

    @Test // (timeout = 15000)
    public void testFixedRate() throws InterruptedException, ExecutionException {
        System.out.println("Test FixedRate");

        ThreadPool threadPool = null;
        try {
            threadPool = new ThreadPool(10);

            LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();

            TestEventRunnableFuture trf = new TestEventRunnableFuture("testPeriodicTimer", queue, 150);

            Future<?> future = threadPool.scheduleAtFixedRate(trf, Duration.ZERO, Duration.ofMillis(200));

            Awaitility.await().until(() -> queue.size() >= 5);

            future.cancel(false);

            try {
                future.get();
            } catch (CancellationException ce) {
                // this is expected
            }

            assertEquals(5, queue.size());
        } finally {
            threadPool.shutdown();
        }
    }

    @Test
    public void testSetThreadFactory() {
        ThreadPool threadPool = new ThreadPool(1, (r, e) -> {
        });

        assertThrows(UnsupportedOperationException.class, () -> threadPool.setThreadFactory(null));
    }
}
