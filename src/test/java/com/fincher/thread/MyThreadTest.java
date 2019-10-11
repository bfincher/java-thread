package com.fincher.thread;

import org.junit.Assert;
import org.junit.Test;

/** Tests My Threads */
public class MyThreadTest {

    /**
     * Method name is self explanatory
     * 
     * @throws InterruptedException
     */
    @Test
    public void testThreadWithExceptionContinueExecution() throws InterruptedException {
        MyThreadIfc thread = new MyThread("TestThread", new MyRunnableIfc() {

            @Override
            public void run() {
                try {
                    System.out.println("Thread running");
                    Thread.sleep(500);
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

        thread.start();

        Thread.sleep(2000);

        if (thread.isTerminated()) {
            Assert.fail("Thread did not continue after Exception");
        }

        thread.terminate();
    }

    /**
     * Method name is self explanatory
     * 
     * @throws InterruptedException
     */
    @Test
    public void testThreadWithExceptionStopExecution() throws InterruptedException {

        ThreadGroup myThreadGroup = new ThreadGroup("MyThreadGroup");

        MyThreadIfc thread = new MyThread(myThreadGroup, "TestThread", new MyRunnableIfc() {

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

        thread.setContinueAfterException(false);
        thread.setExceptionHandler(new ExceptionHandlerIfc() {

            @Override
            public void onException(Throwable t) {
                t.printStackTrace();

            }
        });

        thread.start();

        thread.join(1000);

        if (!thread.isTerminated()) {
            Assert.fail("Thread continued after Exception");
        }
    }

    /**
     * Method name is self explanatory
     * 
     */
    @Test
    public void testThread() {
        MyThreadIfc thread = new MyThread("TestThread", new MyRunnableIfc() {

            private int count = 0;

            @Override
            public void run() {
                try {
                    System.out.println("Thread running");
                    count++;
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public boolean continueExecution() {
                return count <= 10;
            }

            @Override
            public void terminate() {
            }
        });

        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Thread terminated");

        thread.terminate();
    }

}
