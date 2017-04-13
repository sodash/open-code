package com.winterwell.nlp.classifier;

import org.apache.commons.lang3.StringEscapeUtils;

import com.winterwell.utils.containers.Slice;

/**
 * A labelled Slice (text range).
 * 
 * WARNING: these do NOT serialise well using XStream! Suppose you have a large
 * doc with lots of slices... the output will repeat the base doc for every
 * slice, creating a giant file.
 * 
 * @see #appendXml(StringBuilder)
 * 
 * @author daniel
 * 
 * @param <X>
 */
public final class LSlice<X> extends Slice {

	private X label;

	public LSlice(CharSequence base, int start, int end, X label) {
		super(base, start, end);
		this.label = label;
	}

	/**
	 * Helper methof for saving to xml. This does NOT include the base String
	 * (to avoid the repeated inclusion of large documents).
	 * 
	 * @param sb
	 */
	public void appendXml(StringBuilder sb) {
		sb.append("<slice start='" + start + "' end='" + end + "'>");
		sb.append(StringEscapeUtils.escapeXml(getLabel().toString()));
		sb.append("</slice>\n");
	}

	public X getLabel() {
		return label;
	}

	public void setLabel(X label) {
		this.label = label;
	}

}
