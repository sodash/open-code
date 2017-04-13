/**
 *
 */
package com.winterwell.maths.stats.distributions.discrete;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.Containers;

/**
 * @author Joe Halliwell <joe@winterwell.com>
 * 
 */
public class IntegerDistributionTest {

	@Test
	public void testDensity() {
		IntegerDistribution id = new IntegerDistribution();
		id.setProb(2, 0.75);
		id.setProb(1, 0.25);

		assert id.prob(2.0) == 0.75;
		assert id.density(2) == 0.75;
		assert id.density(1.9) == 0.75;
		assert id.density(2.1) == 0.75;
	}

	@Test
	public void testEmpty() {
		IntegerDistribution id = new IntegerDistribution();
		assert id.getMean() == 0;
		assert id.getRange() == null;
	}

	@Test
	public void testKeyOrdering() {
		IntegerDistribution id = new IntegerDistribution();
		id.count(1);
		id.count(2);
		id.count(4);

		List<Double> keys = Containers.getList(id);
		assert keys.equals(Arrays.asList(1.0, 2.0, 3.0, 4.0)) : keys;
	}

	@Test
	public void testMean() {
		IntegerDistribution id = new IntegerDistribution();
		id.count(100);
		id.count(100);
		id.count(150);
		assert MathUtils.equalish(id.getMean(), 116.6666666);
	}

	@Test
	public void testObjectsBeingAnnoying() {
		IntegerDistribution id = new IntegerDistribution();
		id.setProb(2, 1);

		assert id.prob(2.0) == 1;
		assert id.prob(2) == 1;
		Integer two = 2;
		assert id.prob(two) == 1;
		Object obj = two;
		// assert ((ObjectDistribution)id).prob(obj) == 1; This fails!
	}

}
