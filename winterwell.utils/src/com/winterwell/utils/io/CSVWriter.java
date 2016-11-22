package com.winterwell.utils.io;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

import com.winterwell.utils.containers.Containers;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;

/**
 * Support for creating .csv files.
 * 
 * Implements "standard" CSV behaviour as per
 * http://en.wikipedia.org/wiki/Comma-separated_values
 * <p>
 * TODO is this thread safe?? depends on whether {@link BufferedWriter} is. if
 * needed maybe use a lock-free queue instead of a lock for high concurrency??
 * 
 * TODO: Add method for writing a comment, quote fields containing comment
 * chars??
 * 
 * @author daniel
 * @testedby {@link CSVWriterTest}
 */
public class CSVWriter implements Closeable, Flushable {

	/**
	 * Convenience method to write a list of rows out to a String in CSV format (comma separated, " encoded).
	 * @param rows
	 * @return csv
	 */
	public static String writeToString(Collection rows) {
		StringWriter sout = new StringWriter();
		CSVWriter w = new CSVWriter(sout, ',', '"');
		for (Object row : rows) {
			// class check to pick the correct write method
			if (row instanceof List) {
				w.write(row);
			} else if (row instanceof String[]) {
				w.write((String[])row);
			} else {
				w.write((Object[])row);
			}
		}
		w.close();
		return sout.toString();
	}
	
	private static BufferedWriter CSVWriter2_fileWriter(File file,
			boolean append) {
		try {
			return FileUtils.getWriter(new FileOutputStream(file, append));
		} catch (FileNotFoundException e) {
			throw Utils.runtime(e);
		}
	}

	private final CSVSpec spec;
	private File file;
	private CharSequence LINEEND = StrUtils.LINEEND;
	int linesWritten = 0;

	private BufferedWriter out;

	private final String quotedQuote;

	/**
	 * Create a CSV file with the standard double-quote quote character.
	 * 
	 * @param file
	 *            This will be overwritten if it does exist.
	 * @param delimiter
	 * @throws FileNotFoundException
	 */
	public CSVWriter(File file, char delimiter) {
		this(file, delimiter, '"', false);
	}

	/**
	 * Work with CSV file with the standard double-quote quote character.
	 * 
	 * @param file
	 * @param delimiter
	 * @throws FileNotFoundException
	 */
	public CSVWriter(File file, char delimiter, boolean append) {
		this(file, delimiter, '"', append);
	}

	public CSVWriter(File file, char delimiter, char quote)
			throws FileNotFoundException {
		this(file, delimiter, quote, false);
	}

	public CSVWriter(File file, char delimiter, char quote, boolean append) {
		this(file, new CSVSpec(delimiter, quote, CSVSpec.UNSET), append);		
	}

	public CSVWriter(Writer out, char delimiter, char quote) {
		this(out, new CSVSpec(delimiter, quote, CSVSpec.UNSET));
	}
	
	public CSVSpec getSpec() {
		return spec;
	}
	
	public CSVWriter(Writer out, CSVSpec spec) {
		Utils.check4null(out, spec);
		file = null;
		this.out = out instanceof BufferedWriter ? (BufferedWriter) out
				: new BufferedWriter(out);
		this.spec = spec;
		// Possibly this is too restrictive, but actually other values don't
		// really make sense
		assert spec.quote == '\'' || spec.quote == '"';
		this.quotedQuote = "" + spec.quote + spec.quote;
	}

	public CSVWriter(File dest, CSVSpec spec, boolean append) {
		this(CSVWriter2_fileWriter(dest, append), spec);
		this.file = dest;
	}

	/**
	 * Flush & close the underlying file writer
	 */
	@Override
	public void close() {
		FileUtils.close(out);
	}

	public void flush() {
		try {
			out.flush();
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	/**
	 * @return file (if created using the file constructor) or null. null does
	 *         not imply that this is not a file-based writer.
	 */
	public File getFile() {
		return file;
	}

	/**
	 * Set this writer to append to the end of an existing file. Must be called
	 * before any lines are written
	 * 
	 * @param append
	 */
	public void setAppend(boolean append) {
		assert linesWritten == 0;
		if (!append)
			return;
		try {
			out = FileUtils.getWriter(new FileOutputStream(file, true));
		} catch (FileNotFoundException e) {
			throw new WrappedException(e);
		}
	}

	public void setCommentMarker(char commentMarker) {
		setCommentMarker(Character.toString(commentMarker));
	}
	/**
	 * @param commentMarker
	 *            If set (eg to '#'), then items beginning with this character
	 *            will be quoted to avoid them being interpreted as comments at
	 *            the other end. 0 by default. Comment markers are not standard
	 *            csv to the extent that there is such a thing.
	 * @return 
	 */
	public CSVWriter setCommentMarker(String commentMarker) {
		this.spec.comment = commentMarker.charAt(0);
		// can be null, but "" is not allowed
		assert commentMarker==null || commentMarker.length()==1 : commentMarker;
		return this;
	}

	/**
	 * Change the default line-end. E.g. if you want to force M$ style \r\n
	 * output
	 * 
	 * @param lineEnd
	 */
	public void setLineEnd(CharSequence lineEnd) {
		LINEEND = lineEnd;
	}

	@Override
	public String toString() {
		return file == null ? getClass().getSimpleName() : getClass()
				.getSimpleName() + "[" + file + "]";
	}

	/**
	 * Convenience for {@link #write(Object[])}
	 * 
	 * @param line
	 */
	public void write(List line) {
		write(line.toArray());
	}

	/**
	 * Write out a row.
	 * 
	 * @param objects
	 *            These will be converted by {@link String#valueOf(Object)},
	 *            with escaping of the delimiter and the escape char. Quotes:
	 *            added if set, otherwise line-breaks are converted into spaces.
	 */
	public void write(Object... strings) {
		// defend against accidentally routing to the wrong method
		if (strings.length == 1 &&  strings[0].getClass().isArray()) {// instanceof String[]) {
			List<Object> array = Containers.asList(strings[0]);
			write(array);
			return;
		}
		String[] ss = new String[strings.length];
		for (int i = 0; i < strings.length; i++) {
			ss[i] = strings[i] == null ? null : String.valueOf(strings[i]);
		}
		write(ss);
	}

	/**
	 * Write out a row.
	 * 
	 * @param objects
	 *            These will be escaping for the delimiter and the escape char.
	 *            Quotes: added if set, otherwise line-breaks are converted into
	 *            spaces.<br>
	 *            No checking is done on the line-length. Can be length 0 to
	 *            output an empty line.
	 */
	public void write(String... strings) {
		linesWritten++;
		try {
			// empty line?
			if (strings.length == 0) {
				out.append(LINEEND);
				return;
			}
			StringBuilder sb = new StringBuilder();
			for (int i = 0, n = strings.length; i < n; i++) {
				String si = strings[i] == null ? "" : strings[i];

				// TODO: Add an option to suppress in-field line breaks
				// NB: Line breaking within a quote is okay per the standard

				// If field contains the delimiter, quote-char, newline, or
				// comment-char it must be quoted
				if (si.indexOf(spec.delimiter) != -1
						|| si.indexOf(spec.quote) != -1
						|| si.indexOf('\n') != -1
						|| (spec.comment != 0 && si.indexOf(spec.comment) != -1)) {
					// Quote character must be replaced by double quote
					si = si.replace(String.valueOf(spec.quote), quotedQuote);
					si = spec.quote + si + spec.quote;
				}

				sb.append(si);
				sb.append(spec.delimiter);
			}
			// remove final delimiter
			StrUtils.pop(sb, 1);
			sb.append(LINEEND);
			// write
			out.append(sb);
		} catch (IOException ex) {
			throw new WrappedException(ex);
		}
	}

	public void writeComment(String comment) {
		if (spec.comment == 0)
			throw new IllegalStateException(
					"You must specify a comment marker before writing comments");
		// ??
		if (comment.startsWith(String.valueOf(spec.comment))) {
			comment = comment.substring(1);
		}
		try {
			out.append(spec.comment);
			out.append(' ');
			out.append(comment);
			out.append(LINEEND);
		} catch (IOException e) {
			throw Utils.runtime(e);
		}
	}

}
