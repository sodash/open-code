package com.winterwell.utils.threads;

import java.util.concurrent.Callable;

/**
 * {@link Callable} but with unchecked exceptions, so you dont need try-catches.
 * @author daniel
 *
 * @param <V>
 */
public interface ICallable<V> extends Callable<V> {

	@Override
	V call() throws RuntimeException;
	
}
