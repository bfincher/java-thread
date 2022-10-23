package com.fincher.thread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

public class LongLivedTaskTest {

    @Test
    public void testThreadWithExceptionContinueExecutionWithRunnable() throws InterruptedException {

        RunnableTask runnable = new RunnableTask() {
            @Override
            public void run() {
                try {
                    System.out.println("Thread running");
                    Thread.sleep(50);
                    throw new Error("Test Exception");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public boolean continueExecution() {
                return true;
            }

            @Override
            public void terminate() {
            }

        };

        LongLivedTask<Void> task = LongLivedTask.create("TestThread", runnable);
        Future<Void> future = task.start();
        Thread.sleep(200);
        assertFalse(future.isCancelled(), "Thread did not continue after Exception");

        future.cancel(true);
    }

    @Test
    public void testThreadWithExceptionContinueExecutionWithCallable() throws InterruptedException {

        CallableTask<Boolean> callable = new CallableTask<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    System.out.println("Thread running");
                    Thread.sleep(50);
                    throw new Error("Test Exception");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return true;

            }

            @Override
            public boolean continueExecution() {
                return true;
            }

            @Override
            public void terminate() {
            }
        };

        LongLivedTask<Boolean> task = LongLivedTask.create("TestThread", callable);
        Future<Boolean> future = task.start();
        Thread.sleep(200);
        assertFalse(future.isCancelled(), "Thread did not continue after Exception");

        future.cancel(true);
    }

    @Test
    public void testWithExceptionStopExecution() throws InterruptedException, ExecutionException, TimeoutException {

        LongLivedTask<?> task = LongLivedTask.create("TestThread", new RunnableTask() {

            @Override
            public void run() {
                try {
                    System.out.println("Thread running");
                    Thread.sleep(100);
                    throw new Error("Test Exception");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public boolean continueExecution() {
                return true;
            }

            @Override
            public void terminate() {
            }
        });

        task.setContinueAfterException(false);
        task.setExceptionHandler(e -> e.printStackTrace());

        Future<?> future = task.start();

        assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.SECONDS));

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
    }

    @Test
    public void testWithRunnable() throws InterruptedException, ExecutionException {
        LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        testThread(new TestRunnable(queue), null, queue);
    }

    @Test
    public void testWithCallable() throws InterruptedException, ExecutionException {
        LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        RunnableTask runnable = new TestRunnable(queue);

        CallableTask<Boolean> callable = new CallableTask<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                runnable.run();
                return true;
            }

            @Override
            public void terminate() {
                runnable.terminate();
            }

            @Override
            public boolean continueExecution() {
                return runnable.continueExecution();
            }
        };
        testThread(null, callable, queue);
    }

    private void testThread(RunnableTask runnable, CallableTask<?> callable,
            BlockingQueue<Integer> queue) throws InterruptedException, ExecutionException {

        LongLivedTask<?> task;
        if (runnable != null) {
            task = LongLivedTask.create("TestThread", runnable);
        } else {
            task = LongLivedTask.create("TestThread", callable);
        }

        task.start().get();
        assertEquals(10, queue.size());
        System.out.println("Thread terminated");
    }

    private static class TestRunnable implements RunnableTask {
        private int count = 0;

        final BlockingQueue<Integer> queue;

        public TestRunnable(BlockingQueue<Integer> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                System.out.println("Thread running");
                queue.add(count++);
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        @Override
        public boolean continueExecution() {
            return count < 10;
        }

        @Override
        public void terminate() {
        }
    }

}
