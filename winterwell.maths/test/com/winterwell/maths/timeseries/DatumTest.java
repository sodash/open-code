package com.winterwell.maths.timeseries;

import com.winterwell.maths.vector.XY;
import com.winterwell.utils.Utils;
import com.winterwell.utils.time.Time;

import junit.framework.TestCase;

public class DatumTest extends TestCase {

	public void testCopy() {
		Datum a = new Datum(new Time(), 1, "A");
		Datum b = a.copy();
		b.setModifiable(true);
		assert Datum.equals(a, b);
		b.getData()[0] = 7;
		assert a.x() == 1;
		assert !Datum.equals(a, b);
		b.setLabel("B");
		assert a.getLabel() == "A";
	}

	public void testDatumVector() {
		Datum d = new Datum(new Time(100), new XY(1, 2), "A");
		assert d.getTime().longValue() == 100;
		assert d.get(0) == 1;
		assert d.get(1) == 2;
		assert d.isLabelled("A");
	}

	public void testEqualsDatumDatum() {
		{
			Time t = new Time();
			Datum d1 = new Datum(t, new XY(0, 1), "Hello");
			Datum d2 = new Datum(t, new double[] { 0, 1 }, "Hel" + "lo");
			assert Datum.equals(d1, d2);
		}
		{
			Datum d1 = new Datum(new Time(), new XY(0, 1), "Hello");
			Utils.sleep(2);
			Datum d2 = new Datum(new Time(), new double[] { 0, 1 }, "Hel"
					+ "lo");
			assert !Datum.equals(d1, d2);
		}
	}

	public void testSetModifiable() {
		boolean fail = false;
		{
			try {
				Datum a = new Datum(new Time(), 1, "A");
				a.setModifiable(false);
				a.set(0, 7);
				fail = true;
			} catch (Throwable e) {
				// OK
			}
			assert !fail;
		}
		{ // poking the array
			Datum a = new Datum(new Time(), 1, "A");
			a.setModifiable(false);
			a.getData()[0] = 7;
			assert a.get(0) == 1; // What to do? make getData() do an array
									// copy?
		}
	}

	public void testX() {
		Datum a = new Datum(new Time(), 1, "A");
		assert a.x() == 1;
		Datum b = new Datum(new Time(), new double[] { 1, 2 }, "B");
		boolean fail = false;
		try {
			b.x();
			fail = true;
		} catch (Throwable e) {
			//
		}
		assert !fail;
	}

}
