package com.winterwell.utils;

import junit.framework.TestCase;

public class BestOneTest extends TestCase {

	public void testBasic1() {
		BestOne<String> best = new BestOne<String>();
		best.maybeSet("Alpha", 99);
		best.maybeSet("Beta", 15);
		assert best.getBest().equals("Alpha");
		assert best.getBestScore() == 99;
	}

	public void testBasic2() {
		BestOne<String> best = new BestOne<String>();
		best.maybeSet("Alpha", 99);
		best.maybeSet("Beta", 99);
		assert best.getBest().equals("Alpha");
		assert best.getBestScore() == 99;
	}

	public void testBasic3() {
		BestOne<String> best = new BestOne<String>();
		best.maybeSet("Alpha", 99);
		best.maybeSet("Beta", 100);
		assert best.getBest().equals("Beta");
		assert best.getBestScore() == 100;
	}

	public void testBasic4() {
		BestOne<String> best = new BestOne<String>();
		best.maybeSet("Alpha", Double.NEGATIVE_INFINITY);
		assert best.getBestScore() == Double.NEGATIVE_INFINITY;
		assert best.getBest().equals("Alpha");
		// i have supposed that the behaviour of this method is similar to
		// BestOne for this case
	}

	public void testNoElements() {
		BestOne<String> best = new BestOne<String>();
		assert best.getBestScore() == Double.NEGATIVE_INFINITY;
		assert best.getBest() == null;
	}
}
