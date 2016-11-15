package com.winterwell.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.winterwell.utils.io.FileUtils;

/**
 * Gobble output from a stream. Create then call start().
 * 
 * @author Based on code by Michael C. Daconta, published in
 *         http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps_p.html
 */
public class StreamGobbler extends Thread {
	/**
	 * If true, echo the gobbled output to System.out Can be useful for
	 * debugging.
	 */
	private boolean echo;

	private IOException ex;
	private final InputStream is;
	private volatile boolean stopFlag;
	private StringBuffer stringBuffer;

	public StreamGobbler(InputStream is) {
		super("gobbler:" + is.toString());
		setDaemon(true);
		this.is = is;
		stringBuffer = new StringBuffer();
	}

	/**
	 * Clear the stored string.
	 */
	public void clearString() {
		stringBuffer = new StringBuffer();
	}

	/**
	 * @return The string, as collected so far (i.e. since starting, or the last
	 *         call to clearString).
	 * @throws IORException
	 *             if the thread generated an IOException
	 */
	public String getString() throws WrappedException {
		if (ex != null)
			throw new WrappedException(ex);
		return stringBuffer.toString();
	}

	public boolean hasText() {
		return stringBuffer.length() != 0;
	}

	/**
	 * Request that the thread should finish. If the thread is hung waiting for
	 * output, then this will not work.
	 */
	public void pleaseStop() {
		FileUtils.close(is);
		stopFlag = true;
	}

	@Override
	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			while (!stopFlag) {
				// ?? This hangs and it seems even stop() won't kill the process
				int ich = br.read();
				if (ich == -1) {
					break; // End of stream
				}
				char ch = (char) ich;
				stringBuffer.append(ch);
				if (echo) {
					System.out.print(ch);
				}
			}
		} catch (IOException ioe) {
			if (stopFlag)
				// we were told to stop already so ignore
				return;
			ioe.printStackTrace();
			ex = ioe;
		}
	}

	public void setEcho(boolean echo) {
		this.echo = echo;
	}

	@Override
	public String toString() {
		return "StreamGobbler:" + stringBuffer.toString();
	}
}
