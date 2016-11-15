package com.winterwell.utils;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import com.winterwell.utils.MathUtils;

import com.winterwell.utils.containers.Pair2;

public class MathUtilsTest extends TestCase {

	@Test public void testNumDisplay() {
		DecimalFormat format = new DecimalFormat("#,###.##");
		System.out.println(format.format(123));
		System.out.println(format.format(1.2345));
		System.out.println(format.format(1.03));
		System.out.println(format.format(1.07));
		System.out.println(format.format(1.009));
		System.out.println(format.format(169000));
	}

	@Test public void testFilterFinite() {
		{
			double[] xs = new double[]{-1,0,1};
			double[] x2s = MathUtils.filterFinite(xs);
			assert x2s.length==3 && x2s[0] == -1 && x2s[1]==0 && x2s[2]==1;
		}
		{
			double[] xs = new double[]{-1,Double.POSITIVE_INFINITY,Double.NaN};
			double[] x2s = MathUtils.filterFinite(xs);
			assert x2s.length==1 && x2s[0] == -1;
		}
		{
			double[] xs = new double[]{Double.NaN,1,2};
			double[] x2s = MathUtils.filterFinite(xs);
			assert x2s.length==2 && x2s[0] == 1 && x2s[1] == 2;
		}
	}

	@Test public void testCompare() {
		List<Number> list = Arrays.asList((Number)1,null,0.5,-2.5,null);
		Collections.sort(list, MathUtils.COMPARE);
		assert list.equals(Arrays.asList(null,null,-2.5,0.5,1)) : list;
	}

	@Test
	public void testNumberRoundsDown() {
		Double v = 3.99;
		int i = v.intValue();
		assert i == 3 : i;
	}

	@Test
	public void testGenerateCode() {

		char[] sorted = Arrays.copyOf(MathUtils.charTab,
				MathUtils.charTab.length);
		Arrays.sort(sorted);
		assert Arrays.equals(sorted, MathUtils.charTab);

		String c0 = MathUtils.generateB64Code(0);
		assert c0.equals("-");
		long l0 = MathUtils.decodeB64(c0);
		assert l0 == 0 : l0;

		String c1 = MathUtils.generateB64Code(1);
		assert c1.equals("0");
		long l1 = MathUtils.decodeB64(c1);
		assert l1 == 1;

		String c10 = MathUtils.generateB64Code(10);
		assert c10.equals("9") : c10;
		long l10 = MathUtils.decodeB64(c10);
		assert l10 == 10;

		String c11 = MathUtils.generateB64Code(11);
		assert c11.equals("A") : c11;
		long l11 = MathUtils.decodeB64(c11);
		assert l11 == 11;

		String c63 = MathUtils.generateB64Code(63);
		assert c63.equals("z");
		long l63 = MathUtils.decodeB64(c63);
		assert l63 == 63;

		String c2 = MathUtils.generateB64Code(64);
		assert c2.equals("-0") : c2;
		long l64 = MathUtils.decodeB64(c2);
		assert l64 == 64;

		String c65 = MathUtils.generateB64Code(65);
		assert c65.equals("00") : c65;
		long l65 = MathUtils.decodeB64(c65);
		assert l65 == 65;

		String c66 = MathUtils.generateB64Code(66);
		assert c66.equals("10");
		long l66 = MathUtils.decodeB64(c66);
		assert l66 == 66;

		String c3digit = MathUtils.generateB64Code(1 * 64 * 64 + 2 * 64 + 3);
		assert c3digit.equals("210") : c3digit;
		long l3d = MathUtils.decodeB64(c3digit);
		assert l3d == 1 * 64 * 64 + 2 * 64 + 3;

		// Hm - this is longer than I expected, have I screwed up?
		String c5 = MathUtils.generateB64Code(Long.MAX_VALUE);
		assert c5.equals("zzzzzzzzzz6") : c5;
	}

	public void testEqualish() {
		assert MathUtils.equalish(1000000, 1000500);
		assert MathUtils.equalish(100.1, 100);
		assert MathUtils.equalish(0.0001, 0.000101);
		assert MathUtils.equalish(0.000001, 0.000001);

		assert !MathUtils.equalish(1000000, 2000500);
		assert !MathUtils.equalish(100, 110);
		assert !MathUtils.equalish(0.000001, 0.000002);

		assert !MathUtils.equalish(1, -1);

		// ~1%
		assert !MathUtils.equalish(10, 100);
		assert !MathUtils.equalish(108, 100);
		assert !MathUtils.equalish(100, 107);
		assert !MathUtils.equalish(104, 100);
		assert MathUtils.equalish(100, 101);
		assert MathUtils.equalish(101, 100);
		assert MathUtils.equalish(100, 100);
	}

	public void testGetNumber() {
		assert MathUtils.getNumber(null) == 0;
		assert MathUtils.getNumber("") == 0;
		assert MathUtils.getNumber("0") == 0;
		assert MathUtils.getNumber("-1") == -1;
		assert MathUtils.getNumber("-1-2") == 0;
		assert MathUtils.getNumber("-99") == -99;
		assert MathUtils.getNumber("1") == 1;
		assert MathUtils.getNumber("99") == 99;
		assert MathUtils.getNumber("0.01") == 0.01;
		assert MathUtils.getNumber("-0.01") == -0.01;

		assert MathUtils.getNumber("yo") == 0;
		assert MathUtils.getNumber("£5.50") == 5.5 : MathUtils
				.getNumber("£5.50");
		assert MathUtils.getNumber("100px") == 100;
		assert MathUtils.getNumber("20%") == 0.2;

		assert MathUtils.getNumber("1,002") == 1002 : MathUtils.getNumber("1,002");
		
		assert MathUtils.getNumber("2nite") == 0;
	}

	public void testIsFinite() {
		assert MathUtils.isFinite(1);
		assert MathUtils.isFinite(Double.NEGATIVE_INFINITY) == false;
		assert MathUtils.isFinite(Double.POSITIVE_INFINITY) == false;
		assert MathUtils.isFinite(Double.POSITIVE_INFINITY - 1) == false;
		assert MathUtils.isFinite(Double.NaN) == false;
	}

	public void testIsProb() {
		assert MathUtils.isProb(0);
		assert MathUtils.isProb(1);
		assert MathUtils.isProb(-0.005) == false;
		assert MathUtils.isProb(1.005) == false;
		assert MathUtils.isProb(Double.NaN) == false;
		{
			Pair2<?, Float> p = new Pair2(1, new Float(0.5f));
			assert MathUtils.isProb(p.second);
		}
	}

	public void testMachineEpsilon() {
		double machEps = 1.0f;

		do {
			machEps /= 2.0;
		} while ((1.0 + (machEps / 2.0)) != 1.0);

		System.out.println("Calculated Machine epsilon: " + machEps);
	}

	public void testMax() {
		assert MathUtils.max(0.0) == 0.0;
		assert MathUtils.max(0.1, 0.1) == 0.1;
		assert MathUtils.max(-0.5, 0.5) == 0.5;
		assert MathUtils.max(89, 0.0, 0.1) == 89;
		assert MathUtils.max(0, 0.1, 89) == 89;
		assert MathUtils
				.max(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY) == Double.POSITIVE_INFINITY;
	}
	
	public void testMaxBigInt() {
		assert MathUtils.max(null, new BigInteger("1")).doubleValue() == 1;
		assert MathUtils.max(new BigInteger("1"), new BigInteger("1000")).doubleValue() == 1000;
	}

	public void testMin() {
		assert MathUtils.min(0.0) == 0.0;
		assert MathUtils.min(0.1, 0.1) == 0.1;
		assert MathUtils.min(-0.5, 0.5) == -0.5;
		assert MathUtils.min(89, 0.0, 0.1) == 0.0;
		assert MathUtils.min(0.0, 0.1, 89) == 0.0;
		assert MathUtils
				.min(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY) == Double.NEGATIVE_INFINITY;
	}

	public void testMinNoArgs() {
		try {
			MathUtils.min(new double[] {});
			fail("MathUtils.min (double array) should throw an error if no arguments are provided");
		} catch (Exception e) {
		}
	}

	public void testNaN() {
		double x = Double.NaN;
		double x2 = Double.NaN;
		assert x != x2;
	}

}
