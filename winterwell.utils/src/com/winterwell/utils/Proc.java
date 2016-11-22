package com.winterwell.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.time.Dt;

/**
 * Convenience wrapper for the {@link java.lang.Process} class. Makes life more
 * pleasant and less buggy.
 * <p>
 * <i>Warning:</i> A common error is to think Java Process acts like a shell
 * interpreter. It does not. It runs a single executable (a program or script).
 * Things like pipes, redirects, and chained commands will not work. Try
 * {@link ShellScript} for experimental support of shell-script style commands.
 * </p>
 * See http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html for some
 * motivating examples.
 *
 * @author daniel
 *
 *         NB - named Proc to make it easy to distinguish from java.lang.Process
 * @testedby {@link ProcTest}
 */
public class Proc implements Closeable {
	
	public void clearOutput() {
		if (out!=null) {
			out.clearString();
		}
		if (err!=null) {
			err.clearString();
		}
	}
	
	boolean closed;

	private final String command;

	private boolean echo;

	private StreamGobbler err;

	private StreamGobbler out;
	private final ProcessBuilder pb;

	private java.lang.Process process;

	public Proc(List<String> command) {		
		pb = new ProcessBuilder(command);
		this.command = StrUtils.join(command, " ");
	}

	public Proc(String command) {
		this.command = command;
		List<String> parsed = parse(command);
		pb = new ProcessBuilder(parsed);
	}

	/**
	 * Close the process' 3 pipes in, out and error. See
	 * http://www.vnoel.com/Computers
	 * -Related-Issues/Java-problem-Too-many-open-files.html Experimentation
	 * suggests this may not be a problem after all see
	 * {@link ProcTest#testOpenFilesIssue()}
	 */
	private void closeStreams() {
		if (err != null) {
			err.pleaseStop();
			// err = null;
		}
		if (out != null) {
			out.pleaseStop();
			// out = null;
		}
		if (process == null)
			return;
		FileUtils.close(process.getInputStream());
		FileUtils.close(process.getOutputStream());
		FileUtils.close(process.getErrorStream());
	}

	/**
	 * Kills the subprocess. The subprocess represented by this
	 * <code>Process</code> object is forcibly terminated.
	 */
	public void destroy() {
		if (process == null)
			return;
		closeStreams();
		process.destroy();
		process = null;
	}

	/**
	 * Equivalent to {@link #destroy()}
	 */
	@Override
	public void close() {
		destroy();
	}

	/**
	 * Ensure that process streams are closed. Does not terminate the process!
	 *
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		closeStreams();
	}

	/**
	 * WARNING quoting and encoding may not be quite right!
	 * @return
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Returns a string map view of this process builder's environment. The
	 * returned object may be modified using ordinary {@link java.util.Map Map}
	 * operations.
	 *
	 * @see ProcessBuilder#environment()
	 */
	public Map<String, String> getEnvironment() {
		return pb.environment();
	}

	/**
	 * @return the process's error output, or "" if the redirectErrorStream has
	 *         been set to true. Never null
	 */
	public String getError() {
		return err == null ? "" : err.getString();
	}

	/**
	 * @return Can be empty, never null
	 */
	public String getOutput() {
		return out.getString();
	}

	/**
	 * @deprecated Depends on poking around inside the Process object's private
	 *             parts. Can throw exceptions.
	 * @return The UNIX process id (pid), or 0 on failure
	 */
	@Deprecated
	public int getProcessId() {
		Integer id = ReflectionUtils.getPrivateField(process, "pid");
		return id;		
	}

	/**
	 * Split a string representation of a command up into the base call and respective arguments. Handles
	 * OS-specific hacks and other unpleasantries.
	 *
	 * Double-quoted arguments are handled as per the shell i.e. cmd "hello world" -> ["cmd", "hello world"]
	 *
	 * Other forms of shell quoting are *NOT* supported.
	 *
	 * @param cmnd
	 * @return E.g. "ls -l foo*" would be ["ls", "-l", "foo*"]
	 */
	List<String> parse(String cmnd) {
		// support "quoted args" which should not be split up
		List<String> bits = new ArrayList<String>();
		StringBuilder bit = new StringBuilder();
		boolean inQuotes = false;
		for (char c : cmnd.toCharArray()) {
			if (inQuotes) {
				if (c == '"') {
					inQuotes = false;
					continue;
				}
				bit.append(c);
				continue;
			}
			if (Character.isWhitespace(c)) {
				if (bit.length() != 0) {
					bits.add(bit.toString());
					bit = new StringBuilder();
				}
				continue;
			}
			if (c == '"') {
				inQuotes = true;
				continue; // Skip the quote char
			}
			bit.append(c);
		}
		if (bit.length() != 0) {
			bits.add(bit.toString());
		}
		// Windows requires we call cmd.exe
		String osName = System.getProperty("os.name");
		if (osName.equals("Windows 95")) {
			ArrayList<String> wbits = new ArrayList<String>(bits.size() + 2);
			wbits.add("command.com");
			wbits.add("/C");
			wbits.addAll(bits);
			bits = wbits;
		} else if (osName.startsWith("Windows")) {
			ArrayList<String> wbits = new ArrayList<String>(bits.size() + 2);
			wbits.add("cmd.exe");
			wbits.add("/C");
			wbits.addAll(bits);
			bits = wbits;
		}
		// Done
		return bits;
	}

	/**
	 * Execute the process. Returns immediately - use {@link #waitFor(long)} to
	 * block.
	 * @return this
	 *
	 * @throws IOException
	 */
	public Proc start() {
		try {
			process = pb.start();

			// Gobble output or Java hangs!
			out = new StreamGobbler(process.getInputStream());
			if (echo) {
				out.setEcho(true);
			}
			out.start();
			if (!pb.redirectErrorStream()) {
				err = new StreamGobbler(process.getErrorStream());
				if (echo) {
					err.setEcho(true);
				}
				err.start();
			}
			return this;
		} catch (IOException e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * Set the working directory.
	 *
	 * @param dir
	 *            Can be null for the JVM's working dir.
	 * @see ProcessBuilder#directory(File)
	 */
	public void setDirectory(File dir) {
		pb.directory(dir);
	}

	/**
	 * false by default. If true, also send all output to system out
	 *
	 * @param echo
	 */
	public void setEcho(boolean echo) {
		assert process == null : "do earlier!";
		this.echo = echo;
	}

	/**
	 * If true, the error stream will be merged into the output stream.
	 *
	 * @see ProcessBuilder#redirectErrorStream(boolean).
	 */
	public void redirectErrorStream(boolean redirect) {
		pb.redirectErrorStream(redirect);
	}
	

	/**
	 * @return a (buffered UTF8 encoded) Writer for sending input to the process.
	 * <p>
	 * NB: this is the OutputStream (confusingly named here) of the underlying Process object. 
	 */
	public Writer getInput() {
		OutputStream in = process.getOutputStream();
		return FileUtils.getWriter(in);
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(command);
		if (process!=null) {
			try {
				sb.append(" pid: ");
				sb.append(getProcessId());
			} catch(Exception ex) {
				// oh well
			}
		}
		if (out!=null) {
			sb.append("\n");
			sb.append(StrUtils.ellipsize(out.getString(), 200));
		}
		return sb.toString();
	}

	/**
	 * Wait for the process to finish. If the thread is interrupted, this will
	 * destroy the process to ensure system resources are freed.
	 *
	 * FIXME: encountered bug in {@link Utils#getSVNRevision(File)} where this
	 * appears to return too quickly
	 *
	 * @return Exit code from the process, usually 0 for fine, other for error.
	 * @throws RuntimeException
	 *             if interrupted
	 */
	public int waitFor() {
		try {
			// attempting to patch a bug -- see ProcTest.testBug()
			Utils.sleep(3);
			int v = process.waitFor();
			closeStreams();
			return v;
		} catch (InterruptedException e) {
			// Destroy the process!
			// Otherwise it might hog system resources, and the gobbler threads
			// will stay hung forever
			destroy();
			throw Utils.runtime(e);
		}
	}

	/**
	 * Wait for the process to finish. If the timeout is exceeded then the
	 * process will be destroyed to release any resources.
	 *
	 * @param timeout
	 *            time in milliseconds to allow before giving up. 0 or -1
	 *            indicate to wait forever
	 * @return the exit code from the process. usually 0 for fine, other for
	 *         error.
	 * @throws wrapped
	 *             InterruptedException If the timeout is exceeded (or something
	 *             else interrupts).
	 */
	@SuppressWarnings("deprecation")
	public int waitFor(long timeout) {
		assert process != null : "No process. You must run a Proc before waiting on it";
		if (timeout < 1)
			return waitFor();
		try {
			// attempting to patch a bug -- see ProcessTest.testBug()
			Utils.sleep(5);
			TimeOut interrupter = new TimeOut(timeout);
			// Wait for it
			int v = process.waitFor();
			// Cancel that interruption
			interrupter.cancel();
			closeStreams();
			return v;
		} catch (InterruptedException e) {
			// Destroy the process!
			// Otherwise it might hog system resources, and the gobbler threads
			// will stay hung forever
			destroy();
			throw Utils.runtime(e);
		}
	}

	/**
	 * Convenience method for running a command, waiting for it to finish, &
	 * returning the output.
	 *
	 * @param command
	 * @return output from command
	 */
	public static String run(String command) {
		Proc p = new Proc(command);
		p.run();
		int ok = p.waitFor();
		return p.getOutput();
	}

	/**
	 * Wait for the process to finish. If the timeout is exceeded then the
	 * process will be destroyed to release any resources.
	 *
	 * @param timeout
	 *            time in milliseconds to allow before giving up. 0 or -1
	 *            indicate to wait forever
	 * @return the exit code from the process. usually 0 for fine, other for
	 *         error.
	 * @throws wrapped
	 *             InterruptedException If the timeout is exceeded (or something
	 *             else interrupts).
	 */
	public void waitFor(Dt dt) {
		waitFor(dt.getMillisecs());
	}

	/**
	 * @deprecated Equivalent to start() -- use that.
	 */
	public void run() {
		start();
	}

	@Deprecated
	public java.lang.Process getProcess() {
		return process;
	}

	public boolean isOutputting() {
		return out!=null && out.isAlive();
	}

	/**
	 * @deprecated
	 * WARNING Java's Process doesn't seem to need this?!
	 * @param string
	 * @return encoded so it will be treated exactly as-is.
	 * See http://wiki.bash-hackers.org/syntax/quoting
	 */
	public static String bashEncodeStrong(String string) {
		// escape 's
		string = string.replace("'", "'\\''");
		return "'"+string+"'";
	}

}
