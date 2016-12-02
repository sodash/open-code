package com.winterwell.utils.io;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import com.winterwell.utils.WrappedException;

/**
 * Read a file one line at a time. Can be used directly in for-each loops.
 * Closes the file when done. The lines returned do not have line-end
 * characters.
 * 
 * Essentially this is just a wrapper for {@link BufferedReader} making it
 * iterable.
 * 
 * @author daniel
 * 
 */
public class LineReader implements Iterable<String>, Iterator<String>,
		Closeable {

	private boolean fresh = true;
	private String line;
	private int lineNum = 0;
	private final BufferedReader reader;

	/**
	 * Read file one line at a time. Can be used directly in foreach loops.
	 * Closes the file when done. The lines read do not have line-end
	 * characters.
	 * 
	 * @param file
	 * @throws IORException
	 *             if file does not exist or cannot be read
	 */
	public LineReader(File file) throws WrappedException {
		this(FileUtils.getReader(file));
		// this.file = file;
	}

	public LineReader(Reader reader) {
		this.reader = reader instanceof BufferedReader ? (BufferedReader) reader
				: new BufferedReader(reader);
		// prep
		next();
	}

	/**
	 * Close the file, if it is still open. Logs-and-swallows any exception.
	 */
	@Override
	public void close() {
		FileUtils.close(reader);
	}

	/**
	 * Don't count on it, but close the file if still open.
	 */
	@Override
	protected void finalize() throws Throwable {
		close();
	}

	/**
	 * Line number of the last returned line. Indexed from 1 (so 0 indicates
	 * that the reader has not been used).
	 */
	public int getLineNum() {
		return lineNum;
	}

	@Override
	public boolean hasNext() {
		return line != null;
	}

	/**
	 * Hack to allow continued use of a non-fresh LineReader
	 * 
	 * @param fresh
	 */
	public void setFresh(boolean fresh) {
		this.fresh = fresh;
	}

	@Override
	public Iterator<String> iterator() {
		assert fresh;
		fresh = false;
		return this;
	}

	@Override
	public String next() {
		String next = line;
		lineNum++;
		try {
			line = reader.readLine();
			if (line == null) {
				// close the stream
				close();
			}
			return next;
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	public String peekNext() {
		return line;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return "LineReader"; // +file;
	}
}
