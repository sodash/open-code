package com.winterwell.maths.stats.distributions.discrete;

import java.util.Arrays;

import org.junit.Test;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;

public class AndDistTest {

	@Test
	public void testNormalise() {
		ObjectDistribution d1 = new ObjectDistribution(new ArrayMap("a", 1.0, "b", 2.0, "c",1.0));
		ObjectDistribution d2 = new ObjectDistribution(new ArrayMap("a", 1.0, "b", 2.0, "d",1.0));
		AndDist and = new AndDist(d1, d2);
		and.normalise();
		System.out.println(and);
		
		double pa = and.prob("a");
		double pb = and.prob("b");
		double pc = and.prob("c");
		Printer.out(pa, pb, pc);
		assert pc==0;
		assert pb > pa;
		assert pa > 0;
		assert pa+pb == 1 : pa+pb;
	}

	
	@Test
	public void testMissingLabel() {
		ObjectDistribution d1 = new ObjectDistribution(new ArrayMap("a", 1.0, "b", 2.0));
		ObjectDistribution d2 = new ObjectDistribution(new ArrayMap("a", 1.0, "c",1.0));
		AndDist and = new AndDist(d1, d2);
		and.normalise();
		System.out.println(and);
		
		double pa = and.prob("a");
		double pb = and.prob("b");
		double pc = and.prob("c");
		Printer.out(pa, pb, pc);
		assert pb==0;
		assert pc==0;
		assert pa==1;
	}

	
	
	@Test
	public void testProb() {
		ObjectDistribution d1 = new ObjectDistribution(new ArrayMap("a", 1.0, "b", 2.0, "c",1.0));
		ObjectDistribution d2 = new ObjectDistribution(new ArrayMap("a", 1.0, "b", 2.0, "d",1.0));
		AndDist and = new AndDist(d1, d2);
		double pa = and.prob("a");
		double pb = and.prob("b");
		double pc = and.prob("c");
		Printer.out(pa, pb, pc);
		assert pc==0;
		assert pb > pa;
		assert pa > 0;
	}

	@Test
	public void testSimpleWeighting() {
		ObjectDistribution d1 = new ObjectDistribution(new ArrayMap("a", 1.0));
		AndDist a = new AndDist(d1);
		a.setValues(Arrays.asList("a", "b"));
		a.setMaxStrength(0.5);
		assert a.prob("a") == 0.75 : a.prob("a");
		assert a.prob("b") == 0.25 : a.prob("b");
	}
	
	
	@Test
	public void testWeighting() {
		ObjectDistribution d1 = new ObjectDistribution(new ArrayMap("a", 1.0));
		ObjectDistribution d2 = new ObjectDistribution(new ArrayMap("b", 1.0));
		AndDist a = new AndDist(d1, d2);
		a.setValues(Arrays.asList("a","b"));
		a.setMaxStrength(0.5, 0.9);
		System.out.println(a);
		ObjectDistribution od = new ObjectDistribution(
				new ArrayMap("a", 0.75 * 0.05, "b", 0.25 * 0.95));
		od.normalise();
		System.out.println("\n\nOD "+od);
		System.out.println("\nAND "+new ObjectDistribution(a.asMap())+"\n\n");
		
		System.out.println(od.prob("a"));
		assert MathUtils.equalish(a.prob("a"), od.prob("a")) : a.prob("a") + " vs " + od.prob("a");
		assert MathUtils.equalish(a.prob("b"), od.prob("b")) : a.prob("b") + " vs " + od.prob("b");
	}
	
	

	@Test
	public void testWeightingUnNormalised() {
		ObjectDistribution d1 = new ObjectDistribution(new ArrayMap("a", 2.0, "b", 0.0));
		ObjectDistribution d2 = new ObjectDistribution(new ArrayMap("b", 10000.0, "a", 0.0));
		AndDist a = new AndDist(d1, d2);
		a.setValues(Arrays.asList("a","b"));
		a.normalise();
				
		a.setMaxStrength(0.5, 0.9);
		System.out.println(a);
		ObjectDistribution od = new ObjectDistribution(
				new ArrayMap("a", 0.75 * 0.05, "b", 0.25 * 0.95));
		od.normalise();
		
		System.out.println("\n\nOD "+od);
		System.out.println("\nAND "+new ObjectDistribution(a.asMap())+"\n\n");
		
		System.out.println(od.prob("a"));
		double ap = a.prob("a");
		assert MathUtils.equalish(ap, od.prob("a")) : a.prob("a") + " vs " + od.prob("a");
		assert MathUtils.equalish(a.prob("b"), od.prob("b")) : a.prob("b") + " vs " + od.prob("b");
	}

}
