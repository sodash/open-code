package com.winterwell.maths.timeseries;

import java.util.NoSuchElementException;

import com.winterwell.utils.containers.AbstractIterator;

import no.uib.cipr.matrix.Vector;

/**
 * DeltaDataStream -- returns the delta between the current element in the
 * data stream and the last one. Always returns zero for the first element
 * (this means that the size of the data stream is the same as base)
 * 
 * At present takes no account of timestamps, so this isn't really a gradient
 * (would be easy to fix that, if needed).
 * 
 * Nulls out all labels.
 * 
 * @author alisdair@winterwell.com
 *
 */
// Minor TODO refactor onto FilteredDataStream
public class DeltaDataStream extends ADataStream {

	private static final long serialVersionUID = 1L;

	private IDataStream base_stream;
	
	public DeltaDataStream(IDataStream _base_stream) {
		super(_base_stream.getDim());
		this.base_stream = _base_stream;
	}
	
	class DeltaDataStreamIterator extends AbstractIterator<Datum> {
		private AbstractIterator<Datum> base;
		private Datum last;
		
		public DeltaDataStreamIterator(AbstractIterator<Datum> _base_iterator) {
			this.base = _base_iterator;
			this.last = null;
		}
		
		@Override
		protected Datum next2() throws Exception {
			try {
				if (this.last == null) {
					this.last = this.base.next();
					return new Datum(
						this.last.time,
						DataUtils.filledVector(this.last.getDim(), 0.0),
						null
					);
				} else {
					Datum current = this.base.next();
					Vector last_copy = this.last.copy();
					this.last = new Datum(current);
					return new Datum(
						current.time,
						current.copy().add(last_copy.scale(-1)),
						null
					);
				}
			} catch (NoSuchElementException e) {
				return null;
			}
		}
	}
	
	@Override
	public AbstractIterator<Datum> iterator() {
		return new DeltaDataStreamIterator(this.base_stream.iterator());
	}

}
