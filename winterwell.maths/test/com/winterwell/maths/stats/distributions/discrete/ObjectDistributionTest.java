package com.winterwell.maths.stats.distributions.discrete;

import org.junit.Test;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;

import junit.framework.TestCase;

public class ObjectDistributionTest extends TestCase {

	public void testNaNTests() {
		double nan = Double.NaN;
		double inf = Double.POSITIVE_INFINITY;
		assert ! (nan >= 0);
		assert ! (nan <= 0);
		assert inf >= 0;
	}
	
	public void testPrinter() {
		Printer.setPrinter(ObjectDistribution.class, (od, sb) -> {
			for(Object k : od.getMostLikely(5)) {
				sb.append(Printer.str(k));
				sb.append(": ");
				sb.append(Printer.str(100*od.normProb(k))+"%; ");
			}
			if ( ! od.isEmpty()) StrUtils.pop(sb, 2);
		});
		
		ObjectDistribution<Object> od = new ObjectDistribution<>();
		od.put("apple", 123);
		od.put("banana", 223);
		od.put("pear", 23);
		od.put("pear2", 23);
		od.put("pear3", 23);
		od.put("pear4", 23);
		od.put("pear5", 25);
		od.put("pear6", 23);
		od.put("pear7", 2);
		
		String s = Printer.str(od);
		System.out.println(s);
	}
	
	public void testNormEmpty() {
		// There is no sensible answer here, but it should spit out a probability.
		// Given zero evidence, and not even a size-of-space, then being completely undecided on p vs not-p is the least wrong answer.
		ObjectDistribution<Object> od = new ObjectDistribution<>().setPseudoCount(2);
		double p = od.normProb("foo");
		assert p > 0;
		assert p < 1;
		assert MathUtils.equalish(p, 0.5);
	}
	
	public void testAddAll() {
		ObjectDistribution<String> d1 = new ObjectDistribution<String>();
		d1.setProb("a", 5);
		d1.setProb("b", 10);
		ObjectDistribution<String> d2 = new ObjectDistribution<String>();
		d2.setProb("a", 5);
		d2.setProb("c", 11);

		d1.addAll(1.0, d2);
		assert MathUtils.equalish(d1.prob("a"), 10);
		d1.addAll(1.0, d2);
		assert MathUtils.equalish(d1.prob("a"), 15);
		assert MathUtils.equalish(d1.prob("b"), 10);
		assert MathUtils.equalish(d1.prob("c"), 22);
	}
	
	@Test
	public void testNormProb() {
		ObjectDistribution<String> d1 = new ObjectDistribution<String>(new ArrayMap(
			"steven-king", 57,
			"daniel-winterstein", 1,
			"tracey-rosenberg", 0
				));
		{
			double p = d1.normProb("steven-king");
			assert MathUtils.isProb(p) : p;
		}
		{
			double p = d1.normProb("tracey-rosenberg");
			assert MathUtils.isProb(p) : p;
		}
		{
			double p = d1.normProb("foo");
			assert MathUtils.isProb(p) : p;
		}
	}
	
	@Test
	public void testNormProbWithPseudoCount() {
		ObjectDistribution<String> d1 = new ObjectDistribution<String>(new ArrayMap(
			"steven-king", 57,
			"daniel-winterstein", 1,
			"tracey-rosenberg", 0
				));
		double w1 = d1.getTotalWeight();
		d1.setPseudoCount(10);
		double w2 = d1.getTotalWeight();
		assert w2 > w1 : w2;
		assert w1 == 58 : w1;
		assert w2 >= 78 : w2; // NB: tracey: 0 does not get stored, hence doesn't get +10!!
		{
			double p = d1.normProb("steven-king");
			assert MathUtils.isProb(p) : p;
		}
		{
			double p = d1.normProb("tracey-rosenberg");
			assert MathUtils.isProb(p) : p;
		}
		{
			double p = d1.normProb("foo");
			assert MathUtils.isProb(p) : p;
		}
	}
	
	@Test
	public void testNormProbWithPseudoCount2() {
		ObjectDistribution<String> d1 = new ObjectDistribution<String>(new ArrayMap(
			"steven-king", 0.0,
			"daniel-winterstein", 0.0,
			"tracey-rosenberg", 0.0
				), false);
		double w1 = d1.getTotalWeight();
		assert w1 == 0;
		d1.setPseudoCount(10);
		double w2 = d1.getTotalWeight();
		assert w2 > w1 : w2;
		{
			double p = d1.normProb("steven-king");
			assert MathUtils.isProb(p) : p;
		}
		{
			double p = d1.normProb("tracey-rosenberg");
			assert MathUtils.isProb(p) : p;
		}
		{
			double p = d1.normProb("foo");
			assert MathUtils.isProb(p) : p;
		}
	}

	
	@Test
	public void testNormProbWithPseudoCountBug() {
		ArrayMap map = new ArrayMap("negative",0.0, "positive",3.0, "!none-of-sentiment",1.0);
		ObjectDistribution<String> d1 = new ObjectDistribution<String>(map, false);
		double w1 = d1.getTotalWeight();
		d1.setPseudoCount(10);
		double w2 = d1.getTotalWeight();
		assert w2 > w1 : w2;
		double total=0;
		for(String x : d1) {
			total += d1.normProb(x);
		}
		assert total == 1.0 : total;
	}


	public void testCorners() {
		ObjectDistribution<String> d = new ObjectDistribution<String>();
		d.normalise();
		d.setProb("A", 0.00001);
		assert d.getMostLikely().equals("A");

		d.setProb("B", 0);
		d.normalise();
		assert d.getMostLikely().equals("A");

	}

	public void testEffectiveParticleCount() {
		{
			ObjectDistribution<String> oh = new ObjectDistribution<String>();
			oh.count("b");
			oh.count("c");
			for (int i = 0; i < 1000; i++) {
				oh.count("a");
			}

			assert oh.prob("a") == 1000;

			double cnt = oh.getEffectiveParticleCount();
			assert cnt > 1 && cnt < 2 : cnt;
			Printer.out(oh.toString() + " " + cnt);
		}
		{
			ObjectDistribution<String> oh = new ObjectDistribution<String>();
			for (int i = 0; i < 1000; i++) {
				oh.count("a" + i);
			}

			assert oh.prob("a567") == 1;

			double cnt = oh.getEffectiveParticleCount();
			assert cnt > 900 && cnt < 1100 : cnt;
			Printer.out(oh.toString() + " " + cnt);
		}
	}

	public void testNormalise() {
		ObjectDistribution<String> d = new ObjectDistribution<String>();
		d.setProb("A", 90);
		d.setProb("B", 10);
		assert !d.isNormalised();
		d.normalise();
		assert d.isNormalised();
		assert d.prob("A") == 0.9;
		assert d.prob("B") == 0.1;
		d.setProb("C", 1);
		assert !d.isNormalised();
		d.normalise();
		assert d.isNormalised();
		assert d.prob("A") == 0.45;
		assert d.prob("B") == 0.05;
		assert d.prob("C") == 0.5;
	}


	public void testNormaliseWPseudoCount() {
		ObjectDistribution<String> d = new ObjectDistribution<String>();
		d.setProb("A", 90);
		d.setProb("B", 10);
		d.setPseudoCount(0.1);
		assert ! d.isNormalised();
		d.normalise();
		assert d.isNormalised();
		double pa = d.prob("A");
		double pb = d.prob("B");
		assert pa+pb == 1 : pa+pb;
	}

	
	public void testObjectDistribution() {
		ObjectDistribution<String> oh = new ObjectDistribution<String>();
		for (String s : new String[] { "a", "b", "c", "d", "a", "b", "c", "a",
				"b" }) {
			oh.count(s);
		}
		Printer.out(oh.toString());
		assert oh.prob("a") == 3;

		double cnt = oh.getEffectiveParticleCount();
		assert cnt > 1 && cnt < 5 : cnt;
	}

	public void testProb() {
		ObjectDistribution<String> d = new ObjectDistribution<String>();
		d.setProb("A", 90);
		d.setProb("B", 10);
		assert d.prob("A") == 90;
		assert d.prob("B") == 10;
		assert d.prob("C") == 0;
		d.normalise();
		assert d.prob("A") == 0.9;
		assert d.prob("B") == 0.1;
		assert d.prob("C") == 0;
	}

	public void testPruning() {
		ObjectDistribution<String> oh = new ObjectDistribution<String>();
		for (String s : new String[] { "a", "a", "b", "a", "b", "c", "c", "d",
				"a", "b" }) {
			oh.count(s);
		}
		oh.prune(3);
		Printer.out(oh.toString());
		assert oh.prob("a") == 4;
		assert oh.prob("b") == 3;
		assert oh.prob("c") == 2;
		assert oh.prob("d") == 0;
	}

	public void testPseudoCount() {
		ObjectDistribution<String> d = new ObjectDistribution<String>();
		d.setPseudoCount(0.25);
		d.count("A");
		d.count("A");
		d.count("B");

		{
			double pa = d.prob("A");
			assert pa == 2.25;
			double pb = d.prob("B");
			assert pb == 1.25;
		}

		d.normalise();

		{
			double pa = d.prob("A");
			double pb = d.prob("B");
			assert MathUtils.equalish(pa + pb, 1) : pa + pb;

			// Note: after normalising, the pseudocount is going to
			// count for considerably more (because it stays the same).
			// assert MathUtils.equalish(pa, 2.25/3.5) :
			// pa+" "+pb+" vs "+2.25/3.5;
			// assert pb==1.25/3.5;
		}
	}

	public void testSample() {
		ObjectDistribution<String> d = new ObjectDistribution<String>();
		d.setProb("A", 90);
		d.setProb("B", 10);
		d.setProb("C", 0);
		ObjectDistribution<String> sample = new ObjectDistribution<String>();
		for (int i = 0; i < 1000; i++) {
			sample.count(d.sample());
		}
		assert sample.prob("A") > 800;
		assert sample.prob("B") > 50;
		assert sample.prob("C") == 0;
	}

	public void testTrivial() {
		ObjectDistribution<String> oh = new ObjectDistribution<String>();
		for (String s : new String[] { "a", "a", "b" }) {
			oh.count(s);
		}
		Printer.out(oh.toString());
		assert oh.prob("a") == 2;
	}
}
