package com.winterwell.maths.timeseries;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

import junit.framework.TestCase;

public class MovingMeanDataStreamTest extends TestCase {

	private static final double eps = 2e-15;

	private ListDataStream exampleElementList() {
		double[] data = { 0, 3, 1, 23.5, 2, -14, 3, 0, 4, 8.1, 7.5, 12, 20, 1,
				21, 2, 22, 3 };
		List<Datum> data_list = new ArrayList<Datum>();
		for (int i = 0; i < data.length; i += 2) {
			data_list
					.add(new Datum(new Time(1000 * data[i]), data[i + 1], null));
		}
		return new ListDataStream(data_list);
	}

	private IDataStream tenSecondUpDown() {
		FunctionDataStream up_down = new FunctionDataStream(new Dt(1,
				TUnit.SECOND)) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected double f(double x) {
				// Return 1 for odd and 0 for even
				return ((int) Math.floor(x)) % 2;
			}
		};
		up_down.setStart(new Time(0L));
		up_down.setEnd(new Time(10000L)); // i.e. 10s in milliseconds

		IDataStream up_down_3d = new FilteredDataStream(up_down, 3) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected Datum filter(Datum datum) throws EnoughAlreadyException {
				double[] _data_out = { datum.get(0), datum.get(0), datum.get(0) };
				return new Datum(datum.time, _data_out, datum.getLabel());
			}
		};
		return up_down_3d;
	}

	public void testExceptionAtEnd() {
		IDataStream example_list = this.exampleElementList();
		MovingMeanDataStream mm = new MovingMeanDataStream(example_list,
				new Dt(3, TUnit.SECOND));
		AbstractIterator<Datum> iter = mm.iterator();
		for (int i = 0; i < 9; i++) {
			iter.next();
		}
		// Check it throws NoSuchElementException at the end
		boolean threw = false;
		try {
			iter.next();
		} catch (Exception e) {
			assertTrue(
					"Exception thrown at iterator end should be NoSuchElementException",
					e instanceof NoSuchElementException);
			threw = true;
		}
		assertTrue(
				"Iterating after the end of the list should throw NoSuchElementException",
				threw);
	}

	public void testSimple() {
		// Set window equal to value spacing
		MovingMeanDataStream mm = new MovingMeanDataStream(
				this.tenSecondUpDown(), new Dt(1, TUnit.SECOND));
		AbstractIterator<Datum> iter = mm.iterator();
		// FunctionDataStream adds its delta before first return, so ....
		assertEquals(1.0, iter.next().get(0), eps); // series starts at 1,
													// should be odd and return
													// 1
		assertEquals(0.0, iter.next().get(0), eps); // & next one should be even
	}

	public void testSimple2() {
		MovingMeanDataStream mm = new MovingMeanDataStream(
				this.exampleElementList(), new Dt(3, TUnit.SECOND));
		AbstractIterator<Datum> iter = mm.iterator();

		assertEquals((3 + 23.5) / 2, iter.next().get(0), eps); // t = 0
		assertEquals((3 + 23.5 - 14) / 3, iter.next().get(0), eps); // t = 1
		assertEquals((23.5 - 14) / 3, iter.next().get(0), eps); // t = 2
		assertEquals((8.1 - 14) / 3, iter.next().get(0), eps); // t = 3
		assertEquals(8.1 / 2, iter.next().get(0), eps); // t = 4
		assertEquals(12.0, iter.next().get(0)); // t = 7.5
		assertEquals(1.5, iter.next().get(0)); // t = 20
		assertEquals(2.0, iter.next().get(0)); // t = 21
		assertEquals(2.5, iter.next().get(0)); // t = 22
	}
}
