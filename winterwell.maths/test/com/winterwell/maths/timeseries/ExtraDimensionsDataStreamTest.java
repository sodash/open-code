package com.winterwell.maths.timeseries;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import com.winterwell.maths.datastorage.DataSet;
import com.winterwell.maths.timeseries.ExtraDimensionsDataStream.KMatchPolicy;
import com.winterwell.maths.vector.XY;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

/**
 * 
 * 
 * 
 * @tests {@link ExtraDimensionsDataStream}
 * @author daniel
 */
public class ExtraDimensionsDataStreamTest {

	@Test
	public void testCombine() {
		CounterDataStream x = new CounterDataStream(new Dt(1, TUnit.HOUR), 1);
		x.setTime(new Time(2000, 1, 1));
		ADataStream original = new ExtraDimensionsDataStream(
				KMatchPolicy.IGNORE_TIMESTAMPS, "A", x, new CounterDataStream(
						new Dt(1, TUnit.HOUR), 10), new CounterDataStream(
						new Dt(1, TUnit.HOUR), 100));
		AbstractIterator<Datum> dit = original.iterator();
		Datum a = dit.next();
		Datum b = dit.next();
		Datum c = dit.next();

		assert Datum.equals(a, new Datum(new Time(2000, 1, 1), new double[] {
				1, 10, 100 }, "A"));
		assert Datum.equals(b, new Datum(new Time(2000, 1, 1, 1, 0, 0),
				new double[] { 2, 11, 101 }, "A"));
		assert Datum.equals(c, new Datum(new Time(2000, 1, 1, 2, 0, 0),
				new double[] { 3, 12, 102 }, "A"));

	}
	
	@Test
	public void testUsePrevious() {
		{ // All in step
			ListDataStream a = new ListDataStream(1);
			ListDataStream b = new ListDataStream(1);
			ListDataStream c = new ListDataStream(1);
			Time start = new Time(1900, 1, 1);
			// 0,1,2,3
			a.add(new Datum(start, 1, null));
			a.add(new Datum(start.plus(1, TUnit.MINUTE), 1.1, null));
			a.add(new Datum(start.plus(2, TUnit.MINUTE), 1.2, null));
			a.add(new Datum(start.plus(3, TUnit.MINUTE), 1.3, null));
			// 0,1,2,3
			b.add(new Datum(start, 2, null));
			b.add(new Datum(start.plus(1, TUnit.MINUTE), 2.1, null));
			b.add(new Datum(start.plus(2, TUnit.MINUTE), 2.2, null));
			b.add(new Datum(start.plus(3, TUnit.MINUTE), 2.3, null));
			// 0,1,2,3
			c.add(new Datum(start, 3, null));
			c.add(new Datum(start.plus(1, TUnit.MINUTE), 3.1, null));
			c.add(new Datum(start.plus(2, TUnit.MINUTE), 3.2, null));
			c.add(new Datum(start.plus(3, TUnit.MINUTE), 3.3, null));
			// a stream
			ExtraDimensionsDataStream stream = new ExtraDimensionsDataStream(
					KMatchPolicy.USE_PREVIOUS_VALUE_ON_MISMATCH, Arrays.asList(a, b, c));
			AbstractIterator<Datum> dit = stream.iterator();
			Datum first = dit.peekNext();
			System.out.println(first);
			Datum d1 = dit.next();
			Datum d2 = dit.next();
			assert DataUtils.equals(d2, 1.1, 2.1, 3.1);
			System.out.println(stream.list());
		}
		
		{ // Mis-stepping
			ListDataStream a = new ListDataStream(1);
			ListDataStream b = new ListDataStream(1);
			ListDataStream c = new ListDataStream(1);
			Time start = new Time(1900, 1, 1);
			// 0,1,2,3
			a.add(new Datum(start, 1, null));
			a.add(new Datum(start.plus(1, TUnit.MINUTE), 1.1, null));
			a.add(new Datum(start.plus(2, TUnit.MINUTE), 1.2, null));
			a.add(new Datum(start.plus(3, TUnit.MINUTE), 1.3, null));
			// 0,1
			b.add(new Datum(start, 2, null));
			b.add(new Datum(start.plus(1, TUnit.MINUTE), 2.1, null));
//			b.add(new Datum(start.plus(2, TUnit.MINUTE), 2.2, null));
//			b.add(new Datum(start.plus(3, TUnit.MINUTE), 2.3, null));
			// 0, 3,4
			c.add(new Datum(start, 3, null));
//			c.add(new Datum(start.plus(1, TUnit.MINUTE), 3.1, null));
			c.add(new Datum(start.plus(3, TUnit.MINUTE), 3.2, null));
			c.add(new Datum(start.plus(4, TUnit.MINUTE), 3.3, null));
			// a stream
			ExtraDimensionsDataStream stream = new ExtraDimensionsDataStream(
					KMatchPolicy.USE_PREVIOUS_VALUE_ON_MISMATCH, Arrays.asList(a, b, c));
			AbstractIterator<Datum> dit = stream.iterator();
			Datum first = dit.peekNext();
			System.out.println(first);
			Datum d1 = dit.next();
			System.out.println(d1);
			Datum d2 = dit.next();
			System.out.println(d2);
			assert DataUtils.equals(d2, 1.1, 2.1, 3);
			System.out.println(stream.list());
		}
	}

	@Test
	public void testDiscardOnMismatch() {
		{ // no matched items
			ListDataStream a = new ListDataStream(1);
			ListDataStream b = new ListDataStream(1);
			ListDataStream c = new ListDataStream(1);
			Time start = new Time(1900, 1, 1);
			// 0,1,2,3
			a.add(new Datum(start, 1, null));
			a.add(new Datum(start.plus(1, TUnit.MINUTE), 1.1, null));
			a.add(new Datum(start.plus(2, TUnit.MINUTE), 1.2, null));
			a.add(new Datum(start.plus(3, TUnit.MINUTE), 1.3, null));
			// 0,2,2,4
			b.add(new Datum(start, 2, null));
			b.add(new Datum(start.plus(2, TUnit.MINUTE), 2.1, null));
			b.add(new Datum(start.plus(2, TUnit.MINUTE), 2.2, null));
			b.add(new Datum(start.plus(4, TUnit.MINUTE), 2.3, null));
			// 1,1,3,3
			c.add(new Datum(start.plus(1, TUnit.MINUTE), 3, null));
			c.add(new Datum(start.plus(1, TUnit.MINUTE), 3.1, null));
			c.add(new Datum(start.plus(3, TUnit.MINUTE), 3.2, null));
			c.add(new Datum(start.plus(3, TUnit.MINUTE), 3.3, null));
			// a stream
			ExtraDimensionsDataStream stream = new ExtraDimensionsDataStream(
					KMatchPolicy.DISCARD_ON_MISMATCH, Arrays.asList(a, b, c));
			AbstractIterator<Datum> dit = stream.iterator();
			boolean hn = dit.hasNext();
			Datum peek = dit.peekNext();
			assert peek == null : peek;
			assert !hn;
		}
	}

	@Test
	public void testDiscardOnMismatch2() {
		{ // some matched items
			ListDataStream a = new ListDataStream(1);
			ListDataStream b = new ListDataStream(1);
			ListDataStream c = new ListDataStream(1);
			Time start = new Time(1900, 1, 1);
			// 0,1,2,3,4,5
			a.add(new Datum(start, 1.0, null));
			a.add(new Datum(start.plus(1, TUnit.MINUTE), 1.1, null));
			a.add(new Datum(start.plus(2, TUnit.MINUTE), 1.2, null));
			a.add(new Datum(start.plus(3, TUnit.MINUTE), 1.3, null));
			a.add(new Datum(start.plus(4, TUnit.MINUTE), 1.4, null));
			a.add(new Datum(start.plus(5, TUnit.MINUTE), 1.5, null));
			// 0,2,2,3,4,6
			b.add(new Datum(start, 2.0, null));
			b.add(new Datum(start.plus(2, TUnit.MINUTE), 2.2, null));
			b.add(new Datum(start.plus(2, TUnit.MINUTE), 2.2, null));
			b.add(new Datum(start.plus(3, TUnit.MINUTE), 2.3, null));
			b.add(new Datum(start.plus(4, TUnit.MINUTE), 2.4, null));
			b.add(new Datum(start.plus(6, TUnit.MINUTE), 2.6, null));
			// 1,1,3,3,4,5
			c.add(new Datum(start.plus(1, TUnit.MINUTE), 3.1, null));
			c.add(new Datum(start.plus(1, TUnit.MINUTE), 3.1, null));
			c.add(new Datum(start.plus(3, TUnit.MINUTE), 3.3, null));
			c.add(new Datum(start.plus(3, TUnit.MINUTE), 3.3, null));
			c.add(new Datum(start.plus(4, TUnit.MINUTE), 3.4, null));
			c.add(new Datum(start.plus(5, TUnit.MINUTE), 3.5, null));
			// a stream
			ExtraDimensionsDataStream stream = new ExtraDimensionsDataStream(
					KMatchPolicy.DISCARD_ON_MISMATCH, Arrays.asList(a, b, c));
			ArrayList<Datum> data = DataUtils.toList(stream, -1);
			assert data.size() == 2 : data;
			Datum one = new Datum(start.plus(3, TUnit.MINUTE), new double[] {
					1.3, 2.3, 3.3 }, null);
			assert Datum.equals(data.get(0), one) : data;
			Datum two = new Datum(start.plus(4, TUnit.MINUTE), new double[] {
					1.4, 2.4, 3.4 }, null);
			assert Datum.equals(data.get(1), two) : data;
		}
	}

	@Test
	public void testFromCovar() {
		DataSet dataset = new DataSet("test1", "col-a", "col-b", "col-c");
		ListDataStream data = new ListDataStream(3);
		data.add(new Datum(new double[] { 1, 1, 0 }));
		data.add(new Datum(new double[] { 2, 2, 1 }));
		data.add(new Datum(new double[] { 3, 3, -1 }));
		data.add(new Datum(new double[] { 4, 4, 0 }));
		dataset.setData(data);

		IDataStream x = dataset.getDataStream1D(0);
		IDataStream y = dataset.getDataStream1D(0);
		// Need to handle different time stamps:
		// only take points with identical time stamps
		ExtraDimensionsDataStream xy = new ExtraDimensionsDataStream(
				KMatchPolicy.DISCARD_ON_MISMATCH, Arrays.asList(x, y));
		ArrayList<Datum> xys = DataUtils.toList(xy, -1);

		assert xys.size() == 4 : xys;
		assert DataUtils.equals(xys.get(1), new XY(2, 2));

		IDataStream x2 = dataset.getDataStream1D(0);
		IDataStream y2 = dataset.getDataStream1D(2);
		// Need to handle different time stamps:
		// only take points with identical time stamps
		ExtraDimensionsDataStream xy2 = new ExtraDimensionsDataStream(
				KMatchPolicy.DISCARD_ON_MISMATCH, Arrays.asList(x2, y2));
		ArrayList<Datum> xy2s = DataUtils.toList(xy2, -1);

		assert xy2s.size() == 4;
		assert DataUtils.equals(xy2s.get(1), new XY(2, 1));
	}

	@Test
	public void testLabel() {
		{
			CounterDataStream y = new CounterDataStream(new Dt(1, TUnit.HOUR),
					10);
			y.setTime(new Time(2000, 1, 1));
			y.setLabel("Y");
			ADataStream original = new ExtraDimensionsDataStream(
					KMatchPolicy.IGNORE_TIMESTAMPS, Arrays.asList(
							new CounterDataStream(new Dt(1, TUnit.HOUR), 1),
							(IDataStream) y, new CounterDataStream(new Dt(1,
									TUnit.HOUR), 100)));
			AbstractIterator<Datum> dit = original.iterator();
			Datum a = dit.next();
			Datum b = dit.next();
			Datum c = dit.next();

			assert a.getLabel() == "Y";

			assert Datum.equals(a, new Datum(a.time,
					new double[] { 1, 10, 100 }, "Y"));
			assert Datum.equals(b, new Datum(b.time,
					new double[] { 2, 11, 101 }, "Y"));
			assert Datum.equals(c, new Datum(c.time,
					new double[] { 3, 12, 102 }, "Y"));
		}
		{
			CounterDataStream x = new CounterDataStream(new Dt(1, TUnit.HOUR),
					1);
			x.setTime(new Time(2000, 1, 1));
			x.setLabel("X");
			CounterDataStream y = new CounterDataStream(new Dt(1, TUnit.HOUR),
					10);
			y.setTime(new Time(2000, 1, 1));
			y.setLabel("Y");
			ADataStream original = new ExtraDimensionsDataStream(
					KMatchPolicy.IGNORE_TIMESTAMPS, Arrays.asList(
							(IDataStream) x, y, new CounterDataStream(new Dt(1,
									TUnit.HOUR), 100)));
			AbstractIterator<Datum> dit = original.iterator();
			Datum a = dit.next();
			Datum b = dit.next();
			Datum c = dit.next();

			assert a.getLabel() == null;

			assert Datum.equals(a, new Datum(new Time(2000, 1, 1),
					new double[] { 1, 10, 100 }, null));
			assert Datum.equals(b, new Datum(new Time(2000, 1, 1, 1, 0, 0),
					new double[] { 2, 11, 101 }, null));
			assert Datum.equals(c, new Datum(new Time(2000, 1, 1, 2, 0, 0),
					new double[] { 3, 12, 102 }, null));
		}
	}

}
