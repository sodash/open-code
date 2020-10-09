package com.winterwell.utils.log;

import java.io.Closeable;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import com.winterwell.utils.Dep;
import com.winterwell.utils.IFilter;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils;

/**
 * Pipe log reports out to a file.
 * <p>
 * Reports are written and flushed immediately. This is not the most efficient
 * thing, but it guarantees that the log will not lose the reports leading up to
 * a crash (ie. the important ones).
 * <p>
 * LogFile's stay alive until they are closed! Use {@link #close()} to remove
 * this LogFile from the log listeners.
 * 
 * @author daniel
 * @testedby  LogFileTest}
 */
public class LogFile implements ILogListener, Closeable {

	private final File file;

	Time nextRotation;

	int rotationHistory;

	Dt rotationInterval;

	/**
	 * Create a .log file named after the calling class. Will append if the file
	 * already exists.
	 * <p>
	 * This is a wrapper for {@link #LogFile(File)}.
	 */
	public LogFile() {
		this(new File(ReflectionUtils.getCaller().getClassName() + ".log"));
	}
	
	IFilter<Report> filter;

	/**
	 * See {@link LogConfig#fileMaxSize}
	 */
	private long fileMaxSize;
	
	public LogFile setFilter(IFilter<Report> filter) {
		this.filter = filter;
		return this;
	}
	

	/**
	 * Create a log-listener and attach it to the Log.
	 * 
	 * @param f
	 */
	public LogFile(File f) {
		file = f;
		if (file.getParentFile() != null) {
			file.getParentFile().mkdirs();
		}
		Log.addListener(this);
		// settings form config?
		LogConfig lc = Dep.has(LogConfig.class)? Dep.get(LogConfig.class) : new LogConfig();
		if (lc.fileHistory!=null && lc.fileInterval!=null) {
			setLogRotation(lc.fileInterval, lc.fileHistory);
		}
		if (lc.fileMaxSize!=null) {
			setFileMaxSize(ConfigBuilder.bytesFromString(lc.fileMaxSize));
		}
	}

	private void setFileMaxSize(long maxSize) {
		fileMaxSize = maxSize;
	}


	/**
	 * Delete all log entries from the file. The file will still exist but it
	 * will be empty.
	 */
	public void clear() {
		FileUtils.write(file, "");
	}

	/**
	 * Stop listening to log events
	 */
	@Override
	public void close() {
		Log.removeListener(this);
	}

	public File getFile() {
		return file;
	}

	@Override
	public void listen(Report report) {
		if (filter!=null) {
			try {
				if ( ! filter.accept(report)) {
					return; // skip it
				} else {
					assert true; // keep it (this line is for breakpointing)
				}
			} catch(Throwable ex) {
				// bugger!
				if ( ! report.toString().contains("Filter failed!")) {
					Log.e("log", "Filter failed! "+ex+" from "+filter+" for "+report);
				}
			}
		}
		String line = listen2_lineFromReport(report);
		listen2(line, report.getTime());
	}


	private String listen2_lineFromReport(Report report) {
//		String lines = report.toString();
		// Use Java SimpleFormatter to make LogStash happy out of the box
		LogRecord lr = new LogRecord(report.level, report.tag+" "+report.getMessage()
									+" "+report.context+" "+serverName);
//		lr.setThreadID(report.threadId);
		lr.setMillis(report.getTime().getTime());
		lr.setThrown(report.ex);
		// thread as logger name?
		lr.setLoggerName(String.valueOf(report.thread));
		String lines = sf.format(lr);
		// a single line for each report to make it easier to grep
		String line = lines.replaceAll("[\r\n]", " ") + "\n";
		return line;
	}
	
	static final String serverName = WebUtils.hostname();
	
	SimpleFormatter sf = new SimpleFormatter();

	private transient boolean criedForHelp;
	
	/**
	 * Low-level faster writing. 
	 * @param line
	 * @param time
	 */
	public synchronized void listen2(String line, Time time) {
		// too big?!
		if (fileMaxSize > 0 && file.length() > fileMaxSize) {
			// one final log message
			if ( ! criedForHelp) {
				// ??minor: possibly refactor Log so this can use guaranteed the same Report construction
				String tooBigLine = "Log file too big: "+file.length()+" > "+fileMaxSize+". Logging skipped!";
				Report report = new Report("log", tooBigLine, Level.SEVERE, line, null);
				String cry = listen2_lineFromReport(report);
				FileUtils.append(cry, file);
				criedForHelp = true;
			}
			// done
			return;
		}
		// Rotate the logs?
		if (nextRotation != null && nextRotation.isBefore(time)) {
			rotateLogFiles();
		}
		// append to file (flushes immediately)
		FileUtils.append(line, file);
		criedForHelp = false;
	}

	/**
	 * Move all the log files down one.
	 */
	private synchronized void rotateLogFiles() {
		// advance the trigger
		nextRotation = nextRotation.plus(rotationInterval);
		// just nuke the current log?
		if (rotationHistory < 1) {
			FileUtils.delete(file);
			return;
		}
		// rotate the old logs
		for (int i = rotationHistory - 1; i != 0; i--) {
			File src = new File(file.getAbsolutePath() + "." + i);
			File dest = new File(file.getAbsolutePath() + "." + (i + 1));
			if (src.exists()) {
				FileUtils.move(src, dest);
			} else {
				FileUtils.delete(dest);
			}
		}
		// move the current log
		File src = file;
		File dest = new File(file.getAbsolutePath() + ".1");
		if (src.exists()) {
			FileUtils.move(src, dest);
		}
	}

	/**
	 * By default, this class builds one giant log file. If this is set, logs
	 * will get rotated - but only if this JVM keeps running for long enough!
	 * 
	 * @param interval
	 *            How often to rotate
	 * @param history
	 *            How many old log files to keep. 0 means just the current one.
	 * @testedby  LogFileTest#testRotation()}
	 */
	public LogFile setLogRotation(Dt interval, int history) {
		this.rotationInterval = interval;
		this.rotationHistory = history;
		// FIXME how do we get the file created time?
		// ??TODO Round to the nearest interval, to avoid rotate-on-restart
		Time created = file.exists() ? new Time(file.lastModified())
				: new Time();
		nextRotation = created.plus(interval);
		return this;
	}

	@Override
	public String toString() {
		return "LogFile:" + file.getAbsolutePath();
	}

}
