package com.winterwell.utils.web;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

/**
 * A convenience wrapper for writing XML docs saxily FIXME: All the methods
 * should do appropriate escaping
 */
public class SimpleXml {

	StringBuilder sb = new StringBuilder();
	Stack<String> tagStack = new Stack<String>();

	public SimpleXml() throws ParserConfigurationException {
		sb.append("<?xml verion=\"1.0\" charset=\"UTF-8\">");
	}

	public void close() {
		sb.append("</");
		sb.append(tagStack.pop());
		sb.append(">");
	}

	/**
	 * Squirt some CDATA into the current node
	 * 
	 * @param data
	 */
	public void data(String data) {
		sb.append(data);
	}

	public void start(String tagName) {
		start(tagName, Collections.EMPTY_MAP);
	}

	public void start(String tagName, Map<String, String> attributes) {
		assert attributes != null;
		sb.append("<");
		sb.append(tagName);
		sb.append(" ");
		for (Entry<String, String> entry : attributes.entrySet()) {
			sb.append(entry.getKey());
			sb.append("=\"");
			sb.append(entry.getValue());
			sb.append("\"");
		}
		sb.append(">");
		tagStack.push(tagName);
	}

	/**
	 * Convenience for leaf tags
	 * 
	 * @param tagName
	 * @param attributes
	 * @param contents
	 */
	public void tag(String tagName, Map<String, String> attributes,
			String contents) {
		start(tagName, attributes);
		data(contents);
		close();
	}

	/**
	 * Convenience for leaf tags
	 * 
	 * @param tagName
	 * @param contents
	 */
	public void tag(String tagName, String contents) {
		tag(tagName, Collections.EMPTY_MAP, contents);
	}

	/**
	 * Currently has the slightly surprising side effect of closing all open
	 * tags pending a bit of API-fu
	 */
	@Override
	public String toString() {
		// Close any open tags
		while (tagStack.size() > 0) {
			close();
		}
		return sb.toString();
	}
}
