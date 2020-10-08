package com.winterwell.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.web.XStreamUtils;

/**
 * @testedby {@link MathUtilsTest}
 * @author daniel
 * 
 */
public class MathUtils {

	private static double MACH_EPS = -1;

	/**
	 * Allows for e.g. "-£ 10,000" or "100px"
	 */
	private static Pattern NUMBER = Pattern
			.compile("-? *[^0-9a-zA-Z]? *([0-9\\.,]+)[^0-9]{0,3}");

	public static final double ROOT_TWO = Math.sqrt(2);

	public static double[] abs(double... values) {
		double[] values2 = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			values2[i] = Math.abs(values[i]);
		}
		return values2;
	}

	/**
	 * What's the most efficient way of condensing a long into an azAZ0-9-_
	 * string? i.e. we want to express the long in base 64. Uses a
	 * least-significant-first encoding.
	 * 
	 * @param idNum
	 *            Must be positive
	 * @return a short String form for the id
	 */
	public static String generateB64Code(long idNum) {
		assert idNum >= 0 : idNum;
		char[] bin = Long.toBinaryString(idNum).toCharArray();
		// now one base 64 char = 7 binary chars??
		StringBuilder b64 = new StringBuilder(1 + bin.length / 7);
		short b64c = 0;
		int n = 0;
		for (int i = bin.length - 1; i != -1; i--) {
			char bc = bin[i];
			if (bc == '1') {
				b64c += twos[n];
			}
			n++;
			if (n == 6) {
				b64.append(charTab[b64c]);
				n = 0;
				b64c = 0;
			}
		}
		if (n != 0) {
			b64.append(charTab[b64c]);
		}
		// this is least-significant-digit first, but who cares
		return b64.toString();
	}

	static final short[] twos = new short[] { 1, 2, 4, 8, 16, 32, 64 };
	/**
	 * 64 url-safe chars.
	 * <p>
	 * The unusual ordering is so that binary search will work in this array.
	 */
	static final char[] charTab = "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz"
			.toCharArray();

	/**
	 * Inverse of {@link #generateB64Code(long)}
	 * 
	 * @param cde
	 * @return
	 */
	public static long decodeB64(String cde) {
		long v = 0;
		long pow = 1;
		for (int i = 0; i < cde.length(); i++) {
			char c = cde.charAt(i);
			int ci = Arrays.binarySearch(charTab, c);
			v += ci * pow;
			pow = 64 * pow;
		}
		return v;
	}

	/**
	 * A less precise version of {@link #equalish(double, double)}. Useful for
	 * testing stochastic things.
	 * 
	 * @param x
	 * @param y
	 * @return true if x roughly equals y. Uses ~25% tolerance for non-zero x,y.
	 *         The result is inherently arbitrary if x or y are zero.
	 */
	public static boolean approx(double x, double y) {
		if (x == 0 || y == 0)
			// arbitrary limit
			return Math.abs(x - y) < 0.25;
		double d = Math.abs(x - y);
		double m2 = Math.abs(x + y);
		return d / m2 < 0.125;
	}

	/**
	 * @param x
	 * @param y
	 * @return true if x approximately equals y. Uses ~1% tolerance for non-zero
	 *         x,y. The result is inherently arbitrary if x or y are zero.
	 * @see #approx(double, double)     
	 */
	public static boolean equalish(double x, double y) {
		if (x == 0 || y == 0)
			// arbitrary limit
			return Math.abs(x - y) < 0.001;
		double d = Math.abs(x - y);
		double m2 = Math.abs(x + y);
		return d / m2 < 0.005;
	}

	public static double euclideanLength(double[] vector) {
		double x2 = 0;
		for (double d : vector) {
			x2 += d * d;
		}
		return Math.sqrt(x2);
	}

	/**
	 * Simple convenience function: Cap value - if necessary - to lie in [min,
	 * max].
	 * 
	 * @param x
	 * @param min
	 * @param max
	 * @return x'
	 */
	public static double forceIn(double x, double min, double max) {
		assert min <= max;
		if (x < min)
			return min;
		if (x > max)
			return max;
		return x;
	}

	/**
	 * The smallest safe double. 
	 * Machine epsilon gives an upper bound on the relative error due to
	 * rounding in floating point arithmetic.
	 * 
	 * @author Wikipedia
	 */
	public static double getMachineEpsilon() {
		if (MACH_EPS != -1)
			return MACH_EPS;
		double machEps = 1.0;
		do {
			machEps /= 2.0;
		} while ((1.0 + (machEps / 2.0)) != 1.0);
		MACH_EPS = machEps;
		return MACH_EPS;
	}

	/**
	 * Flexible human-friendly convertor
	 * @param num
	 *            Can be null. Can be a Number or a String. 
	 *            Strings can include an optional leading symbol and
	 *            upto 3 trailing symbols (any non digit character or point).
	 *            E.g. £10 (but not -£10, at least for now), 20EUR, 100px, 0.5%,	 *            
	 *            etc.
	 *            Percentages are returned as fractions! E.g. "50%" -> 0.5
	 * @return double. 0 if num is null or could not be parsed. Does not throw an exception
	 *         if num is badly formatted.
	 * 
	 * @see #num(Object)
	 * @see StrUtils#isNumber(String)
	 * @see #toNum(Object)
	 * @testedby {@link MathUtilsTest#testGetNumber()}
	 */
	public static double getNumber(Object _num) {
		Double d = getNumber2(_num);
		return d==null? 0 : d;
	}
	
	private static Double getNumber2(Object _num) {
		if (_num == null) return null;
		if (_num instanceof Number) {
			return ((Number) _num).doubleValue();
		}
		String num = _num.toString().trim();
		if (num.isEmpty()) return null;
		// percentage?
		if (num.endsWith("%")) {
			String n = num.substring(0, num.length()-1);
			Double p = Double.valueOf(n);
			return p/100.0;
		}
		Matcher m = NUMBER.matcher(num);
		boolean ok = m.matches();
		if ( ! ok) {
			// HACK handle XStream pickled numbers		
			if (num.charAt(0)=='<' && num.endsWith(">")) {
				try {
					Number n = XStreamUtils.serialiseFromXml(num);
					return ((Number) _num).doubleValue();
				} catch(Exception ex) {
					// oh well
				}
			}
			return null;
		}
		
		try {
			String snum = m.group(1);			
			// HACK commas 
			snum = snum.replace(",", "");
			double x = Double.valueOf(snum);
			if (num.charAt(0) == '-') {
				x = - x;
			}
			return x;
		} catch (NumberFormatException e) {
			// quiet fail
			return null;
		}		
	}

	/**
	 * Less lenient Long flavoured version of {@link #getNumber(Object)}.
	 * Throws exceptions on fail.
	 * @param num Must not be null. Can be a Number or String
	 * @return long 
	 * 
	 * @see #getNumber(Object)
	 * @see #num(Object)
	 * @see StrUtils#isNumber(String)
	 */
	public static long numLong(Object _num) throws NumberFormatException {
		if (_num instanceof Number) {
			return ((Number) _num).longValue();
		}
		String num = _num.toString();
		return Long.valueOf(num);
	}
	
	
	/**
	 * Less lenient version of {@link #getNumber(Object)}.
	 * Throws exceptions on fail.
	 * @param num Must not be null. This can include an optional leading symbol and
	 *            upto 3 trailing symbols (any non digit character or point).
	 *            E.g. £10 (but not -£10, at least for now), 20EUR, 100px, 0.5%,
	 *            etc.
	 * @return double. 
	 * 
	 * @see #getNumber(Object)
	 * @see StrUtils#isNumber(String)
	 * @see #toNum(Object)
	 */
	public final static double num(Object _num) throws NumberFormatException {
		Double n = getNumber2(_num);
		if (n==null) throw new NumberFormatException("NAN: "+_num);
		return n;
	}
	
	/**
	 * @param x
	 * @return true if not infinite and not NaN
	 * @testedby {@link MathUtilsTest#testIsFinite()}
	 */
	public static boolean isFinite(double x) {
		return ! (Double.isNaN(x) || Double.isInfinite(x));
	}
	/**
	 * @param x
	 * @return true if finite and below {@link #TOO_BIG}
	 */
	public static boolean isSafe(double x) {
		return ! (Math.abs(x) > TOO_BIG || Double.isNaN(x) || Double.isInfinite(x));
	}

	public static boolean isProb(double x) {
		return x >= 0 && x <= 1.0000000001;
	}

	public static boolean isProb(float x) {
		return x >= 0 && x <= 1.0000000001;
	}

	/**
	 * @param x
	 * @return if true, this should be treated as zero. Never divide by such a
	 *         number!
	 */
	public static boolean isTooSmall(double x) {
		// allow a bit extra room around machine-epsilon
		return Math.abs(x) < 2*getMachineEpsilon();
	}

	/**
	 * Handle &lt; comparison between any two Numbers, e.g. BigInteger and Long
	 * 
	 * @param a Must not be null
	 * @param b Must not be null
	 * @return true if a < b
	 */
	public static boolean lessThan(Number a, Number b) {
		assert a != null && b!=null : a+" "+b;
		// unavoidably ugly
		if (a instanceof BigInteger) {
			BigInteger bigB = b instanceof BigInteger ? (BigInteger) b
					: new BigInteger(b.toString());
			int c = ((BigInteger) a).compareTo(bigB);
			return c < 0;
		}
		if (b instanceof BigInteger) {
			BigInteger bigA = a instanceof BigInteger ? (BigInteger) a
					: new BigInteger(a.toString());
			int c = bigA.compareTo((BigInteger) b);
			return c < 0;
		}
		if (a instanceof Long || b instanceof Long)
			return a.longValue() < b.longValue();
		return a.doubleValue() < b.doubleValue();
	}
	
	/**
	 * Handle &lt; comparison between any two Numbers, e.g. BigInteger and Long
	 * 
	 * @param a
	 * @param b
	 * @return a compareTo b. If a or b are null, this treats null as a low number.
	 * -1 if a is less than b
	 */
	public static int compare(Number a, Number b) {
		return COMPARE.compare(a, b);
	}
	
	
	/**
	 * Handle &lt; comparison between any two Numbers, e.g. BigInteger and Long
	 * 
	 * @param a
	 * @param b
	 * @return a compareTo b. If a or b are null, this treats null as a low number.
	 */
	public static final Comparator<Number> COMPARE = new NumberComparator();

	/**
	 * Not the maximum double, but anything bigger than this is too easy to overflow. 
	 */
	public static final double TOO_BIG = Math.pow(Double.MAX_VALUE, 0.2);

	private static double MAX_DOUBLE_INT = 0;

	/**
	 * 
	 * @param xs
	 *            Can contain nulls -- which are ignored
	 * @return this will be a member of the xs, null if the xs had no non-null
	 *         values
	 */
	public static <N extends Number> N max(Collection<N> xs) {
		assert !xs.isEmpty();
		double max = Double.NEGATIVE_INFINITY;
		N maxn = null;
		for (N number : xs) {
			// skip null!
			if (number == null) {
				continue;
			}
			// Cast down into double?! ??use lessThan instead??
			double x = number.doubleValue();
			if (x > max || maxn==null) {
				max = x;
				maxn = number;
			}
		}
		return maxn;
	}

	/**
	 * 
	 * @param values
	 *            Must not be zero-length or null. Cannot contain nulls (because: double), but see
	 *            {@link #max(Collection)} which can.
	 * @return max of values
	 */
	public static double max(double... values) {
		double max = values[0];
		for (double i : values) {
			if (i > max) {
				max = i;
			}
		}
		return max;
	}

	public static int max(int... values) {
		int max = values[0];
		for (int i : values) {
			if (i > max) {
				max = i;
			}
		}
		return max;
	}

	/**
	 * 
	 * @param xs
	 *            Can contain nulls -- which are ignored
	 * @return this will be a member of the xs
	 */
	public static <N extends Number> N min(Collection<N> xs) {
		assert !xs.isEmpty();
		double min = Double.POSITIVE_INFINITY;
		N minn = null;
		for (N number : xs) {
			// skip null!
			if (number == null) {
				continue;
			}
			double x = number.doubleValue();
			if (x < min) {
				min = x;
				minn = number;
			}
		}
		return minn;
	}

	/**
	 * @param values
	 *            Must not be zero-length or null
	 * @return min of values (remember that -100 beats 1 - use min + abs if you
	 *         want the smallest number)
	 */
	public static double min(double... values) {
		double min = values[0];
		for (double i : values) {
			if (i < min) {
				min = i;
			}
		}
		return min;
	}

	/**
	 * @param x
	 * @return x*x (yes it's just a minor convenience)
	 */
	public static double sq(double x) {
		return x * x;
	}

	public static double sum(double[] values) {
		double total = 0;
		for (double d : values) {
			total += d;
		}
		return total;
	}

	/**
	 * Copy numbers into an array.
	 */
	public static double[] toArray(Collection<? extends Number> values) {
		double[] arr = new double[values.size()];
		int i = 0;
		for (Number d : values) {
			arr[i] = d.doubleValue();
			i++;
		}
		return arr;
	}

	/**
	 * Copy numbers into an array. Note that using a Trove list in the first
	 * place is more efficient. ?? Should this be in {@link Containers}?
	 */
	public static int[] toIntArray(Collection<Integer> values) {
		int[] arr = new int[values.size()];
		int i = 0;
		for (Integer d : values) {
			arr[i] = d;
			i++;
		}
		return arr;
	}

	/**
	 * Since Math.max() doesn't handle BigInteger
	 * 
	 * @param a Can be null
	 * @param b Can be null
	 * @return the max of a,b, or null if both are null
	 */
	public static BigInteger max(BigInteger a, BigInteger b) {
		if (a==null) return b;
		if (b==null) return a;
		int c = a.compareTo(b);
		return c < 0? b : a;
	}
	
	/**
	 * Since Math.max() doesn't handle BigInteger
	 * 
	 * @param a Can be null
	 * @param b Can be null
	 * @return the max of a,b, or null if both are null
	 */
	public static BigInteger min(BigInteger a, BigInteger b) {
		if (a==null) return b;
		if (b==null) return a;
		int c = a.compareTo(b);
		return c > 0? b : a;
	}
	
	/**
	 * Cast from one number type to another. Handles the annoying Double/double divide.
	 * @param klass
	 * @param nv Can be null (returns null)
	 * @return nv as klass (which may involve rounding, e.g. double to int)
	 */
	public static <N extends Number> N cast(Class<N> klass, Number nv) {
		if (nv==null) return null;
		// no change :)
		if (nv.getClass()==klass) return (N) nv;
		// Ugly switch
		if (klass==Integer.class || klass==int.class) return (N)(Number) nv.intValue();
		if (klass==Long.class || klass==long.class) return (N)(Number) nv.longValue();
		if (klass==Float.class || klass==float.class) return (N)(Number) nv.floatValue();
		if (klass==Double.class || klass==double.class) return (N)(Number) nv.doubleValue();
		if (klass==Short.class || klass==short.class) return (N)(Number) nv.shortValue();
		if (klass==BigInteger.class) {
			// We've probably lost some level of detail here!
			if (nv instanceof BigDecimal) {
				return (N) ((BigDecimal) nv).toBigInteger();
			}
			BigInteger value = new BigInteger(""+nv.longValue());
			return (N) value;
		}
		if (klass==BigDecimal.class) {			
			BigDecimal value = new BigDecimal(nv.toString());
			return (N) value;
		}
		throw new TodoException(klass+" "+nv.getClass());
	}

	/**
	 * Lenient but vanilla convertor -- accepts String or any Number subclass.
	 * @param object Can be null (returns 0). Must be a simple number format - this method does NOT try
	 * to read e.g. "10%" or "5k" or "1,000".
	 * @return
	 * @see #getNumber(Object)
	 */
	public static double toNum(Object object) throws NumberFormatException {
		if (object==null) return 0;
		if (object instanceof Number) return ((Number) object).doubleValue();
		return Double.valueOf((String)object);
	}

	/**
	 * 
	 * @param value
	 * @return true if value is (not null) and a Number or a number or a String like "1"
	 * Note: false for char/Character.
	 * @see StrUtils#isNumber(String) which is equivalent for Strings
	 */
	public static boolean isNumber(Object value) {
		if (value==null) return false;
		Class<? extends Object> fClass = value.getClass();
		if (ReflectionUtils.isa(fClass, Number.class)) return true;
		if (value instanceof String) {
			return StrUtils.isNumber((String) value);
		}
		if ( ! fClass.isPrimitive()) return false;
		return fClass==int.class || fClass==double.class || fClass==long.class
				|| fClass==float.class || fClass==short.class;
	}


	/**
	 * Lenient convertor -- accepts String or any Number subclass.
	 * @param object Can be null (returns 0)
	 */
	public static long toLong(Object object) {
		if (object==null) return 0;
		if (object instanceof Number) return ((Number) object).longValue();
		return Long.valueOf((String)object);
	}

	/**
	 * Remove NaN and infinity from data
	 * @param data
	 * @return A fresh copy of data without any NaNs
	 */
	public static double[] filterFinite(double[] data) {
		double[] ok = new double[data.length];
		int oki =0;
		for (int i = 0; i < data.length; i++) {
			double v = data[i];
			if (isFinite(v)) {
				ok[oki] = v;
				oki++;
			}
		}
		if (oki==ok.length) {
			return ok;
		}
		double[] ok2 = Arrays.copyOf(ok, oki);
		return ok2;
	}

	public static int sum(int[] values) {
		int total = 0;
		for (int d : values) {
			total += d;
		}
		return total;
	}

	public static double[] toDoubleArray(int[] vector) {
		double[] ds = new double[vector.length];
		for(int i=0; i<vector.length; i++) {
			ds[i] = vector[i];
		}
		return ds;
	}

	public static boolean isFinite(double[] xs) {
		for (double d : xs) {
			if ( ! isFinite(d)) return false;
		}
		return true;
	}

	/**
	 * @return The point at which double loses integer level precision.
	 * (actually, this is half the true value for safety).
	 */
	public static double getMaxIntWithDouble() {
		if (MAX_DOUBLE_INT != 0) return MAX_DOUBLE_INT;
		long step = 1000l * Integer.MAX_VALUE;		
		double mdi = 0;
		for(int i=0; i<100000000; i++) {
			double md1 = mdi + step;
			long step2 = step -1;
			double md2 = mdi + step2;
			if (md1 == md2) {
				step = step / 2;
				if (step==0) {
					break; // happens at i = ~4k
				}
				continue;
			}
			mdi = md1;
		}
		// allow a little leeway
		MAX_DOUBLE_INT = mdi / 2;
		assert MAX_DOUBLE_INT != mdi; 
		return MAX_DOUBLE_INT;
	}
	
}


final class NumberComparator implements Comparator<Number> {
	@Override
	public int compare(Number a, Number b) {
		if (a==null) {
			return b==null? 0 : -1;
		}
		if (b==null) {
			return a==null? 0 : 1;
		}
		// unavoidably ugly
		if (a instanceof BigInteger) {
			BigInteger bigB = b instanceof BigInteger ? (BigInteger) b
					: new BigInteger(String.valueOf(b.intValue())); // othewise 1.0 causes an error :(
			int c = ((BigInteger) a).compareTo(bigB);
			return c;
		}
		if (b instanceof BigInteger) {
			BigInteger bigA = a instanceof BigInteger ? (BigInteger) a
					: new BigInteger(String.valueOf(a.intValue()));
			int c = bigA.compareTo((BigInteger) b);
			return c;
		}
		if (a instanceof Long || b instanceof Long) {
			// Copied from the Java 7 Long.compare() method
			long al = a.longValue(), bl = b.longValue();
			return (al < bl) ? -1 : ((al == bl) ? 0 : 1);
		}			
		return Double.compare(a.doubleValue(), b.doubleValue());
	}		
}