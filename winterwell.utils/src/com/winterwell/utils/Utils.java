package com.winterwell.utils;

import java.io.File;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.io.FastByteArrayInputStream;
import com.winterwell.utils.io.FastByteArrayOutputStream;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.XStreamUtils;

/**
 * @testedby {@link UtilsTest}
 * @author daniel
 * 
 */
public class Utils {

	private static final char[] consonants = "bcdfghjklmnpqrstvwxz"
			.toCharArray();
	private static final AtomicInteger id = new AtomicInteger(1);
	private final static Pattern notBlank = Pattern.compile("\\S");

	/**
	 * Note Random is thread safe. Is using it across threads a bottleneck? If
	 * you need repeatable results, you should create your own Random where you
	 * can control both the seed and the usage.
	 */
	private static final Random r = new Random();

	private static final char[] vowels = "aeiouy".toCharArray();

	/**
	 * Check inputs are all non-null
	 * 
	 * @param args
	 * @throws NullPointerException
	 *             if any of args are null
	 */
	public static void check4null(Object... args) throws NullPointerException {
		for (int i = 0; i < args.length; i++) {
			if (args[i] == null)
				throw new NullPointerException("Argument " + i + " in "
						+ Printer.toString(args));
		}
	}

	/**
	 * A version of {@link Comparable#compareTo(Object)} for <i>any</i> two
	 * objects. Arguments can be null (which come last).
	 * 
	 * @param a
	 * @param b
	 * @return equivalent to a.compareTo(b) if that makes sense. So 0 for
	 *         same-rank, -1 for a-before-b, and 1 for b-before-a.
	 */
	public static int compare(Object a, Object b) {
		if (a == b)
			return 0;
		if (a == null)
			return 1;
		if (b == null)
			return -1;
		try {
			return ((Comparable) a).compareTo(b);
		} catch (ClassCastException e) {
			int ahash = a.hashCode();
			int bhash = b.hashCode();
			if (ahash == bhash)
				return 0;
			return ahash < bhash ? -1 : 1;
		}
	}

	/**
	 * Perform a DEEP copy of the object, using XStream
	 * 
	 * @param object
	 * @return a copy of object, which should share no structure
	 * @testedby {@link UtilsTest#testCopy()}
	 */
	public static <X> X copy(X object) {
		FastByteArrayOutputStream out = new FastByteArrayOutputStream();
		XStreamUtils.xstream().toXML(object, out);
		FastByteArrayInputStream in = new FastByteArrayInputStream(
				out.getByteArray(), out.getSize());
		Object copy = XStreamUtils.xstream().fromXML(in);
		return (X) copy;
	}

	/**
	 * Convenience for a==null? b==null : a.equals(b) + special handling for arrays -- using List.equals() rules.
	 * <p>
	 * Beware: numerical values of different classes are NOT tested for
	 * numerical equivalence. E.g. 1 != 1L != 1.0. You must cast them into
	 * yourself if you need this behaviour. Note: experimented with this, but
	 * there are issues, e.g. with large longs vs large doubles
	 * 
	 * @param a
	 * @param b
	 * @return true if a.equals(b)
	 */
	public static boolean equals(Object a, Object b) {
		if (a == null)
			return b == null;
		if (a.getClass().isArray() && b.getClass().isArray()) {			
			List<Object> al = Containers.asList(a);
			List<Object> bl = Containers.asList(b);
//			boolean e = a.equals(b); This will be false!
			boolean el = al.equals(bl);
			return el;
		}
		return a.equals(b);
	}

	/**
	 * 
	 * @param f
	 * @return time-of-commit, SHA1-key
	 */
	public static Pair2<Time, String> getGitRevision(File f) {
		// git log --shortstat -n 1
		Proc p = new Proc("git log --shortstat -n 1 " + f.getAbsolutePath());
		p.run();
		p.waitFor();
		String output = p.getOutput();
		String[] tBits = StrUtils.find("Date:\\s*(.+)", output);
		String[] cBits = StrUtils.find("commit\\s*(\\w+)", output);
		assert tBits != null && tBits.length > 1 : f.getAbsolutePath() + "\n"
				+ output;
		assert cBits != null && cBits.length > 1 : f.getAbsolutePath() + "\n"
				+ output;
		Time time = new Time(tBits[1].trim());
		String key = cBits[1];
		return new Pair2<Time, String>(time, key);
	}

	/**
	 * @return a number, starting with 1 and incremented each time. This is
	 *         guaranteed to be unique _within this run of the jVM_, upto
	 *         overflow.
	 * 
	 * @see #getUID()
	 */
	public static int getId() {
		return id.getAndIncrement();
	}

	/**
	 * @return lower case string for the operating system. E.g. ??
	 */
	public static String getOperatingSystem() {
		String osName = System.getProperty("os.name");
		return osName.toLowerCase();
	}

	/**
	 * Load a password from HOME/.winterwell/password
	 */
	public static String getPassword() {
		String home = System.getProperty("user.home");
		File pwdf = new File(home, ".winterwell/password");
		String pwd = FileUtils.read(pwdf).trim();
		return pwd;
	}

	/**
	 * @return a Random instance for generating random numbers. This is to avoid
	 *         generating new Random instances for each number as the results
	 *         aren't well distributed.
	 *         <p>
	 *         If you need repeatable results, you should create your own
	 *         Random.
	 *         <p>
	 *         Note: Java's Random <i>is</i> thread safe, and can be used by
	 *         many threads - although for high simultaneous usage, you may wish
	 *         to create your own Randoms.
	 */
	public static Random getRandom() {
		return r;
	}

	/**
	 * @param prob
	 * @return true with P(prob)
	 */
	public static boolean getRandomChoice(double prob) {
		assert MathUtils.isProb(prob) : prob;
		if (prob == 0)
			return false;
		if (prob == 1)
			return true;
		return getRandom().nextDouble() < prob;
	}

	/**
	 * Pick an element using uniform random choice
	 * 
	 * @param <X>
	 * @param list
	 * @return a random member, or null if list is empty or null
	 */
	public static <X> X getRandomMember(Collection<X> list) {
		if (list==null || list.isEmpty()) return null;
		int i = getRandom().nextInt(list.size());
		return Containers.get(list, i);
	}

	/**
	 * Return a set of (uniformly) randomly selected elements of the specified
	 * collection. Equality in the set is determined in the usual way i.e. by
	 * calling equals(). If num is greater than choices, then return all the
	 * choices in a fresh object.
	 * 
	 * @param num Can be 0, but not lower (throws IllegalArgumentException)
	 * @param choices
	 * @return set of randomly selected choices.
	 */
	public static <X> Collection<X> getRandomSelection(int num,
			Collection<X> choices) {
		return getRandomSelection(num, choices, r);
	}
	
	
	public static <X> Collection<X> getRandomSelection(int num, Collection<X> choices, Random rnd) 
	{
		if (num >= choices.size())
			return new HashSet<X>(choices);
		if (num < 0)
			throw new IllegalArgumentException("Num must be >= 0");
		List<X> listChoices = Containers.getList(choices);
		Set<X> selected = new HashSet<X>(num);
		int iter = 0;
		while (selected.size() < num) {
			int i = rnd.nextInt(choices.size());
			X x = listChoices.get(i);
			selected.add(x);
			// time out if we get stuck
			iter++;
			if (iter == num * 100) {
				Log.w("utils", "Breaking out of random-selection loop early: Failed to select a full "
						+ num + " from " + choices);
				break;
			}
		}
		return selected;
	}

	/**
	 * A random lower case string
	 * 
	 * @param len
	 * @return
	 */
	public static String getRandomString(int len) {
		Random rnd = getRandom();
		char[] s = new char[len];
		for (int i = 0; i < len; i++) {
			// roughly consonant-consonant-vowel for pronounceability
			char c;
			if (rnd.nextInt(3) == 0) {
				c = vowels[rnd.nextInt(vowels.length)];
			} else {
				c = consonants[rnd.nextInt(consonants.length)];
			}
			s[i] = c;
		}
		return new String(s);
	}

	/**
	 * The original exception within a nested exception. Has some knowledge of
	 * SQLExceptions
	 * 
	 * @param e
	 *            Any exception
	 * @return the root cause, or e if it is the root cause. Never null.
	 */
	public static Throwable getRootCause(Throwable e) {
		// Chained SQL exceptions?
		// SQL exceptions are horrible - they hide their true cause.
		if (e instanceof SQLException) {
			SQLException ex = (SQLException) e;
			SQLException ex2 = ex.getNextException();
			if (ex2 != null)
				return getRootCause(ex2);
		}
		Throwable cause = e.getCause();
		if (cause == null || cause == e)
			return e;
		return getRootCause(cause);
	}

	/**
	 * Unwrap a potentially nested exception until it matches one of the given
	 * causes (or not).
	 * 
	 * @param e
	 *            Any exception
	 * @param causes
	 *            Must not be empty, or you'll always get null back!
	 * @return The first cause to match one of the given classes, or null.
	 * @see #getRootCause(Throwable)
	 */
	public static Exception getCause(Throwable e, Class... causes) {
		while (e != null) {
			Class<? extends Throwable> ec = e.getClass();
			for (Class<? extends Exception> klass : causes) {
				if (ReflectionUtils.isa(ec, klass)) {
					return (Exception) e;
				}
			}
			e = e.getCause();
		}
		return null;
	}

	@Deprecated
	// since we don't use SVN anymore. Much
	public static int getSVNRevision(File f) {
		// workaround: svn can be a bit slow - which returns early & blank!
		for (int sleep : new int[] { 100, 2000 }) {
			Proc p = new Proc("svn info " + f.getAbsolutePath());
			p.run();
			Utils.sleep(sleep);
			p.waitFor();
			String output = p.getOutput();
			if (!output.contains("Revision")) {
				continue;
			}
			String[] bits = StrUtils.find("Revision:\\s*(\\d+)", output);
			assert bits != null && bits.length > 1 : f.getAbsolutePath() + " "
					+ output;
			return Integer.valueOf(bits[1]);
		}
		throw new FailureException("svn info request failed");
	}

	/**
	 * @return an id that shouldn't be in use anywhere else (inc. on any
	 *         server).
	 * @see #getId()
	 */
	public static String getUID() {
		return getRandomString(6)
				+ Long.toHexString(System.currentTimeMillis());
	}

	/**
	 * @param line
	 * @return true if line is null, empty, or contains nothing but whitespace.
	 * 
	 * ??Should this also catch "null" (it doesn't currently)??
	 */
	public static boolean isBlank(CharSequence line) {
		if (line == null || line.length() == 0)
			return true;
		Matcher m = notBlank.matcher(line);
		boolean nb = m.find();
		return !nb;
	}

	/**
	 * Convenience for null or []
	 * 
	 * @param list
	 * @return true if list == null || list.isEmpty()
	 */
	public static boolean isEmpty(Collection list) {
		return list == null || list.isEmpty();
	}

	/**
	 * Convenience for null or {}
	 * 
	 * @param map Can be null
	 * @return true if null or empty.
	 */
	public static boolean isEmpty(Map map) {
		return map == null || map.isEmpty();
	}

	public static boolean isInt(String matchid) {
		try {
			Integer.valueOf(matchid);
			return true;
		} catch (Exception e) {
			return false;
		}

	}

	/**
	 * Generalized version of Math.max
	 */
	public static <T extends Comparable<T>> T max(T a, T b) {
		if (a.compareTo(b) <= 0)
			return b;
		return a;
	}

	/**
	 * Generalized version of Math.min
	 */
	public static <T extends Comparable<T>> T min(T a, T b) {
		if (a.compareTo(b) <= 0)
			return a;
		return b;
	}

	/**
	 * Convenience filter.
	 * 
	 * @param objects
	 *            Can be null
	 * @return first non-null non-blank object (zero-length CharSequences count
	 *         as blank!), or null if all are null.
	 */
	// Sadly no lazy evaluation, so this is less useful than it's lisp
	// counterpart.
	public static <X> X or(X... objects) {
		if (objects == null)
			return null;
		for (X object : objects) {
			if (object == null) {
				continue;
			}
			if (object instanceof CharSequence
					&& ((CharSequence) object).length() == 0) {
				continue;
			}
			return object;
		}
		return null;
	}

	/**
	 * @return true for linux or unix
	 */
	public static boolean OSisUnix() {
		String os = getOperatingSystem();
		return os.contains("linux") || os.contains("unix");
	}

	/**
	 * Includes some special case handling for SQL exceptions, to make debugging
	 * a bit less painful (trys to avoid the "SQLException caused by: see next
	 * exception, which you can't do now, mwhaha" message).
	 * 
	 * @param e Should not be null (although that isn't enforced to avoid error-on-error badness)
	 * @return
	 */
	public static RuntimeException runtime(Throwable e) {
		if (e instanceof RuntimeException)
			return (RuntimeException) e;
		// SQL exceptions are horrible - throw the cause instead
		if (e instanceof SQLException) {
			e = getRootCause(e);
		}
		// get some more info out of Hibernate... would be nice but the
		// dependencies are horrible
		// if (e instanceof JDBCException) e.getSQL()
		return new WrappedException(e);
	}

	/**
	 * Sleep. Converts InterruptedExceptions into unchecked wrapped exceptions.
	 * 
	 * @param millis
	 */
	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * A fairly lenient boolean parser. Accepts booleans, the Strings true, on,
	 * 1 as true, and false, off, 0 as false.
	 * 
	 * Convenience for {@link #yes(Object, boolean)} with strict=true
	 * 
	 * @param on
	 *            Can be a Boolean or a String. Can be null (returns false).
	 *            Cannot be just a random string though.
	 * @return true or false
	 * @throws IllegalArgumentException
	 */
	public static boolean yes(Object on) {
		return yes(on, true);
	}
	
	/**
	 * Convenience for on!=null && on
	 * @param on
	 * @return true/false
	 */
	public static boolean yes(Boolean on) {
		return on!=null && on;
	}
	

	/**
	 * Dubious convenience method. ??keep or delete??
	 * @param off
	 * @return ! yes
	 */
	public static boolean no(Object off) {
		return ! yes(off);
	}

	/**
	 * A lenient boolean parser, similar to JavaScript's if() test (but even
	 * more lenient).<p>
	 * 
	 * false: boolean false, the Strings "", false, off, null, 0, the
	 * number 0, an empty list, an empty array, an empty map.<br>
	 * true: anything else (see the strict parameter)
	 * 
	 * @param on
	 *            Can be a Boolean, a String, a Collection or an array. Can be null (returns false).
	 * @param strict
	 *            If false, allow random strings => true. If true, throw an
	 *            exception for unrecognised inputs.
	 * @return true or false
	 * @throws IllegalArgumentException
	 */
	public static boolean yes(Object on, boolean strict) {
		if (on == null)
			return false;
		if (on instanceof Boolean)
			return (Boolean) on;
		if (on instanceof Number) {
			return ((Number) on).doubleValue() != 0;
		}
		if (on instanceof Collection) {
			return ! ((Collection) on).isEmpty();
		}
		if (on.getClass().isArray()) {
			return Array.getLength(on) != 0;
		}
		if (on instanceof Map) {
			return ! ((Map) on).isEmpty();
		}
		String s = on.toString().trim();
		// "on" will be sent by a value-less checkbox
		// - eg if the sending form is hand-written html
		if (s.equals("true") || s.equals("on") || s.equals("1"))
			return Boolean.TRUE;
		// "off" shouldn't happen, but just in case
		if (s.isEmpty() || s.equals("false") || s.equals("off")
				|| s.equals("0") || s.equals("null"))
			return Boolean.FALSE;
		if (strict)
			throw new IllegalArgumentException("Invalid boolean value: " + s);
		// lenient mode: Treat any old String as true.
		return true;
	}

	/**
	 * Convenience for {@link #sleep(long)}.
	 * @param n
	 * @param unit
	 */
	public static void sleep(int n, TUnit unit) {
		sleep(n*unit.getMillisecs());
	}

	/**
	 * Convenience for {@link #sleep(long)}.
	 */
	public static void sleep(Dt dt) {
		sleep(dt.getMillisecs());		
	}
	
	/**
	 * Convenience for {@link #sleep(long)}.
	 */
	public static void sleep(TUnit dt) {
		sleep(dt.getMillisecs());		
	}

	/**
	 * A convenience -- helps when comparing could-be-null values.
	 * @param a Can be null
	 * @param b Can be null
	 * @return true if these values are toString() equals, or both null. 
	 */
	public static boolean streq(Object a, Object b) {
		return String.valueOf(a).equals(String.valueOf(b));
	}

	/**
	 * WARNING: Differs from javascript truthiness!
	 * @param x
	 * @return false if x is null, false, 0, an empty collection or map, or a blank String or CharSequence.
	 * All other objects return true.
	 */
	public static boolean truthy(Object x) {		
		if (x==null) return false;
		if (x instanceof Boolean) {
			return (Boolean) x;
		}
		if (x instanceof Double) {
			double v = (double) x;
			if (Double.isNaN(v)) return false;
		}
		if (x instanceof Number) {
			if (((Number) x).doubleValue() == 0) return false;
		}
		if (x instanceof Collection) {
			return ! ((Collection) x).isEmpty();
		}
		if (x instanceof Map) {
			return ! ((Map) x).isEmpty();
		}
		if (x instanceof CharSequence) {
			return ! isBlank((CharSequence) x);
		}
		return true;
	}

	public static String getExceptionMessage(Throwable ex) {
		if (ex==null || ex.getMessage()==null) return "";
		return ex.getMessage();
	}


}
