package com.winterwell.maths.timeseries;

import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

import junit.framework.TestCase;

/**
 * Tests for {@link SharedDataStream}
 * 
 * TODO test multi-threaded access!
 * 
 * @author daniel
 */
public class SharedDataStreamTest extends TestCase {

	public void testSharing() {
		CounterDataStream base = new CounterDataStream(new Dt(1, TUnit.MINUTE),
				1);
		SharedDataStream share = new SharedDataStream(base.iterator(), 2);
		AbstractIterator<Datum> a = share.getStream(0);
		AbstractIterator<Datum> b = share.getStream(1);

		Datum a1 = a.next();
		assert a1.x() == 1;
		Datum a2 = a.next();
		assert a2.x() == 2;
		Datum a3 = a.next();
		assert a3.x() == 3;

		// caching the next value will mean the backlog for b is one greater at
		// this point
		assert share.backlog.size() == 3 : share.backlog;
		b.peekNext();
		// assert share.backlog.size() == 3 : share.backlog;

		Datum b1 = b.next();
		assert b1.x() == 1;
		Datum b2 = b.next();
		assert b2.x() == 2;
		assert share.backlog.size() == 1 : share.backlog;

		Datum a4 = a.next();

		Datum b3 = b.next();
		Datum b4 = b.next();
		Datum b5 = b.next();
		Datum b6 = b.next();

		Datum a5 = a.next();
		Datum a6 = a.next();

		Datum[] as = new Datum[] { a1, a2, a3, a4, a5, a6 };
		Datum[] bs = new Datum[] { b1, b2, b3, b4, b5, b6 };

		for (int i = 0; i < 6; i++) {
			assert as[i] == bs[i] : i + ": " + as[i] + " " + bs[i];
			assert as[i].x() == i + 1;
		}

		assert share.backlog.size() == 0;
	}

	public void testSharing3Way() {
		CounterDataStream base = new CounterDataStream(new Dt(1, TUnit.MINUTE),
				1);
		SharedDataStream share = new SharedDataStream(base.iterator(), 3);
		AbstractIterator<Datum> a = share.getStream(0);
		AbstractIterator<Datum> b = share.getStream(1);
		AbstractIterator<Datum> c = share.getStream(2);

		Datum a1 = a.next();
		assert a1.x() == 1;
		Datum a2 = a.next();
		assert a2.x() == 2;
		Datum a3 = a.next();
		assert a3.x() == 3;

		Datum b1 = b.next();
		assert b1.x() == 1;
		Datum b2 = b.next();
		assert b2.x() == 2;

		Datum a4 = a.next();

		Datum c1 = c.next();
		Datum c2 = c.next();
		Datum c3 = c.next();

		Datum b3 = b.next();
		Datum b4 = b.next();
		Datum b5 = b.next();
		Datum b6 = b.next();

		Datum c4 = c.next();
		Datum c5 = c.next();
		Datum c6 = c.next();

		Datum a5 = a.next();
		Datum a6 = a.next();

		Datum[] as = new Datum[] { a1, a2, a3, a4, a5, a6 };
		Datum[] bs = new Datum[] { b1, b2, b3, b4, b5, b6 };
		Datum[] cs = new Datum[] { c1, c2, c3, c4, c5, c6 };

		for (int i = 0; i < 6; i++) {
			assert as[i] == bs[i] : i + ": " + as[i] + " " + bs[i];
			assert as[i] == cs[i] : i + ": " + as[i] + " " + cs[i];
			assert as[i].x() == i + 1;
		}

		assert share.backlog.size() == 0;
	}

}
