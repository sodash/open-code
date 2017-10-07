package com.winterwell.utils.log;

import java.io.Closeable;
import java.io.File;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import com.winterwell.utils.IFilter;
import com.winterwell.utils.ReflectionUtils;
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
 * @testedby {@link LogFileTest}
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
				}
			} catch(Throwable ex) {
				// bugger!
				if ( ! report.toString().contains("Filter failed!")) {
					Log.e("log", "Filter failed! "+ex+" from "+filter+" for "+report);
				}
			}
		}
//		String lines = report.toString();
		// Use Java SimpleFormatter to make LogStash happy out of the box
		LogRecord lr = new LogRecord(report.level, report.getMessage()
									+" "+report.context+" "+serverName);
//		lr.setThreadID(report.threadId);
		lr.setMillis(report.getTime().getTime());
		lr.setThrown(report.ex);
		// thread as logger name?
		lr.setLoggerName(String.valueOf(report.thread));
		String lines = sf.format(lr);
		// a single line for each report to make it easier to grep
		String line = lines.replaceAll("[\r\n]", " ") + "\n";
		listen2(line, report.getTime());
	}
	
	static final String serverName = WebUtils.hostname();
	
	SimpleFormatter sf = new SimpleFormatter();
	
	/**
	 * Low-level faster writing. 
	 * @param line
	 * @param time
	 */
	public synchronized void listen2(String line, Time time) {
		// Rotate the logs?
		if (nextRotation != null && nextRotation.isBefore(time)) {
			rotateLogFiles();
		}
		// append to file (flushes immediately)
		FileUtils.append(line, file);		
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
	 * @testedby {@link LogFileTest#testRotation()}
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
