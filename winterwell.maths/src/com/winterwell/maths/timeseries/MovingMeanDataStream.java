package com.winterwell.maths.timeseries;

import java.util.LinkedList;
import java.util.NoSuchElementException;

import com.winterwell.maths.stats.distributions.MeanVar;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

/**
 * MovingMeanDataStream -- experimental moving mean class
 * 
 * Uses a circular buffer, with time rather than number of elements as the
 * radius. Does not apply a window function (i.e. boxcar mean) and all values are
 * treated equally regardless of spacing in time (i.e. no weighting of any
 * sort).
 * 
 * @author alisdair@winterwell.com
 * 
 */

public class MovingMeanDataStream extends ADataStream {

	class MovingMeanDataStreamIterator extends AbstractIterator<Datum> {
		private AbstractIterator<Datum> base_iterator;
		// this is the buffer that is used to generate the moving mean
		// it is always centred in time on the current entry in the base
		// DataStream
		private LinkedList<Datum> buffer;
		private int current_datum_index; // index of current datum in buffer
		private Dt radius;

		public MovingMeanDataStreamIterator() {
			this.base_iterator = base_stream.iterator();
			this.radius = window.multiply(0.5);
			this.current_datum_index = 0;
			this.buffer = new LinkedList<Datum>();
			if (base_iterator.hasNext()) { this.buffer.add(base_iterator.next()); }
		}

		@Override
		protected Datum next2() throws Exception {
			// It's possible that the next Datum is not in the window.
			// In this case, grab from the base_iterator. It should *always*
			// be the next item, so only check for this specific case
			// (anything else will correctly raise IndexOutOfBoundsException)
			if (this.current_datum_index == this.buffer.size()) {
				// If base iterator is exhausted, and we're past the end of the
				// buffer, we're done
				try {
					buffer.addLast(this.base_iterator.next());
				} catch (NoSuchElementException e) {
					return null;
				}
			}

			Datum current_datum = this.buffer.get(this.current_datum_index);

			Time window_start = null;
			Time window_end = null;
			if (trailing) {
				window_start = current_datum.time.minus(this.radius.multiply(2.0));
				window_end = current_datum.time.plus(new Dt(1,TUnit.MILLISECOND)); // make sure current is included ....
			} else {
				window_start = current_datum.time.minus(this.radius);
				window_end = current_datum.time.plus(this.radius);
			}

			// Now knock anything before window_start off the beginning of the
			// buffer
			while (buffer.getFirst().time.isBefore(window_start)) {
				buffer.removeFirst();
				current_datum_index--;
			}

			// Add data, until the next Datum would be after window_end
			while (this.base_iterator.peekNext() != null
					&& this.base_iterator.peekNext().time.isBefore(window_end)) {
				try {
					buffer.addLast(this.base_iterator.next());
				} catch (NoSuchElementException e) {
					; // intentional! this method should only return null when
						// we are at the end of the base iterator *and* the
						// buffer (see above)
				}
			}
			// Now return the mean -- using MeanVar as backend, but may want to
			// change this for speed
			MeanVar mean = new MeanVar(buffer.get(0).getDim());
			for (Datum datum : buffer) {
				mean.train1(DataUtils.newVector(datum.getData()));
			}
			this.current_datum_index++;
			return new Datum(current_datum.time, mean.getMean(), null);
		}
	}

	private static final long serialVersionUID = 1L;
	private IDataStream base_stream;
	private Dt window;
	private Boolean trailing;

	/**
	 * Create a moving mean centred on the current datum, with the given time radius
	 * @param _base_stream input stream
	 * @param _window time radius
	 */
	public MovingMeanDataStream(IDataStream _base_stream, Dt _window) {
		super(_base_stream.getDim());
		this.base_stream = _base_stream;
		this.window = _window;
		this.trailing = false;
	}

	/**
	 * Create a moving mean centred on or trailing from the current datum, with the
	 * given time radius
	 * @param _base_stream input stream
	 * @param _window time radius
	 * @param _trailing true if the window trails from the current datum (i.e. current
	 * is the latest element in the window), false if it is centred on the current datum.
	 */
	public MovingMeanDataStream(IDataStream _base_stream, Dt _window, Boolean _trailing) {
		super(_base_stream.getDim());
		this.base_stream = _base_stream;
		this.window = _window;
		this.trailing = _trailing;
	}

	@Override
	public AbstractIterator<Datum> iterator() {
		return new MovingMeanDataStreamIterator();
	}

}
