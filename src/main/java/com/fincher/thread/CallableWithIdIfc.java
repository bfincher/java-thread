package com.fincher.thread;

import java.util.concurrent.Callable;

/** A Java Callable with an added identifier.
 * 
 * @author Brian Fincher
 *
 * @param <T> see Callable
 */
public interface CallableWithIdIfc<T> extends Callable<T>, IdentifiableIfc {

}
