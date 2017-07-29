package com.winterwell.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.winterwell.utils.threads.ICallable;
import com.winterwell.utils.threads.IFuture;

/**
 * Callable/future wrapper for a fixed value.
 * @author daniel
 *
 * @param <V>
 */
public final class Constant<V> implements ICallable<V>, IFuture<V> {
	private final V v;

	public Constant(V value) {
		this.v = value;
	}

	@Override
	public V call() {
		return v;
	}			
	
	@Override
	public int hashCode() {
		return v==null? 31 : v.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Constant other = (Constant) obj;
		if (v == null) {
			if (other.v != null)
				return false;
		} else if ( ! v.equals(other.v))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return v==null? "" : v.toString();
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
	public V get(long timeout, TimeUnit unit) {
		return v;
	}

	@Override
	public V get() throws RuntimeException {
		return v;
	}
}