package com.winterwell.utils.threads;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import winterwell.utils.IFn;
import winterwell.utils.Utils;

/**
 * Identical to {@link Future}, just replaces the checked exception on get()
 * with a runtime one. And implementations should over-ride toString() for easier use in building ops.
 * 
 * @author daniel
 * 
 * @param <V>
 */
public interface IFuture<V> extends Future<V> {
	
	public static interface IDeferred<V> extends IFuture<V> {
		public IFuture.IDeferred<V> fail(IFn<Throwable,Object> failFn);
		public IFuture.IDeferred<V> done(IFn<V,Object> successFn);
		public IFuture.IDeferred<V> always(IFn fn);
	}
	
	/**
	 * Wrap a Future from one of Java's built-in executors as an IFuture
	 * -- so we get nice toString() behaviour.
	 * @author daniel
	 */
	public static final class WrapFuture<V> implements IFuture<V> {
		private final Future<V> f;

		public WrapFuture(Future<V> f) {
			this.f = f;
			assert f != null;
		}
		@Override
		public V get() throws RuntimeException {
			try {
				return f.get();
			} catch (Exception e) {
				throw Utils.runtime(e);
			}
		}
		
		/**
		 * Wait for the Future, then return it's toString
		 */
		@Override
		public String toString() {
			V v = get();
			return v==null? "" : v.toString();
		}
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return f.cancel(mayInterruptIfRunning);
		}
		@Override
		public V get(long timeout, TimeUnit unit) {
			try {
				return f.get(timeout, unit);
			} catch (Exception e) {
				throw Utils.runtime(e);
			}
		}
		
		@Override
		public boolean isCancelled() {
			return f.isCancelled();
		}
		@Override
		public boolean isDone() {
			return f.isDone();
		}
	}
	
	/**
	 * toString() for the value! This allows IFuture to be dropped into e.g.
	 * String-building operations. Should be "" for null values.
	 */
	@Override
	String toString();
	
	@Override
	public V get() throws RuntimeException;
	
}
