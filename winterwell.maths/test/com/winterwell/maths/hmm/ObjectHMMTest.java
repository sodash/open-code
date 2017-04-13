package com.winterwell.maths.hmm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;

import junit.framework.TestCase;

public class ObjectHMMTest extends TestCase {

	public void testPEmit() {
		ObjectHMM<String, Integer> abc123 = new ObjectHMM<String, Integer>();
		List<String> abcd = Arrays.asList("A A A A A".split(" "));
		List<Integer> nums = Arrays.asList(1, 1, 1, 1, 2);
		for (int i = 0; i < 1000; i++) {
			abc123.trainOneExample(abcd, nums);
		}
		abcd = Arrays.asList("B B B B B".split(" "));
		nums = Arrays.asList(2, 2, 2, 2, 2);
		for (int i = 0; i < 100; i++) {
			abc123.trainOneExample(abcd, nums);
		}
		double p1a = abc123.PEmit(1, "A");
		double p2a = abc123.PEmit(2, "A");
		double p1b = abc123.PEmit(1, "B");
		double p2b = abc123.PEmit(2, "B");
		assert p1a > 0.75;
		assert p2a < 0.25;
		assert Math.abs(p1a + p2a - 1) < 0.1;
		assert p1b < 0.1;
		assert p2b > 0.9;
		assert Math.abs(p1b + p2b - 1) < 0.1;
	}

	public void testPruneHiddenStates() {
		ObjectHMM<String, Integer> abc123 = new ObjectHMM<String, Integer>();
		List<String> abcd = Arrays.asList("A B C A B C D".split(" "));
		List<Integer> nums = Arrays.asList(1, 2, 3, 1, 2, 3, 4);
		for (int i = 0; i < 1000; i++) {
			abc123.trainOneExample(abcd, nums);
		}
		List<String> pruned = abc123.pruneHiddenStates(3);
		assert pruned.size() == 1;
		assert pruned.contains("D");
	}

	public void testPTrans() {
		ObjectHMM<String, Integer> abc123 = new ObjectHMM<String, Integer>();
		List<String> abcd = Arrays.asList("A A A A B".split(" "));
		List<Integer> nums = Arrays.asList(1, 1, 1, 1, 2);
		for (int i = 0; i < 1000; i++) {
			abc123.trainOneExample(abcd, nums);
		}
		abcd = Arrays.asList("B B B B A".split(" "));
		nums = Arrays.asList(2, 2, 2, 2, 1);
		for (int i = 0; i < 100; i++) {
			abc123.trainOneExample(abcd, nums);
		}
		double paa = abc123.PTrans("A", "A");
		double pba = abc123.PTrans("B", "A");
		double pbb = abc123.PTrans("B", "B");
		double pab = abc123.PTrans("A", "B");
		assert paa > 0.7 : paa;
		assert pba < 0.3 : pba;
		assert Math.abs(paa + pba - 1) < 0.1;
		assert pbb > 0.7 : pbb;
		assert pab < 0.3 : pab;
		assert Math.abs(pbb + pab - 1) < 0.1;
	}

	@Test
	public void testSample() {
		ObjectHMM<String, Integer> abc123 = new ObjectHMM<String, Integer>();
		List<String> abcd = Arrays.asList("A B C D".split(" "));
		List<Integer> nums = Arrays.asList(1, 2, 3, 4);
		for (int i = 0; i < 1000; i++) {
			abc123.trainOneExample(abcd, nums);
		}
		List<String> abc = abc123.sample(nums);
		assert abc.equals(abcd) : abc;
	}

	public void testStartPoints() {
		ObjectHMM<String, Integer> abc123 = new ObjectHMM<String, Integer>();
		{
			List<String> abcd = Arrays.asList("A".split(" "));
			List<Integer> nums = Arrays.asList(1);
			for (int i = 0; i < 1000; i++) {
				abc123.trainOneExample(abcd, nums);
			}
		}
		{
			List<String> abcd = Arrays.asList("B".split(" "));
			List<Integer> nums = Arrays.asList(1);
			for (int i = 0; i < 500; i++) {
				abc123.trainOneExample(abcd, nums);
			}
		}
		assert abc123.PStart("A") > 0.6;
		assert abc123.PStart("B") < 0.4;
		List<Integer> nums = Arrays.asList(1);
		ObjectDistribution<String> samples = new ObjectDistribution<String>();
		for (int i = 0; i < 1000; i++) {
			List<String> s = abc123.sample(nums);
			samples.count(s.get(0));
		}
		assert samples.prob("A") > 600 : samples;
		assert samples.prob("B") > 250 : samples;
	}

	public void testTrainOneExample() {
		ObjectHMM<String, Integer> abc123 = new ObjectHMM<String, Integer>();
		List<String> abcd = Arrays.asList("A B C D".split(" "));
		List<Integer> nums = Arrays.asList(1, 2, 3, 4);
		abc123.trainOneExample(abcd, nums);
		// NB: trans and emit both use a default pseudo-count of 1
		assert abc123.PTrans("B", "A") == 2.0 / 5 : abc123.PTrans("B", "A");
		assert abc123.PTrans("B", "C") == 1.0 / 5;
		assert abc123.PEmit(1, "A") == 2.0 / 5 : abc123.PEmit(1, "A");
		assert abc123.PEmit(2, "A") == 1.0 / 5;
	}

	public void testViterbi() {
		ObjectHMM<String, Integer> abc123 = new ObjectHMM<String, Integer>();
		List<String> abcd = Arrays.asList("A B C D".split(" "));
		List<Integer> nums = Arrays.asList(1, 2, 3, 4);
		for (int i = 0; i < 1000; i++) {
			abc123.trainOneExample(abcd, nums);
		}
		List<String> abc = abc123.viterbi(nums);
		assert abc.equals(abcd) : abc;
	}

	public void testViterbi2() {
		ObjectHMM<String, Integer> abc123 = new ObjectHMM<String, Integer>();
		List<String> abcd = Arrays.asList("A B B A".split(" "));
		List<Integer> nums = Arrays.asList(1, 2, 4, 3);
		for (int i = 0; i < 1000; i++) {
			abc123.trainOneExample(abcd, nums);
		}
		for (int i = 0; i < 100; i++) {
			abc123.trainOneExample(Arrays.asList("A A".split(" ")),
					Arrays.asList(5, 5));
		}

		List<String> abc = abc123.viterbi(nums);
		assert abc.equals(abcd) : abc;

		{
			nums = Arrays.asList(1, 1);
			abc = abc123.viterbi(nums);
			assert abc.equals(Arrays.asList("A A".split(" "))) : abc;
		}
		{
			nums = Arrays.asList(1, 1, 4, 2);
			abc = abc123.viterbi(nums);
			assert abc.equals(Arrays.asList("A A B B".split(" "))) : abc;
		}
	}

	/**
	 * Bug caused by normalisation issues?
	 */
	public void testViterbi3() {
		ObjectHMM<String, Integer> abc123 = new ObjectHMM<String, Integer>();
		List<String> abcd = Arrays.asList("A A A A A".split(" "));
		List<Integer> nums = Arrays.asList(1, 1, 1, 1, 2);
		for (int i = 0; i < 1000; i++) {
			abc123.trainOneExample(abcd, nums);
		}
		abcd = Arrays.asList("B B B B B".split(" "));
		nums = Arrays.asList(2, 2, 2, 2, 2);
		for (int i = 0; i < 100; i++) {
			abc123.trainOneExample(abcd, nums);
		}
		List<String> abc = abc123.viterbi(nums);
		assert abc.equals(abcd) : abc;
	}

	/**
	 * Tests smoothing
	 */
	public void testViterbi4() {
		ObjectHMM<String, Integer> abc123 = new ObjectHMM<String, Integer>();
		List<String> abcd = Arrays.asList("A B".split(" "));
		List<Integer> nums = Arrays.asList(1, 2);
		for (int i = 0; i < 1000; i++) {
			abc123.trainOneExample(abcd, nums);
		}
		abcd = Arrays.asList("A A".split(" "));
		nums = Arrays.asList(1, 1);
		for (int i = 0; i < 100; i++) {
			abc123.trainOneExample(abcd, nums);
		}
		abcd = Arrays.asList("A A".split(" "));
		nums = Arrays.asList(2, 2);
		for (int i = 0; i < 10; i++) {
			abc123.trainOneExample(abcd, nums);
		}

		nums = Arrays.asList(1, 1, 2, 2, 2, 1);
		List<String> abc = abc123.viterbi(nums);
		assert abc.equals(Arrays.asList("A A B B B A".split(" "))) : abc;

		// And now with almost no smoothing
		abc123.setAnyOutputIsPossible(0);
		abc123.setAnyTransitionIsPossible(0);
		abc = abc123.viterbi(nums);
		assert abc.equals(Arrays.asList("A A A A A A".split(" "))) : abc;
	}

	public void testViterbiSpeed() {
		ObjectHMM<String, Integer> abc123 = new ObjectHMM<String, Integer>();
		// Train
		Random random = new Random();
		for (int i = 0; i < 10000; i++) {
			List<String> abc = new ArrayList<String>();
			List<Integer> nums = new ArrayList<Integer>();
			for (int t = 0; t < 10; t++) {
				int i1 = random.nextInt(26);
				int i2 = random.nextInt(26);
				String ab = "" + (char) ('a' + i1) + (char) ('a' + i2);
				abc.add(ab);
				int n = 10 * i1 + i2;
				nums.add(n);
			}
			abc123.trainOneExample(abc, nums);
		}
		// OK?
		List<Integer> nums = Arrays.asList(0, 11, 22, 33, 44, 55, 66, 77, 88,
				99);
		double e1 = abc123.PEmit(0, "aa");
		double e2 = abc123.PEmit(11, "aa");
		double e3 = abc123.PEmit(11, "bb");
		assert e1 > e2;
		assert e3 > e2;
		// Viterbi stress test
		for (int i = 0; i < 10; i++) {
			List<String> abcd = abc123.viterbi(nums);
		}
		List<String> abc = abc123.viterbi(nums);
		// assert
		// abc.equals(Arrays.asList("aa bb cc dd ee ff gg hh ii jj".split(" ")))
		// : abc;
	}

	/**
	 * Previously unseen input
	 */
	public void testViterbiUnseen() {
		ObjectHMM<String, Integer> abc123 = new ObjectHMM<String, Integer>();
		abc123.setAllowUnseenObservations(true);
		List<String> abcd = Arrays.asList("A A A A B B A".split(" "));
		List<Integer> nums = Arrays.asList(1, 1, 1, 1, 2, 2, 1);
		for (int i = 0; i < 1000; i++) {
			abc123.trainOneExample(abcd, nums);
		}
		nums = Arrays.asList(1, 3, 1, 1, 2);
		List<String> abc = abc123.viterbi(nums);
		assert abc.equals(Arrays.asList("A A A A B".split(" ")));
	}

}
