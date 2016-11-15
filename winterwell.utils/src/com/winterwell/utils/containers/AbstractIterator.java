/**
 *
 */
package com.winterwell.utils.containers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import winterwell.utils.Utils;

import com.winterwell.utils.threads.IProgress;

/**
 * Helper class for creating iterable things with 1-step lookahead.
 * 
 * Note: Google utils has a very similar class with a couple more
 * safety-features. We might want to switch (but then we get an extra
 * dependency).
 * 
 * @author Daniel
 * 
 */
public abstract class AbstractIterator<T> implements Iterator<T>, IProgress {

	private T next;

	@Override
	public double[] getProgress() {
		return null;
	}

	@Override
	public final boolean hasNext() {
		return peekNext() != null;
	}

	/**
	 * @return true if the next item is already loaded. In this case, a call to
	 *         either {@link #next()} or {@link #peekNext()} will return
	 *         immediately.
	 */
	public final boolean hasNextReady() {
		return next != null;
	}

	@Override
	public final T next() {
		T now = peekNext();
		if (now == null)
			throw new NoSuchElementException();
		next = null;
		return now;
	}

	/**
	 * Retrieve a batch of data. This is not more efficient by default, but
	 * could in principle be if overridden.
	 * 
	 * @param n
	 * @return
	 */
	public List<T> next(int n) {
		ArrayList<T> buffer = new ArrayList<T>(n);
		for (int i = 0; i < n; i++) {
			T x = next();
			if (x == null)
				return buffer;
			buffer.add(x);
		}
		return buffer;
	}

	/**
	 * @return the next item, or null if at the end of sequence. Advances the
	 *         sequence by one.
	 *         <p>
	 *         The only difference between this and {@link #next()} is that it
	 *         can return null. The reason for separate methods is too provide
	 *         next() and peekNext(), by holding the value returned by this
	 *         method.
	 * @throws Exception
	 */
	protected abstract T next2() throws Exception;

	/**
	 * Look at the next value without advancing the iterator
	 * 
	 * @return next value or null if at the end. Warning: the iterator can block
	 *         until it has a next value to return.
	 *         <p>
	 *         Note for those who care about the details: This uses
	 *         {@link #next2()}, and so does advance the underlying state of the
	 *         iterator. But the value is held until a call to #next(), so it
	 *         doesn't appear to advance anything.
	 */
	public final T peekNext() {
		if (next != null)
			return next;
		try {
			next = next2();
			return next;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	@Override
	public void remove() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

}
