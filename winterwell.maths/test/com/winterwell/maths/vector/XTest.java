package com.winterwell.maths.vector;

import java.util.Iterator;

import junit.framework.TestCase;
import no.uib.cipr.matrix.VectorEntry;

public class XTest extends TestCase {

	public void testGet() {
		X x = new X(7);
		assert x.get(0) == 7;
		x.set(0, 1);
		assert x.get(0) == 1;
	}

	public void testIterator() {
		X x = new X(7);
		Iterator<VectorEntry> it = x.iterator();
		assert it.hasNext();
		VectorEntry xi = it.next();
		assert !it.hasNext();
		assert xi.index() == 0;
		assert xi.get() == 7;
		xi.set(5);
		assert x.get(0) == 5;

		Iterator<VectorEntry> it2 = x.iterator();
		assert it2.hasNext();
		xi = it2.next();
		assert xi.get() == 5;
		assert !it2.hasNext();

	}

}
