package com.winterwell.utils.threads;

import com.winterwell.utils.Constant;

/**
 * For when the future really is forseeable.
 * TODO merge with {@link Constant}
 */
public final class FixedFuture<V> extends AFuture<V> implements IFuture<V> {
	
	private final V _v;

	public FixedFuture(V v) {
		this._v = v;
		resolve();
	}
	
	@Override
	public String toString() {
		return get()==null? "" : String.valueOf(get());
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public V get2() throws RuntimeException {
		return _v;
	}
	
}