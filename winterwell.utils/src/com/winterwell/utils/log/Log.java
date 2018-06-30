package com.winterwell.utils.log;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.winterwell.utils.Environment;
import com.winterwell.utils.IFilter;
import com.winterwell.utils.IFn;
import com.winterwell.utils.Key;
import com.winterwell.utils.Printer;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.ConfigBuilder;

/**
 * Yet another logging system. We use Android LogCat style commands, e.g.
 * <code>Log.e(tag, message)</code> to report an error.
 * <p>
 * Why?<br>
 * Simpler than Log4J, but without features such as "crashes when it fails to
 * find it's config file" or "classpath settings can cause versions to conflict and kill your JVM".
 * It's not that this is better than log4j / slf4j / java logging / etc., it's that it's small, simple and
 * never breaks.
 *
 * @testedby {@link LogFileTest}
 * @author daniel
 */
public class Log {
	
	/**
	 * tag = calling class's name
	 * @param msg
	 */
	public static void d(Object msg) {
		String cn = ReflectionUtils.getCallingClassSimpleName(1);
		d(cn, msg);
	}
	/**
	 * tag = calling class's name
	 * @param msg
	 */
	public static void i(Object msg) {
		String cn = ReflectionUtils.getCallingClassSimpleName(1);
		i(cn, msg);
	}
	
	/**
	 * An extra string which can be accessed by log listeners. Added to
	 * {@link LogFile}s. Usage: e.g. a high-level process wishes to include info
	 * in low-level reports.
	 */
	private static final Key<String> ENV_CONTEXT_MESSAGE = new Key<String>(
			"Log.context");

	private static Map<String,Level> sensitiveTags = new HashMap();

	private static ILogListener[] listeners = new ILogListener[0];

	/**
	 * Maximum length (in chars) of a single log report: 4k
	 */
	public static final int MAX_LENGTH = 1048 * 4;

	public static final Level WARNING = Level.WARNING;
	public static final Level ERROR = Level.SEVERE;
	public static final Level DEBUG = Level.FINE;
	public static final Level INFO = Level.INFO;
	public static final Level VERBOSE = Level.FINEST;
	public static final Level OFF = Level.OFF;	
	
	private static Level MIN_LEVEL = DEBUG;

	static {
		stdInit();
	}

	/**
	 * Listen for log reports
	 *
	 * @param listener
	 */
	public static synchronized void addListener(ILogListener listener) {
		assert listener != null;
		for (ILogListener l : listeners) {
			if (l.equals(listener))
				return;
		}
		listeners = Arrays.copyOf(listeners, listeners.length + 1);
		listeners[listeners.length - 1] = listener;
	}

	private static void stdInit() {
		// switch off logging during log init
		Level minLevel = MIN_LEVEL;
		setMinLevel(OFF);
		// Add a simple console output listener
		addListener(new ILogListener() {
			@Override
			public void listen(Report report) {
				Printer.out(// Environment.get().get(Printer.INDENT)+
				'#' + report.tag + " " + report.getMessage()
				+(report.ex==null? "" : report.getDetails()
						));
			}
		});

		// config
		try {
			config = new ConfigBuilder(new LogConfig())
					.set(new File("config/log.properties"))
					.get();
			setConfig(config);
		} catch(Throwable ex) {
			// How can we report this bad config issue? Only to std-error :(
			System.err.println(ex);
		} finally {
			setMinLevel(minLevel);
		}
	}
	
	static LogConfig config;

	private static IFilter<String> excludeFilter;

	/**
	 * Allows a config file change to downgrade severe reports (to stop unwanted alerts)
	 * @see LogConfig#downgrade
	 */
	private static IFilter<String> downgradeFilter;
	
	public static void setConfig(LogConfig config) {
		Log.config = config;
		sensitiveTags.clear();
		if (config.ignoretags!=null) {
			for(String tag : config.ignoretags) {
				setMinLevel(tag, OFF);
			}
		}
		if (config.verbosetags!=null) {
			for(String tag : config.verbosetags) {
				setMinLevel(tag, VERBOSE);
			}
		}
		// no blank patterns!
		config.exclude = Containers.filter(config.exclude, x -> ! Utils.isBlank(x));
		if (Utils.isEmpty(config.exclude)) {
			excludeFilter = null;
		} else {
			excludeFilter = new IFilter<String>() {
				@Override
				public boolean accept(String x) {
					if (Utils.isBlank(x)) return false;
					for(String s : config.exclude) {
						if (x.contains(s)) return true;
					}
					return false;
				}
			};
		}
		// no blank patterns!
		config.downgrade = Containers.filter(config.downgrade, x -> ! Utils.isBlank(x));
		if (Utils.isEmpty(config.downgrade)) {
			downgradeFilter = null;
		} else {
			downgradeFilter = new IFilter<String>() {
				@Override
				public boolean accept(String x) {
					if (Utils.isBlank(x)) return false;
					for(String s : config.downgrade) {
						if (x.contains(s)) return true;
					}
					return false;
				}
			};
		}
		// all set (let's log that)
		Log.i("log", "setConfig "+ReflectionUtils.getSomeStack(10));
	}

	/**
	 * @return extra contextual message, or "" if unset
	 */
	static String getContextMessage() {
		String cm = Environment.get().get(Log.ENV_CONTEXT_MESSAGE);
		return cm == null ? "" : cm;
	}

	/**
	 * Get the minimum level to report events. Events with this level are
	 * reported. Events below this level are ignored.<br>
	 * Default: ignore verbose
	 * @param tag Can be null. You can set some tags to be extra sensitive
	 */
	public static Level getMinLevel(String tag) {
		if (tag!=null) {
			Level ml = sensitiveTags.get(tag);
			if (ml!=null) return ml;
		}
		return MIN_LEVEL;
	}

	/**
	 * @param listener Can be null
	 */
	public static synchronized void removeListener(ILogListener listener) {
		if (listener==null) return;
		ArrayList<ILogListener> ls = new ArrayList(Arrays.asList(listeners));
		ls.remove(listener);
		listeners = ls.toArray(new ILogListener[0]);
	}

	@Deprecated
	public static void report(Object msg) {
		if (!(msg instanceof Throwable)) {
			report(msg, Level.WARNING);
		} else {
			report((Throwable) msg);
		}
	}

	@Deprecated
	public static void report(Object msg, Level error) {
		report(null, msg, error, null);
	}

	/**
	 * This is the "master" version of this method (to which the others delegate
	 * - so perhaps that makes it more the servant method?).
	 * <p>
	 * It should never throw an exception. Any exceptions will be swallowed.
	 *
	 * @param tag
	 *            Inspired by Android's LogCat. The tag is a rough
	 *            classification on the report, which allows for
	 *            simple-but-effective filtering. Can be null
	 * @param msg
	 * @param error
	 */
	static void report(String tag, Object msg, Level error, Throwable ex) {
		// Ignore?
		Level minLevel = getMinLevel(tag);
		if (minLevel.intValue() > error.intValue())
			return;
		// null tag? Put in the calling class.method
		if (tag == null) {
			StackTraceElement ste = ReflectionUtils.getCaller(Log.class
					.getName());
			tag = ' ' + ste.toString(); // add a space from the # to make these
										// clickable from the Eclipse console
		}

		String smsg = Printer.toString(msg);
		String msgText;
		// Exception? Add in some stack
		if (msg instanceof Throwable) {
			msgText = Printer.toString((Throwable)msg, true);
			if (ex==null) ex = (Throwable) msg;
		} else {
			msgText = Printer.toString(msg);			
		}
		// Guard against giant objects getting put into log, which is almost
		// certainly a careless error
		if (msgText.length() > MAX_LENGTH) {
			msgText = msgText.substring(0, MAX_LENGTH - 100)
					+ "... (message is too long for Log!)";
//			System.err.println(new IllegalArgumentException(
//					"Log message too long: " + msgText));
		}
		// exclude or downgrade?
		if (excludeFilter!=null) {
			// tag or message ??should this be report.toString()
			if (excludeFilter.accept(tag)) {
				return;
			}
			if (excludeFilter.accept(smsg)) {
				return;
			}
		}		
		if (downgradeFilter!=null && error.intValue() > Level.INFO.intValue()) {
			if (downgradeFilter.accept(smsg)) {
				error = Level.INFO;
			} else if (downgradeFilter.accept(tag)) {
				error = Level.INFO;
			}
		}
		// make a Report
		Report report = new Report(tag, smsg, error, msgText, ex);
		// Note: using an array for listeners avoids any concurrent-mod
		// exceptions
		for (ILogListener listener : listeners) {
			try {
				listener.listen(report);
			} catch (Throwable ex2) {
				// swallow if something goes wrong
				ex2.printStackTrace();
			}
		}
		// HACK escalate on error + #escalate?
		if (error==Level.SEVERE && msgText.contains("#escalate")) {
			escalate(new WeirdException("Escalating "+msgText));
		}
	}

	@Deprecated
	public static void report(String tag, Object msg, Level error) {
		report(tag,msg,error,null);
	}
	
	@Deprecated
	public static void report(Throwable ex) {
		report(Printer.toString(ex, true), Level.SEVERE);
	}

	public static void setContextMessage(String message) {
		Environment.get().put(Log.ENV_CONTEXT_MESSAGE, message);
	}

	/**
	 * Set *default* minimum level to report events. Applies across all threads.
	 *
	 * @param level
	 *            DEBUG by default. Use Level.ALL to show everything. Events equal to or above this are reported.
	 */
	public static void setMinLevel(Level level) {
		assert level != null;
		MIN_LEVEL = level;
	}


	/**
	 * For pain-level debugging.
	 * <p>
	 * This prints out (via .v()):<br>
	 * class.method(file:linenumber): objects<br>
	 * It does so in a format which can be copied-and-pasted into Eclipse's Java
	 * Stack Trace Console, where it will gain a link to the line of code.
	 * <p>
	 * Uses Level.FINEST -- which is ignored by default!!
	 *
	 * @param objects
	 *            Optional. These will be printed out. Can be empty.
	 */
	public static void trace(Object... objects) {
		if (MIN_LEVEL.intValue() > Level.FINEST.intValue())
			return;
		StackTraceElement caller = ReflectionUtils.getCaller();
		Log.v(caller.getClass().getSimpleName(), caller.getMethodName() + ": "
				+ Printer.toString(objects));
	}

	/**
	 * Does nothing. Provides an object if you need one - but all the methods
	 * are static.
	 */
	public Log() {
		// does nothing
	}

	/**
	 * Add a log message for a warning. Use Log.e for genuine errors.
	 * @param tag
	 * @param msg
	 */
	public static void w(String tag, Object msg) {
		report(tag, msg, Level.WARNING, null);
	}

	/**
	 * Add a Log message on error.
	 * @param tag
	 * @param msg - Note that msg here, can be a Throwable, and you'll get some stack
	 */
	public static void e(String tag, Object msg) {
		report(tag, msg, Level.SEVERE, null);
	}

	/**
	 * @deprecated
	 * This one logs the stack-trace too.
	 * @param tag
	 * @param msg
	 * @param t
	 */
	public static void st(String tag, Throwable t){
		report(tag + ".stacktracelog", Printer.toString(t, true), WARNING, t);
	}


	public static void i(String tag, Object msg) {
		report(tag, msg, INFO, null);
	}

	/**
	 * A debug report (uses Level.FINE)
	 *
	 * @param tag
	 * @param msg
	 */
	public static void d(String tag, Object msg) {
		report(tag, msg, DEBUG);
	}

	/**
	 * A verbose report (uses Level.FINEST -- which is ignored by default)
	 *
	 * @param tag
	 * @param msg
	 */
	public static void v(String tag, Object msg) {
		report(tag, msg, VERBOSE);
	}

	public static void v(String tag, Object... items) {
		report(tag, items, VERBOSE);
	}

	
	@Deprecated
	// use i()
	public static void info(String string) {
		i(null, string);
	}

	@Deprecated
	// use w()
	public static void warn(String string) {
		w(null, string);
	}

	public static String stackToString(Throwable throwable){
		StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
	}

	/**
	 * @deprecated
	 * Replace {class} and {method} with values obtained from reflection lookups.
	 * Convenience method for easy creation of log messages.
	 * <p>
	 * Note: This is not normally called by default (for performance & predictability).
	 *
	 * @param s Can be null (returns null)
	 * @return s'
	 */
	public static String format(String s) {
		if (s==null) return s;
		StackTraceElement c = ReflectionUtils.getCaller(Log.class.getName(), StrUtils.class.getName(), Printer.class.getName());
		String cn = c.getClassName();
		int i = cn.lastIndexOf('.');
		String sn = i==-1? cn : cn.substring(i+1);
		ArrayMap vars = new ArrayMap(
				"class", sn,
				"method", c.getMethodName());
		return Printer.format(s, vars);
	}

	/**
	 * By default, this throws the error!
	 * But you can override it to do something less drastic.
	 * <p>
	 * Example use-case: In development, you might throw errors, then in production you might handle things via logging/reporting.
	 * @param error
	 */
	public static void escalate(Throwable error) {
		if (error==null) return;
		ESCALATOR.apply(error);
	}
	
	static IFn<Throwable,Object> ESCALATOR = new ThrowIt();

	/**
	 * Change how {@link #escalate(Throwable)} functions.
	 * @param escalator
	 */
	public static void setEscalator(IFn<Throwable, Object> escalator) {
		ESCALATOR = escalator;
	}

	public static void setMinLevel(String tag, Level level) {
		// thread safe put
		HashMap map = new HashMap(sensitiveTags);
		map.put(tag, level);
		sensitiveTags = map;
	}
	
	public static LogConfig getConfig() {
		return config;
	}
	public static void w(Object warning) {
		String cn = ReflectionUtils.getCallingClassSimpleName(1);
		w(cn, warning);
	}
	public static void e(Object warning) {
		String cn = ReflectionUtils.getCallingClassSimpleName(1);
		e(cn, warning);
	}

}

class ThrowIt implements IFn<Throwable,Object> {

	@Override
	public Object apply(Throwable value) {
		throw Utils.runtime(value);
	}
	
}
