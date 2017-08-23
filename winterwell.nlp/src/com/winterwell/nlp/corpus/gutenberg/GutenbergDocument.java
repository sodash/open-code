package com.winterwell.nlp.corpus.gutenberg;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.utils.IProperties;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Slice;
import com.winterwell.utils.io.FileUtils;

/**
 * Whitespace separated, word/tag format
 * 
 * @author daniel
 * 
 */
public class GutenbergDocument implements IDocument {

	private String rawTxt;

	public GutenbergDocument(File file) {
		rawTxt = FileUtils.read(file);
	}

	public GutenbergDocument(String rawText) {
		this.rawTxt = rawText;
	}
	
	@Override
	public String getTitleAndContents() {
		return StrUtils.joinWithSkip("\n\n", getName(), getContents());
	}

	
	@Override
	public String getLang() {
		return null;
	}

	@Override
	public String getAuthor() {
		Pattern p = Pattern.compile("^Author:\\s*(.+)$", Pattern.MULTILINE);
		Matcher m = p.matcher(rawTxt);
		boolean ok = m.find();
		if (!ok)
			return null;
		return m.group(1).trim();
	}

	@Override
	public String getContents() {
		// strip out Project Gutenberg header & footer
		String[] lines = StrUtils.splitLines(rawTxt);
		// last early gb
		int start;
		for (start = 200; start >= 0; start--) {
			if (lines[start].toLowerCase().contains("gutenberg")) {
				break;
			}
		}
		// first late gb
		int end;
		for (end = 300; end < lines.length; end++) {
			if (lines[end].toLowerCase().contains("gutenberg")) {
				break;
			}
		}
		// build the text
		StringBuilder sb = new StringBuilder();
		for (int i = start + 1; i < end; i++) {
			sb.append(lines[i]);
			sb.append(StrUtils.LINEEND);
		}
		return sb.toString();
	}

	@Override
	public Slice getFocalRegion() {
		return null;
	}

	@Override
	public IProperties getMetaData() {
		return null;
	}

	@Override
	public String getName() {
		Pattern p = Pattern.compile("^Title:\\s*(.+)$", Pattern.MULTILINE);
		Matcher m = p.matcher(rawTxt);
		boolean ok = m.find();
		if (!ok)
			return null;
		return m.group(1).trim();
	}

	@Override
	public ITokenStream getTokenStream() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return StrUtils.ellipsize("# " + getName() + "\n" + getContents(), 500);
	}

}
