package com.winterwell.maths.timeseries;

import java.util.List;

import com.winterwell.maths.stats.distributions.d1.Constant1D;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

import junit.framework.TestCase;

/**
 * 
 * 
 * 
 * @tests {@link MixedDataStream}
 * 
 * @author daniel
 */
public class MixedDataStreamTest extends TestCase {

	public void testSimple() {
		ADataStream streamA = new RandomDataStream(new Constant1D(2),
				new Time(), new Dt(1, TUnit.HOUR));
		ADataStream streamB = new RandomDataStream(new Constant1D(4),
				new Time(), new Dt(1, TUnit.HOUR));
		streamA = new FilteredDataStream(streamA) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected Datum filter(Datum d) {
				d.setModifiable(true);
				d.setLabel("A");
				return d;
			}
		};
		streamB = new FilteredDataStream(streamB) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected Datum filter(Datum d) {
				d.setModifiable(true);
				d.setLabel("B");
				return d;
			}
		};

		IDataStream mixed = new MixedDataStream(streamA, streamB);

		List<Datum> data = DataUtils.toList(mixed, 20);
		for (int i = 0; i < data.size(); i++) {
			Datum d = data.get(i);
			if (i % 2 == 0) {
				assert d.getLabel() == "A" : d;
			} else {
				assert d.getLabel() == "B" : d;
			}
		}
	}
}
