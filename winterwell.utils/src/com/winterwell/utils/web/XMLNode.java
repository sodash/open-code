package com.winterwell.utils.web;

import java.util.Map;

import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;

/**
 * For use with Tree to provide a simple DOM. See
 * {@link WebUtils#parseXmlToTree(String, boolean)}.
 * 
 * @author Daniel
 * 
 */
public final class XMLNode {

	private final Map<String, String> attributes;
	private final String tag;
	private final String text;

	public XMLNode(String tag) {
		this.tag = tag;
		attributes = new ArrayMap<String, String>();
		text = null;
	}

	public XMLNode(String text, boolean isTextNode) {
		assert isTextNode;
		this.tag = null;
		this.attributes = null;
		this.text = text;
	}

	/**
	 * Convenience for getting an attribute.
	 * 
	 * @param attribute
	 * @return value or null. Never ""
	 */
	public String getAttribute(String attribute) {
		String v = attributes.get(attribute);
		return v == null || v.length() == 0 ? null : v;
	}

	public Map<String, String> getAttributes() {
		assert !isTextNode();
		return attributes;
	}

	/**
	 * null for text leaf nodes
	 */
	public String getTag() {
		return tag;
	}

	public String getText() {
		assert isTextNode();
		return text;
	}

	/**
	 * Convenience for testing getTag().equals(tag)
	 * 
	 * @param _tag
	 * @return true if this is a _tag
	 */
	public boolean is(String _tag) {
		return this.tag.equals(_tag);
	}

	public boolean isTextNode() {
		return tag == null;
	}

	@Override
	public String toString() {
		// TODO test does this make proper output?
		// include attributes
		String sa = "";
		if (attributes!=null && ! attributes.isEmpty()) {
			sa = " ";
			for(String k : attributes.keySet()) {
				String v = attributes.get(k);
				sa += k+"='"+v+"' ";
			}
//			sa = sa.trim();
		}
		// TODO children
		return "<" + tag +sa+">" + text + "</" + tag + ">";
	}
}
