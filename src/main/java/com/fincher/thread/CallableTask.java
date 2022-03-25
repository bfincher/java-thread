package com.fincher.thread;

import java.util.concurrent.Callable;

/**
 * A callable task.
 * 
 * @author Brian Fincher
 *
 */
public interface CallableTask<T> extends Callable<T>, Task {
}
