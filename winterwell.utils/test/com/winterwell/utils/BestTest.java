package com.winterwell.utils;

import junit.framework.TestCase;

public class BestTest extends TestCase {

	public void testBasic() {
		Best<String> best = new Best<String>();
		best.maybeSet("Alpha", 99);
		best.maybeSet("Beta", 15);
		assert best.getBest().equals("Alpha");
		assert best.getBestScore() == 99;
		assert best.getBestList().size() == 1;
	}

	public void testBasic2() {
		Best<String> best = new Best<String>();
		best.maybeSet("Alpha", 99);
		best.maybeSet("Beta", 99);
		assert best.getBestScore() == 99;
		assert best.getBestList().size() == 2;
	}

	public void testBasic3() {
		Best<String> best = new Best<String>();
		best.maybeSet("Alpha", 99);
		best.maybeSet("Beta", 100);
		assert best.getBestScore() == 100;
		assert best.getBestList().size() == 1;
	}

	public void testBasic4() {
		Best<String> best = new Best<String>();
		best.maybeSet("Alpha", Double.NEGATIVE_INFINITY);
		assert best.getBestScore() == Double.NEGATIVE_INFINITY;
		assert best.getBestList().size() == 1;
	}

	public void testNoElements() {
		Best<String> best = new Best<String>();
		assert best.getBestScore() == Double.NEGATIVE_INFINITY;
		assert best.getBestList().size() == 0;
	}
}
