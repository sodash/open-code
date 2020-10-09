/**
 * 
 */
package com.winterwell.maths.timeseries;

import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.time.Time;

import junit.framework.TestCase;

/**
 * TODO test blocking behaviour!
 * 
 * @testedby  PipedDataStreamTestTest}
 * @author daniel
 */
public class PipedDataStreamTest extends TestCase {

	/**
	 * Test method for
	 * {@link winterwell.maths.timeseries.PipedDataStream#hasNext()}.
	 */
	public void testHasNext() {
		PipedDataStream _pipe = new PipedDataStream(1);
		_pipe.add(new Datum(new Time(), 1, null));
		_pipe.add(new Datum(new Time(), 2, null));
		AbstractIterator<Datum> pipe = _pipe.iterator();
		assert pipe.hasNext();
		pipe.next();
		assert pipe.hasNext();
		_pipe.addEndOfStream();
		assert pipe.hasNext();
		pipe.next();
		assert !pipe.hasNext();
	}

	/**
	 * Test method for
	 * {@link winterwell.maths.timeseries.PipedDataStream#next()}.
	 */
	public void testNext() {
		PipedDataStream pipe = new PipedDataStream(1);
		pipe.add(new Datum(new Time(), 1, null));
		pipe.add(new Datum(new Time(), 2, null));
		AbstractIterator<Datum> dit = pipe.iterator();
		Datum a = dit.next();
		Datum b = dit.next();
		pipe.addEndOfStream();
		Datum c = dit.peekNext();
		assert a.x() == 1;
		assert b.x() == 2 : b;
		assert c == null;
	}

	/**
	 * Test method for
	 * {@link winterwell.maths.timeseries.PipedDataStream#peekNext()}.
	 */
	public void testPeekNext() {
		PipedDataStream pipe = new PipedDataStream(1);
		pipe.add(new Datum(new Time(), 1, null));
		pipe.add(new Datum(new Time(), 2, null));
		AbstractIterator<Datum> dit = pipe.iterator();
		Datum a = dit.peekNext();
		Datum a2 = dit.peekNext();
		dit.next();
		Datum b = dit.peekNext();
		dit.next();
		pipe.addEndOfStream();
		Datum c = dit.peekNext();
		assert a.x() == 1;
		assert a2.x() == 1 : a2;
		assert b.x() == 2 : b;
		assert c == null;
	}

}
