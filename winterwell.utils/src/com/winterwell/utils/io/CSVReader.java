package com.winterwell.utils.io;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.winterwell.utils.BestOne;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.IOneShot;
import com.winterwell.utils.log.Log;

/**
 * Support for reading comma (etc.) separated values. This aims to be
 * "standards" compliant as per
 * http://en.wikipedia.org/wiki/Comma-separated_values
 *
 * It does not trim whitespace.
 *
 * Policy for handling incorrectly sized records is defined by
 * {@link #reportBadRecord(int, String[])}
 *
 * TODO: This is all geared up to work with UNIX line-breaks. Fix it to work
 * with Windows and Mac too.
 *
 * TODO refactor to use AbstractIterator TODO add skipTo(Time)
 *
 * @testedby  CSVReaderTest}
 * @author Daniel, Joe Halliwell <joe@winterwell.com>
 *
 */
public class CSVReader implements Iterable<String[]>, Iterator<String[]>, Closeable, IOneShot {

	public CSVSpec getSpec() {
		return spec;
	}
	
	/**
	 * Split using default CSV rules
	 * @param line
	 * @return
	 */
	public static final String[] split(String line) {
		CSVReader r = new CSVReader(new StringReader(line), new CSVSpec());
		if ( ! r.hasNext()) return null;
		String[] row = r.next();
		return row;
	}
	
	public final static char DEFAULT_DELIMITER_CHARACTER = ',';
	public final static char DEFAULT_COMMENT_CHARACTER = '#';
	public final static char DEFAULT_QUOTE_CHARACTER = '"';
	public final static int VARIABLE_NUMBER_OF_COLUMNS = -1;

	/**
	 * Lines beginning with this will be skipped
	 * @param comment e.g. '#' Off by default (it's not part of the "standard"). 0 for off.
	 * @return 
	 */
	public CSVReader setCommentMarker(char comment) {
		this.spec.comment = comment;
		return this;
	}
	
	/**
	 * The starting line number of the current record
	 */
	int currentLineNumber = 0;

	final PushbackReader input;
	
	/**
	 *  The starting line number of the next record (i.e.
	// handling comments)
	 */
	int nextLineNumber = 0; //


	/** record (i.e. handling multi-line records) */
	String[] nextRecord;

	/** Fields for keeping track of row and line counts
	NB scanLineNumber >= nextLineNumber >= currentLineNumber
	*/
	int nextRowNumber = 0;
	/**
	 * -1 for variable width mode. The starting value will get over-written by
	 * the constructor!
	 */
	int numFields = -1;
	
	/**
	 *  The starting line number of the next but one
	 */
	int scanLineNumber = 0;


	private File file;
	private CSVSpec spec;
	private String[] headers;
	private int badRecords;

	/**
	 * @return the file we're looking at. Can be null if this was created using
	 *         a Reader!
	 */
	public File getFile() {
		return file;
	}

	/**
	 * Create a CSVReader from a file with the default quote and comment
	 * characters.
	 *
	 * @param f
	 * @param delimiter
	 */
	public CSVReader(File f, char delimiter) {
		this(f, delimiter, DEFAULT_QUOTE_CHARACTER);
	}

	/**
	 * Create a CSVReader from a file, using the default comment character.
	 *
	 * @param file
	 * @param delimiter
	 * @param quote
	 */
	public CSVReader(File file, char delimiter, char quote) {
		this(FileUtils.getReader(file), delimiter, quote,
				DEFAULT_COMMENT_CHARACTER);
		this.file = file;
	}

	/**
	 * Full constructor for CSVReaders.
	 *
	 * @param input
	 * @param delimiter
	 * @param quote
	 * @param comment
	 *            lines beginning with this character will be ignored! 0 for off.
	 */
	public CSVReader(Reader input, char delimiter, char quote, char comment) {
		this(input, new CSVSpec(delimiter, quote, comment));
	}
	
	public CSVReader(File f, CSVSpec spec) {
		this(FileUtils.getReader(f), spec);
		this.file = f;
	}
	
	public CSVReader(Reader input, CSVSpec spec) {
		this.spec = spec;
		// Why pushback??
		this.input = new PushbackReader(input);
		try {
			nextRecord = getNextRecord();
			if (nextRecord != null) {
				numFields = nextRecord.length;
			}
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	public CSVReader(File f) {
		this(f, ',');
	}

	/**
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() {
		FileUtils.close(input);
	}

	/**
	 * The number of the line on which the last row returned began.
	 * Zero-indexed. This can be higher than the row-number, as it includes comments and
	 * multi-line rows.
	 */
	public int getLineNumber() {
		return currentLineNumber;
	}
	
	/**
	 * 
	 * @return count of bad records - compare this with {@link #getRowNumber()} which is the number of good records
	 */
	public int getBadRecordCount() {
		return badRecords;
	}

	/**
	 * Pull in the next record of the correct length.
	 *
	 * @return
	 * @throws IOException
	 */
	private String[] getNextGoodRecord() throws IOException {
		String[] record;
		while (true) {
			record = getNextRecord();
			if (record == null)
				return null;
			if (isGoodRecord(record))
				return record;
			reportBadRecord(nextLineNumber, record);
		}
	}

	/**
	 * Pull in the next record line (possibly ignoring several comments)
	 *
	 * @return
	 * @throws IOException
	 */
	private String[] getNextRecord() throws IOException {
		boolean inQuote = false;
		ArrayList<String> row = new ArrayList<String>(); // NB: Using a size hint doesn't help much
		StringBuilder currentField = new StringBuilder();

		int c = input.read();

		// Nothing left
		if (c == -1)
			return null;

		// Ignore comment lines (but increment line counter)
		while (c == spec.comment) {
			// read to line end
			while (!(c == -1 || c == '\n')) {
				c = input.read();
			}
			c = input.read();
			nextLineNumber++;
			scanLineNumber++;
		}

		// The logic here is a bit deep, but hopefully clear
		while (c != -1) {
			// Not in quotes
			if (!inQuote) {
				if (c == spec.delimiter) {
					row.add(currentField.toString());
					currentField = new StringBuilder();
					// NB currentField.setLength(0); is slower in my (JH) tests
				} else if (c == '\n') { // TODO mac/windows formats
					row.add(currentField.toString());
					break;
				} else if (c == spec.quote) {
					inQuote = true;
				} else {
					currentField.append((char) c);
				}
			}
			// In quotes
			else {
				if (c == spec.quote) {
					// Effectively peek the next char (via PushbackReader)
					int d = input.read();
					if (d == spec.quote) {
						currentField.append(spec.quote);
					} else {
						inQuote = false;
						if (d != -1) {
							input.unread(d);
						}
					}
				} else {
					if (c == '\n') { // TODO mac/windows formats
						scanLineNumber++;
					}
					currentField.append((char) c);
				}
			}
			c = input.read();
		}
		if (c == -1) {
			row.add(currentField.toString());
		}
		scanLineNumber++;
		return row.toArray(new String[0]);
	}

	/**
	 * The expected number of fields in a record. -1 if in variable width mode.
	 *
	 * @return
	 */
	public int getNumFields() {
		return numFields;
	}

	/**
	 * The number of the last row returned. zero indexed (-1 at the beginning). This may be less than the line number due to comments and
	 * multi-line items
	 *
	 * @see #getLineNumber()
	 */
	public int getRowNumber() {
		return nextRowNumber - 1;
	}

	/**
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return nextRecord != null;
	}

	private boolean isGoodRecord(String[] record) {
		assert record != null;
		if (numFields == -1)
			return true;
		return record.length == numFields;
	}

	// Interface implementations

	/**
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<String[]> iterator() {
		return this;
	}

	/**
	 * @see java.util.Iterator#next()
	 */
	@Override
	public String[] next() {
		if (nextRecord == null)
			throw new NoSuchElementException();
		try {
			currentLineNumber = nextLineNumber;
			nextLineNumber = scanLineNumber;
			String[] record = nextRecord;
			nextRecord = getNextGoodRecord();
			nextRowNumber++;
			// If record is good return it...
			if (isGoodRecord(record)) {
				return record;
			}
			// ...otherwise someone has poked something, so let's try looking
			// again
			return next();
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	/**
	 * @see java.util.Iterator#remove()
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Override to change the behaviour. Default is to print a log message, but
	 * carry on without Exception.
	 *
	 * @param lineNumber
	 * @param record
	 */
	public void reportBadRecord(int lineNumber, String[] record) {
		// ignore empty rows
		if (record.length == 0 || record.length == 1
				&& Utils.isBlank(record[0])) {
			return;
		}
		badRecords++;
		Log.w("csv",
				"Bad record at line " + lineNumber + ": "
						+ StrUtils.ellipsize(Printer.toString(record), 36)
						+ (file == null ? "" : " in " + file));
	}

	@Override
	public String toString() {
		return "CSVReader" + (file == null ? "" : "[" + file + "]");
	}

	/**
	 * Set the expected number of fields in a record. -1 to set variable width
	 * mode. Changing this value mid-way through an iteration may result in odd
	 * behaviour.
	 * 
	 * Default: Guess from the first line! 
	 *
	 * @param numFields
	 * @return this
	 */
	public CSVReader setNumFields(int numFields) {
		this.numFields = numFields;
		return this;
	}

	/**
	 * Status: Experimental!
	 * @param csv
	 * @return
	 */
	public static char guessSeparator(File csv) {
		// Should we use \t or ,
		BestOne<Character> sep = new BestOne<Character>();
		for(char s : new char[]{',', '\t', '|'}) {
			CSVReader r1 = new CSVReader(csv, s);
			int i = 0;
			double score = 0;
			for (String[] rows : r1) {
				i++;
				// TODO + for consistent between rows, and not too high
				score += rows.length;
				if (i>=4) break;
			}
			r1.close();
			sep.maybeSet(s, score);
		}
		return sep.getBest();
	}

	/**
	 * e.g. if the first row is headers `csvReader.setHeaders(csvReader.next())`
	 * @param rowOfHeaders
	 */
	public void setHeaders(String[] rowOfHeaders) {
		this.headers = rowOfHeaders;
		for (int i=0; i<headers.length; i++) {
			headers[i] = headers[i].trim();
		}	
	}
	
	public List<String> getHeaders() {
		return Arrays.asList(headers);
	}

	/**
	 * Use headers to convert each row into a map.
	 * 
	 * If {@link #setHeaders(String[])} has not been called, the first row will be used (and consumed) by default.
	 * 
	 * @return
	 */
	public Iterable<Map<String,String>> asListOfMaps() {
		if (headers==null) {
			assert getRowNumber() == -1 : "No headers set, and beyond 1st row "+file;
			// Use first row as headers
			setHeaders(next());
		}
		final CSVReader r = this;
		
		return () -> new AbstractIterator() {
			@Override
			protected Object next2() throws Exception {
				if ( ! r.hasNext()) return null;
				String[] row = r.next();
				Map rmap = new ArrayMap();
				for (int i=0; i<headers.length; i++) {
					String hi = headers[i];
					String ri = row[i];
					if (ri != null) ri = ri.strip();
					rmap.put(hi, ri);
				}
				return rmap;
			}
		};
	}
	

}
