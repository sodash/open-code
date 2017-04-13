/**
 *
 */
package com.winterwell.maths.stats.distributions.cond;

import org.junit.Test;

import com.winterwell.maths.vector.XY;

/**
 * @author Joe Halliwell <joe@winterwell.com>
 * 
 */
public class CntxtTest {

	@Test
	public void testEqualityEtc() {
		Cntxt c1 = new Cntxt(new String[] { "hello", "there" }, new Object[] {
				new XY(1, 2), 2 });
		Cntxt c2 = new Cntxt(new String[] { "hello", "there" }, new Object[] {
				new XY(1, 2), 2 });
		assert c1.equals(c2);
	}

}
