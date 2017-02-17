package com.winterwell.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.w3c.dom.Node;

import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;


/**
 * Pretty-print stuff
 * <p>
 * Do NOT use this for serialisation! The format may change according to
 * aesthetic judgement. Numbers may be rounded off.
 * 
 * @testedby {@link PrinterTest}
 * @author daniel
 * 
 */
public class Printer {

	/**
	 * Interface for serialise-to-string plugins.
	 */
	public static interface IPrinter<X> {
		void append(final X obj, final StringBuilder sb);
	}

	/**
	 * used to spot Arrays.asList() lists
	 */
	private static final Class ArraysListType = Arrays.asList().getClass();

	// TODO ! round to two/three significant figures
	// TODO add in ,s for 1,000
	private static final DecimalFormat df = new DecimalFormat("#.##");

	// /**
	// * An indent to be applied to the beginning of each line.
	// */
	// public static final Key<String> INDENT = new
	// Key<String>("Printer.indent");

	private static final int MAX_ITEMS = 120;

	static final Pattern n = Pattern.compile("[1-9]");

	private static final IPrinter PLAIN_TO_STRING = new IPrinter() {
		@Override
		public void append(final Object obj, final StringBuilder sb) {
			sb.append(obj);
		}

		@Override
		public String toString() {
			return "PLAIN_TO_STRING";
		};
	};

	private static boolean useListMarkers;

	static final Map<Class, IPrinter> useMe = new ConcurrentHashMap<>();
	
	public static <X> void setPrinter(Class<X> klass, IPrinter<X> printer) {
		useMe.put(klass, printer);
	}

	/**
	 * Use with e.g. Containers#apply()
	 */
	public static final IFn<Object, String> FN = new IFn<Object, String>() {
		@Override
		public String apply(Object value) {
			return Printer.toString(value);
		}
	};

	// static {
	// Environment.putDefault(INDENT, "");
	// }

	/**
	 * @deprecated Add a (thread-local) indent.
	 * 
	 * @param indent
	 */
	public static void addIndent(String indent) {
		// Environment env = Environment.get();
		// String oldindent = env.get(Printer.INDENT);
		// String newIndent = oldindent + indent;
		// env.put(Printer.INDENT, newIndent);
	}

	private static void append(Object x, StringBuilder sb) {
		if (x == null)
			return;
		// special case: string or stringbuilder
		if (x instanceof CharSequence) {
			sb.append((CharSequence) x);
			return;
		}
		// List-like things
		if (x.getClass().isArray()) {
			x = new ArrayList(Containers.asList(x));
		} else if (x instanceof Enumeration && ( ! (x instanceof List))) {
			x = Containers.getList((Enumeration)x);
		}
		// already know what to do?
		IPrinter prntr = useMe.get(x.getClass());
		if (prntr != null) {
			prntr.append(x, sb);
			return;
		}
		// Note: these have their own toString, but we don;t want to use it
		if (x instanceof Number) {
			sb.append(toStringNumber((Number) x));
			return;
		}
		// common collection types (ignore their toString()s)
		if (x instanceof ArrayList || x instanceof HashSet
				|| x instanceof ArraySet || x.getClass() == ArraysListType) {
			append(sb, (Collection) x, ", ");
			return;
		}
		if (x instanceof HashMap || x instanceof ArrayMap) {
			append(sb, (Map) x, StrUtils.LINEEND
			// + Environment.get().get(INDENT)
					, ": ", "{}");
			return;
		}
		// own toString? Beware - this can lead to infinite loops!
		if (ReflectionUtils.hasMethod(x.getClass(), "toString")) {
			useMe.put(x.getClass(), PLAIN_TO_STRING);
			sb.append(x.toString());
			return;
		}
		if (x instanceof Iterable) {
			x = Containers.getList((Iterable) x);
		}
		if (x instanceof Enumeration) { // old school iterators. yuck
			x = Collections.list((Enumeration) x);
		}
		if (x instanceof Collection) {
			append(sb, (Collection) x, ", ");
			return;
		}
		if (x instanceof Map) {
			append(sb, (Map) x, StrUtils.LINEEND
			// + Environment.get().get(INDENT)
					, ": ", "{}");
			return;
		}
		if (x instanceof Exception) {
			sb.append(toString((Exception) x, true));
			return;
		}
		if (x instanceof Node) {
			Node node = (Node) x;
			sb.append("<" + node.getNodeName() + ">" + node.getTextContent()
					+ "</" + node.getNodeName() + ">");
			return;
		}
		if (x instanceof Class) {
			// simple names are best for most uses
			String sn = ((Class) x).getSimpleName();
			// anonymous classes have no simple name
			if (sn == null)
				sn = x.toString();
			sb.append(sn);
			return;
		}
		// oh well
		sb.append(x);
	}

	/**
	 * 
	 * @param sb
	 * @param list
	 *            Skips over nulls
	 * @param separator
	 * @return sb for convenience
	 */
	public static StringBuilder append(StringBuilder sb, Collection list,
			String separator) {
		// TODO add in []s??
		boolean added = false;
		if (useListMarkers) {
			sb.append('[');
		}
		for (Object y : list) {
			if (y == null) {
				// Just skip null elements!
				continue;
			}
			added = true;
			if (y == list) {
				sb.append("(this Collection)");
				continue;
			}
			append(y, sb);
			sb.append(separator);
		}
		if (added) {
			StrUtils.pop(sb, separator.length());
		}
		if (useListMarkers) {
			sb.append(']');
		}
		return sb;
	}

	/**
	 * Append a string representation of the provided map. If the map contains
	 * more than MAX_ITEMS items, the surplus will be *silently* ignored. FIXME:
	 * Add ellipsis or something to indicate that some entries have been
	 * dropped!
	 * 
	 * @param sb
	 * @param x
	 * @param entrySeparator
	 * @param keyValueSeparator
	 * @param startEnd
	 *            start and end chars, e.g. "{}" or "[]" or null
	 * @return e.g. "{hello: world, foo: bar}" with ", " and ": " as separators
	 */
	public static void append(StringBuilder sb, Map x, String entrySeparator,
			String keyValueSeparator, String startEnd) {
		assert startEnd == null || startEnd.length() == 0
				|| startEnd.length() == 2;
		List keys = new ArrayList(x.keySet());
		if (keys.size() > MAX_ITEMS) {
			keys = keys.subList(0, MAX_ITEMS);
		}

		if (startEnd != null && startEnd.length() > 0) {
			sb.append(startEnd.charAt(0));
		}
		for (Object k : keys) {
			sb.append(toString(k));
			sb.append(keyValueSeparator);
			sb.append(toString(x.get(k)));
			sb.append(entrySeparator);
		}
		if ( ! keys.isEmpty()) {
			StrUtils.pop(sb, entrySeparator.length());
		}
		if (startEnd != null && startEnd.length() > 1) {
			sb.append(startEnd.charAt(1));
		}
	}

	/**
	 * Based on {@link MessageFormat}, but simpler. Use {0}, {1}, ... to
	 * reference variables message is appended to result, with args substituted
	 * into message using {@link #toString(Object)} to convert them. TODO make
	 * this more efficient
	 * 
	 * @param result
	 * @param message
	 * @param args
	 */
	public static void appendFormat(StringBuilder result, String message,
			Object... args) {
		for (int i = 0; i < args.length; i++) {
			// could be more efficient
			message = message.replace("{" + i + "}", toString(args[i]));
		}
		result.append(message);
	}

	/**
	 * Based on {@link MessageFormat}, but simpler though less efficient.
	 * 
	 * @param message Using {0}, {1}, ... to reference arguments into an array. OR {key} to reference
	 * values in a map
	 * @param args Array or Map
	 * @return message with args substituted in, using {@link #toString(Object)}
	 *         to convert them
	 */
	public static String format(String message, Object... args) {
		// map based?
		if (args.length==1 && args[0] instanceof Map) {
			Map<String,?> map = (Map) args[0];
			for(String k : map.keySet()) {
				assert ! k.contains("}") && ! k.contains("{") : k+" from "+map;
				// could be more efficient!
				message = message.replace("{" + k + "}", toString(map.get(k)));
			}
		} else {
			// array based
			for (int i = 0; i < args.length; i++) {
				// could be more efficient!
				message = message.replace("{" + i + "}", toString(args[i]));
			}
		}
		return message;
	}

	public static void formatOut(String message, Object... args) {
		String fm = format(message, args);
		System.out.println(fm);
	}

	public static String out(Object... x) {
		String s = toString(x);
		System.out.println(s);
		return s;
	}
	public static String out(String... x) {
		String s = toString(x);
		System.out.println(toString(x));
		return s;
	}

	/**
	 * @param x
	 * @return e.g. 1201.01 converts to 1,200
	 * @testedby {@link PrinterTest#testPrettyNumber()}
	 */
	public static String prettyNumber(double x) {
		return prettyNumber(x, 3);
	}

	/**
	 * @param x
	 * @return e.g. 1201.01 converts to 1,200
	 * @testedby {@link PrinterTest#testPrettyNumber()}
	 */
	public static String prettyNumber(double x, int sigFigs) {
		// millions?
		if (x >= 1000000)
			return StrUtils.toNSigFigs(x / 1000000, sigFigs) + " million";
		if (x >= 1000) {
			String s = StrUtils.toNSigFigs(x, sigFigs);
			x = Double.valueOf(s);
			// insert commas
			DecimalFormat f = new DecimalFormat("###,###");
			return f.format(x);
		}
		// zero-ish?
		if (MathUtils.equalish(x, 0)) {
			return "0";
		}
		return StrUtils.toNSigFigs(x, sigFigs);
	}

	@Deprecated
	public static void removeIndent(String indent) {
		// Environment env = Environment.get();
		// String oldindent = env.get(Printer.INDENT);
		// assert oldindent.endsWith(indent);
		// String newIndent = oldindent.substring(0,
		// oldindent.length() - indent.length());
		// env.put(Printer.INDENT, newIndent);
	}

	/**
	 * This is ugly code.
	 * 
	 * @param useListMarkers
	 *            If true, lists get wrapped with []. If false, they're just ,
	 *            separated. false by default.
	 */
	public static void setUseListMarkers(boolean useListMarkers) {
		Printer.useListMarkers = useListMarkers;
	}

	// TODO optional cap on number of elements printed
	/**
	 * Filters out nulls
	 */
	public static String toString(Collection list, String separator) {
		StringBuilder sb = new StringBuilder();
		append(sb, list, separator);
		return sb.toString();
	}

	/**
	 * 
	 * @param x
	 * @param entrySeparator
	 * @param keyValueSeparator
	 * @return e.g. "{hello: world, foo: bar}" with ", " and ": " as separators
	 */
	public static String toString(Map x, String entrySeparator,
			String keyValueSeparator) {
		StringBuilder sb = new StringBuilder();
		append(sb, x, entrySeparator, keyValueSeparator, "{}");
		return sb.toString();
	}

	/**
	 * To get a nice str() function:<br>
	 * <code>import static com.winterwell.utils.Printer.str;</code> 
	 * 
	 * <p>
	 * Equivalent to {@link #toString()}, but with a Python/javascript style of name.
	 * 
	 * @param x Anything, including null.
	 * @return a sensible String for x, "" if x is null.
	 */
	public static String str(Object x) {
		return toString(x);
	}
	
	public static String toString(Object x) {
		if (x == null)
			return "";
		StringBuilder sb = new StringBuilder();
		append(x, sb);
		return sb.toString();
	}

	/**
	 * Some Throwable messages can be very long. So ellipsize at
	 * MAX_ERROR_MSG_LENGTH characters then add the detail after the stacktrace,
	 * to give it a better chance of surviving subsequent truncations. NB
	 * Stacktrace lines contain the message, so this isn't as useful as I'd
	 * hoped - JH
	 */
	static int MAX_ERROR_MSG_LENGTH = 200;

	/**
	 * 
	 * @param x Can be null (returns "")
	 * @param stacktrace
	 * @return
	 */
	public static String toString(Throwable x, boolean stacktrace) {
		if (x==null) return "";
		// Don't generally unwrap, but do unwrap our own wrapper
		if (x instanceof WrappedException) {
			x = x.getCause();
		}
		if ( ! stacktrace)
			return x.getMessage() == null ? x.getClass().getSimpleName() : x
					.getClass().getSimpleName() + ": " + x.getMessage();
		// NB: the use of StringWriter here means there's little point having an
		// append-to-StringBuilder version of this method
		StringWriter w = new StringWriter();
		w.append(x.getClass() + ": "
				+ StrUtils.ellipsize(x.getMessage(), MAX_ERROR_MSG_LENGTH)
				+ StrUtils.LINEEND
				// + Environment.get().get(INDENT)
				+ "\t");
		PrintWriter pw = new PrintWriter(w);
		x.printStackTrace(pw);
		pw.flush();
		FileUtils.close(pw);
		// // If the message got truncated, append it in full here
		// if (x.getMessage().length() > MAX_ERROR_MSG_LENGTH) {
		// w.append(StrUtils.LINEEND);
		// w.append("Full message: ");
		// w.append(x.getMessage());
		// }
		return w.toString();
	}

	/**
	 * Shrinks down to a float and will round to two decimal places if >= 1.
	 * Trailing zeros are removed. Do NOT use this for accurate storage of
	 * doubles!
	 * 
	 * @param x
	 * @return string rep for x
	 * @see StrUtils#toNSigFigs(double, int)
	 */
	public static String toStringNumber(Number x) {
		float f = x.floatValue();
		if (f == Math.round(f))
			return Integer.toString((int) f);
		if (Math.abs(f) >= 1)
			return df.format(f);
		// it's a decimal
		String fs = Float.toString(f);
		if (fs.contains("E"))
			return fs;
		// WTF?? -- DBW (yeah I wrote this, I know)
		String fss = StrUtils.substring(fs, 0, 5);
		if (n.matcher(fss).find())
			return fss;
		return fs;
	}

}
