package com.winterwell.nlp.corpus;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.utils.StrUtils;

import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.utils.IProperties;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Properties;
import com.winterwell.utils.containers.Slice;

/**
 * A simple version of {@link IDocument}. Note: This is not called Document to
 * avoid name clashes.
 * 
 * @author daniel
 * 
 */
public class SimpleDocument implements IDocument {

	private final String author;

	private Slice focalRegion;
	private final Properties props = new Properties();
	private final String text;
	private final String title;
	private List<String> tags = new ArrayList();
	
	public List<String> getTags() {
		return tags;
	}
	
	@Override
	public String getTitleAndContents() {
		return StrUtils.joinWithSkip("\n\n", getName(), getContents());
	}
	
	public SimpleDocument addTag(String tag) {
		if ( ! tags.contains(tag)) {
			tags.add(tag);
		}
		return this;
	}

	@Override
	public String getLang() {
		return null;
	}
	
	public SimpleDocument(String text) {
		this(null, text, null);
	}

	/**
	 * 
	 * @param title
	 *            Can be null
	 * @param text
	 * @param author
	 *            Can be null
	 */
	public SimpleDocument(String title, String text, String author) {
		assert text != null;
		this.text = text;
		this.title = title;
		this.author = author;
	}

	@Override
	public String getAuthor() {
		return author;
	}

	@Override
	public String getContents() {
		return text;
	}

	@Override
	public Slice getFocalRegion() {
		return focalRegion;
	}

	@Override
	public IProperties getMetaData() {
		return props;
	}

	@Override
	public String getName() {
		return title;
	}

	@Override
	public ITokenStream getTokenStream() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public void setFocalRegion(int start, int end) {
		this.focalRegion = new Slice(text, start, end);
	}

	@Override
	public String toString() {
		return Utils.isBlank(title) ? text : "# " + title + "\n\n" + text;
	}
}
