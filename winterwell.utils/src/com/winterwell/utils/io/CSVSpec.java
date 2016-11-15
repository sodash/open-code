package com.winterwell.utils.io;

import java.io.File;

/**
 * What kind of CSV do you mean? Since CSV isn't quite a standard
 * @author daniel
 *
 */
public class CSVSpec {

	public static final char UNSET = 0;
	public char delimiter;
	public char quote;

	/**
	 * Defaults!
	 */
	public CSVSpec() {
		this(CSVReader.DEFAULT_DELIMITER_CHARACTER, CSVReader.DEFAULT_QUOTE_CHARACTER, CSVReader.DEFAULT_COMMENT_CHARACTER);
	}
	public CSVSpec(char delimiter, char quote, char comment) {
		this.delimiter = delimiter;
		this.quote = quote;
		this.comment = comment;
	}

	public char comment;

	public CSVSpec setCommentMarker(Character comment) {
		this.comment = comment==null? UNSET : comment;
		return this;
	}
	
	public CSVSpec setQuote(Character quote) {
		this.quote = quote==null? UNSET : quote;
		return this;
	}
	
	public CSVSpec setDelimiter(char delimiter) {
		this.delimiter = delimiter;
		return this;
	}
	
	public CSVWriter buildWriter(File out) {
		return new CSVWriter(FileUtils.getWriter(out), this);
	}
	public CSVReader buildReader(File f) {
		return new CSVReader(f, this);
	}

}
