/**
 *
 */
package com.winterwell.maths.datastorage;

import java.util.Arrays;

import org.junit.Test;

import com.winterwell.maths.datastorage.Vectoriser.KUnknownWordPolicy;

import junit.framework.Assert;
import no.uib.cipr.matrix.Vector;

/**
 * @author Joe Halliwell <joe@winterwell.com>
 * 
 */
public class VectoriserTest {

	@Test
	public void basicTest() {
		Vectoriser<String, Object> vr = new Vectoriser<String, Object>(
				new Index<String>(), KUnknownWordPolicy.Add);
		Vector v = vr.toVector(Arrays.asList("hello", "world", "sunny"));
		assert v.get(0) == 1;
		assert v.get(1) == 1;
		assert v.get(2) == 1;
		assert v.get(3) == 0;
		Vector w = vr.toVector(Arrays.asList("world"));
		Assert.assertEquals(1.0, w.get(1));
		Assert.assertEquals(0.0, w.get(0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testForbidPolicy() {
		Index<String> i = new Index<String>();
		i.add("alpha");
		i.add("axe");
		i.add("amazon");
		Vectoriser<String, Object> vr = new Vectoriser<String, Object>(i,
				KUnknownWordPolicy.Forbid);
		Vector v = vr.toVector(Arrays.asList("alpha", "bravo", "charlie"));
	}

	@Test
	public void testIgnorePolicy() {
		Index<String> i = new Index<String>();
		i.add("alpha");
		i.add("axe");
		i.add("amazon");
		Vectoriser<String, Object> vr = new Vectoriser<String, Object>(i,
				KUnknownWordPolicy.Ignore);
		Vector v = vr.toVector(Arrays.asList("alpha", "bravo", "charlie"));
		Assert.assertEquals(1.0, v.get(0));
		Assert.assertEquals(0.0, v.get(1));
	}

	@Test
	public void testZeroPolicy() {
		Index<String> i = new Index<String>();
		i.add("alpha");
		i.add("axe");
		i.add("amazon");
		Vectoriser<String, Object> vr = new Vectoriser<String, Object>(i,
				KUnknownWordPolicy.Zero);
		Vector v = vr.toVector(Arrays.asList("alpha", "bravo", "charlie"));
		Assert.assertEquals(1.0, v.get(1));
		Assert.assertEquals(2.0, v.get(0));
	}

}
