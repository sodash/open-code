package com.winterwell.maths.timeseries;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.AbstractIterator;

/**
 * Status: possibly broken. Is it wanted??
 * 
 * The problem with a pull mechanism is when several people want to pull from
 * it. This class shares a stream between several pullers, using a buffer to
 * preserve state. That can be expensive in terms of memory if the pullers get
 * majorly out of sync.
 * 
 * @testedby {@link SharedDataStreamTest}
 * 
 * @author daniel
 */
public final class SharedDataStream {

	class SStream extends AbstractIterator<Datum> {

		private int i;

		public SStream(int i) {
			this.i = i;
		}

		@Override
		public Datum next2() {
			synchronized (SharedDataStream.this) {
				Datum n = peekNext2(i);
				if (n == null)
					throw new NoSuchElementException();
				advance(i);
				return n;
			}
		}

	}

	final List<Datum> backlog = new ArrayList<Datum>();
	private final AbstractIterator<Datum> base;

	/**
	 * How far behind is each stream?
	 */
	private final int[] behind;

	/**
	 * Share the base stream between n readers.
	 * 
	 * @param base
	 * @param shares
	 */
	public SharedDataStream(AbstractIterator<Datum> base, int shares) {
		assert base != null && shares > 0;
		this.base = base;
		behind = new int[shares];
	}

	synchronized void advance(int i) {
		behind[i]--;
		// drop some history from the front?
		int b = behind[i];
		if (b == backlog.size() - 1) {
			getNext2_dropHistory(i);
		}
	}

	/**
	 * pull a new Datum from the base
	 * 
	 * @return base datum
	 */
	private void getNext2_advance() {
		Datum d = base.next();
		backlog.add(d);
		for (int i = 0; i < behind.length; i++) {
			behind[i]++;
		}
		assert MathUtils.max(behind) == backlog.size();
		// return d;
	}

	/**
	 * Drop the 0th backlog if there are no streams at full backlog.
	 * 
	 * @param i
	 */
	private void getNext2_dropHistory(int i) {
		assert behind[i] == backlog.size() - 1 : behind[i] + " vs " + backlog;
		int mb = MathUtils.max(behind);
		if (mb != backlog.size()) {
			backlog.remove(0);
			assert mb == backlog.size();
		}
	}

	/**
	 * Get the ith copy of this shared stream. Can be called multiple times with
	 * the same i.
	 * 
	 * @param i
	 * @return
	 */
	public AbstractIterator<Datum> getStream(final int i) {
		if (i < 0 || i >= behind.length)
			throw new IndexOutOfBoundsException();
		return new SStream(i);
	}

	synchronized Datum peekNext2(int i) {
		int b = behind[i];
		if (b == 0) {
			getNext2_advance();
			b = behind[i];
		}
		Datum d = backlog.get(backlog.size() - b);
		return d;
	}

}
