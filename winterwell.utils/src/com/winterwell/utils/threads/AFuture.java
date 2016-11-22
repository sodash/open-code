package com.winterwell.utils.threads;


import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.winterwell.utils.TimeOut;

import com.winterwell.utils.IFn;
import com.winterwell.utils.Utils;

import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;

/**
 * Helper class for implementing IFuture / {@link Future}
 * @author daniel
 *
 * @param <V>
 */
public abstract class AFuture<V> implements IFuture<V>, IFuture.IDeferred<V> {

	/**
	 * TODO test and remove this
	 */
	@Override
	protected void finalize() throws Throwable {
		if (thens!=null && ! thens.isEmpty()) {
			Log.e("future", "Un-run deferred: "+thens+" from "+this);
		}
		super.finalize();
	}
	
	/**
	 * Chain of functions to run.
	 * [(true, successFn), (false, failFn), (null, alwaysFn)]
	 */
	private ArrayList<Pair2<Boolean,IFn>> thens;
	
	@Override
	public IFuture.IDeferred<V> fail(IFn<Throwable,Object> failFn) {
		return then(false, failFn);
	}
	private IFuture.IDeferred<V> then(Boolean success, IFn fn) {		
		if (thens==null) thens = new ArrayList(1);
		thens.add(new Pair2<Boolean, IFn>(success, fn));
		// already done?
		if (isDone()) {
			// call now
			resolve();
		}
		return this;
	}
	@Override
	public IFuture.IDeferred<V> done(IFn<V,Object> fn) {
		return then(true, fn);
	}
	@Override
	public IFuture.IDeferred<V> always(IFn fn) {
		return then(null, fn);
	}
	
	/**
	 * Mark as done
	 */
	public final synchronized void resolve() {
		done = true;
		if (thens==null) return;
		Object out = success? v : ex;
		for (Pair2<Boolean, IFn> then : thens) {
			// run fail on fail, success on success
			if (then.first!=null && then.first != success) {
				continue;
			}
			then.second.apply(out);
		}
		// resolve can be called repeatedly -- fns shouldn't be
		thens.clear();
	}
	
	protected volatile boolean done;
	private volatile boolean success;
	private V v;
	private Throwable ex;

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		TimeOut to = new TimeOut(new Dt(timeout, unit));
		try {
			V v = get();
			return v;
		} finally {
			to.cancel();
		}
	}
	
	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	
	@Override
	public final V get() throws RuntimeException {
		try {
			v = get2();
			success = true;
			return v;
		} catch(Throwable ex) {
			success = false;
			this.ex = ex;
			throw Utils.runtime(ex);
		} finally {
			resolve();
		}		
	}
	
	/**
	 * Waits if necessary for the computation to complete, and then retrieves its result.
	 * @return the value, once known
	 */
	protected abstract V get2() throws Exception;
	
	@Override
	public String toString() {
		return isDone()? String.valueOf(v) : "Future[...]";
	}

}
