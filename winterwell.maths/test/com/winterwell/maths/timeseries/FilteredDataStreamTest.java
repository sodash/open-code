package com.winterwell.maths.timeseries;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

import junit.framework.TestCase;

public class FilteredDataStreamTest extends TestCase {

	public void testNext() {
		IDataStream stream = new RandomDataStream(new Gaussian1D(0, 1),
				new Time(), new Dt(1, TUnit.DAY));
		FilteredDataStream filterStream = new FilteredDataStream(stream) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected Datum filter(Datum datum) {
				if (datum.getData()[0] < 0)
					return null;
				return datum;
			}
		};
		List<Datum> data = new ArrayList<Datum>();
		AbstractIterator<Datum> dit = filterStream.iterator();
		for (int i = 0; i < 100; i++) {
			Datum d = dit.next();
			assert d.getData()[0] >= 0;
			data.add(d);
		}
		assert data.size() == 100;
	}

	public void testNext1D() {
		IDataStream stream = new RandomDataStream(new Gaussian1D(0, 1),
				new Time(), new Dt(1, TUnit.DAY));
		FilteredDataStream filterStream = new FilteredDataStream(stream) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected Datum filter(Datum datum) {
				if (datum.x() < 0)
					return null;
				return datum;
			}
		};
		List<Datum> data = new ArrayList<Datum>();
		AbstractIterator<Datum> dit = filterStream.iterator();
		for (int i = 0; i < 100; i++) {
			Datum d = dit.next();
			assert d.x() >= 0;
			data.add(d);
		}
		assert data.size() == 100;
	}

	public void testUsingList() {
		List<Datum> list = new ArrayList<Datum>();
		Datum a = new Datum(new Time(), new double[] { 0 }, null);
		Datum b = new Datum(new Time(), new double[] { 1 }, null);
		Datum c = new Datum(new Time(), new double[] { 2 }, null);
		list.add(a);
		list.add(b);
		list.add(c);
		IDataStream stream = new ListDataStream(list);
		FilteredDataStream filterStream = new FilteredDataStream(stream) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected Datum filter(Datum datum) {
				if (datum.getData()[0] <= 0)
					return null;
				return new Datum(datum.getTime(),
						new double[] { datum.getData()[0] + 1 }, null);
			}
		};
		List<Datum> data = new ArrayList<Datum>();
		for (Datum d : filterStream) {
			assert d.getData()[0] >= 0;
			data.add(d);
		}
		assert data.size() == 2;
		assert data.get(0).getData()[0] == 2;
		assert data.get(1).getData()[0] == 3;
	}

	public void testUsingList1D() {
		List<Datum> list = new ArrayList<Datum>();
		Datum a = new Datum(new Time(), 0, null);
		Datum b = new Datum(new Time(), 1, null);
		Datum c = new Datum(new Time(), 2, null);
		list.add(a);
		list.add(b);
		list.add(c);
		IDataStream stream = new ListDataStream(list);
		FilteredDataStream filterStream = new FilteredDataStream(stream) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected Datum filter(Datum datum) {
				if (datum.x() <= 0)
					return null;
				return new Datum(datum.getTime(), datum.x() + 1, null);
			}
		};
		List<Datum> data = new ArrayList<Datum>();
		for (Datum d : filterStream) {
			assert d.x() >= 0;
			data.add(d);
		}
		assert data.size() == 2;
		assert data.get(0).x() == 2;
		assert data.get(1).x() == 3;
	}

}
