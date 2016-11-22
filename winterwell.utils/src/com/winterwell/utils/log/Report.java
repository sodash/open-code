package com.winterwell.utils.log;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.logging.Level;

import com.winterwell.utils.Printer;
import com.winterwell.utils.time.Time;

/**
 * A log report (just a simple time + message + level).
 * 
 * @author daniel
 * 
 */
public final class Report implements Serializable {

	private static final long serialVersionUID = 1L;
	/**
	 * NB: Only set by exceptions
	 */
	private String details = "";
	public final Level level;
	private final String msg;
	/**
	 * The object behind the message (can be handy to keep it for listeners).
	 */
	public final Object ref;

	/**
	 * NB: does not start with a # -- that's added by toString().
	 */
	public final String tag;
	private final Time time = new Time();

	public Report(String tag, Exception ex) {
		this.tag = tag;
		ref = ex;
		msg = ex.getMessage();
		level = Level.SEVERE;
		StringWriter sw = new StringWriter();
		PrintWriter w = new PrintWriter(sw);
		ex.printStackTrace(w);
		details = sw.toString();
	}

	public Report(String tag, Object ref, String msg, Level level) {
		this.tag = tag;
		this.msg = msg;
		this.level = level;
		this.ref = ref;
	}

	public String getMessage() {
		return msg;
	}

	public Time getTime() {
		return time;
	}

	public final Thread thread = Thread.currentThread();
	
	/**
	 * [time] level #tag message details context thread
	 */
	@Override
	public String toString() {
		// Convert tabs, so we lines are nicely tab-aligned
		// Assumes: level & tag don't have tabs, and after message we don't care
		String _msg = msg.replace('\t', ' ');
		return // Environment.get().get(Printer.INDENT)+
		Printer.format("[{0}]\t{1}\t#{2}\t{3}\t{4}\t{5}\t{6}", 
				time, level, tag, _msg, details, Log.getContextMessage(), thread);
	}
	
	/** 
	 * A shorter String, for conserving log file space at the cost of details.
	 * @return time tag message
	 */
	public String toStringShort() {
		// Convert tabs, so we lines are nicely tab-aligned
		// Assumes: level & tag don't have tabs, and after message we don't care
		String _msg = msg.replace('\t', ' ');
		return "["+time+"]\t\t#"+tag+"\t"+_msg+"\n";
	}

}
