package com.winterwell.web.fields;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.io.ISerialize;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.web.WebInputException;

/**
 * A comma or whitespace separated list of words. All words will be trimmed,
 * unless explicitly untrimmed using quote marks, e.g. tag1, " tag 2", tag3 Uses
 * {@link StrUtils#split(String)} to split the input.
 * <p>
 * This can also handle the multiple returns from same-name input fields (e.g.
 * checkboxes, or ImageCheckbox) -- provided the values do not contain spaces or
 * commas!
 * 
 * TODO support for Set rules
 * 
 * @author daniel
 * @testedby  ListFieldTest}
 */
public class ListField<X> extends AField<List<X>> {

	public static enum KNullPolicy {
		FILTER_OUT, KEEP, ERROR
	}
	
	@Override
	public Class getValueClass() {
		return List.class;
	}

	private static final long serialVersionUID = 1L;

	private ISerialize<X> elementConverter;

	KNullPolicy nullPolicy = KNullPolicy.KEEP;

	private String splitPattern;

	/**
	 * 
	 * @param name
	 * @param elementConverter
	 *            Can be null if the elements are Strings which need no
	 *            converting
	 */
	public ListField(String name, ISerialize<X> elementConverter) {
		super(name);
		this.elementConverter = elementConverter;
	}

	public ListField(String name, String type, ISerialize<X> elementConverter) {
		super(name, type);
		this.elementConverter = elementConverter;
	}

	/**
	 * No-convertor, String list
	 * @param name
	 */
	public ListField(String name) {
		this(name, null);
	}

	/**
	 * @return a list of objects. This is a fresh ArrayList which can be
	 *         modified.
	 */
	@Override
	public List<X> fromString(String s) throws Exception {
		List<String> bits = splitPattern!=null? Arrays.asList(s.split(splitPattern)) : StrUtils.split(s);
		if (elementConverter == null) {
			for (int i = 0; i < bits.size(); i++) {
				String bit = bits.get(i);
				if (bit.length() == 0) {
					continue;
				}
				// HACK for decoding problem characters
				if (bit.charAt(0) == '?') {
					bit = WebUtils.urlDecode(bit.substring(1));
					bits.set(i, bit);
				}
			}
			// copy to ArrayList as per javadoc so the list could be edited
			return new ArrayList(bits);
		}
		ArrayList<X> conv = new ArrayList<X>(bits.size());
		for (String bit : bits) {
			if (bit.length() == 0) {
				continue;
			}
			// HACK for decoing problem characters
			if (bit.charAt(0) == '?') {
				bit = WebUtils.urlDecode(bit.substring(1));
			}
			X x = elementConverter.fromString(bit);
			// nulls can happen, e.g. if a dud object slug is passed in
			if (x == null) {
				switch (nullPolicy) {
				case ERROR:
					throw new WebInputException(this, "null value in " + this);
				case FILTER_OUT:
					continue;
				}
			}
			conv.add(x);
		}
		return conv;
	}

	@Override
	public String getStringValue(HttpServletRequest request) {
		String[] values = request.getParameterValues(getName());
		if (values == null) {
			// hack: jquery sends multiple values by adding [] to the parameter name
			values = request.getParameterValues(getName()+"[]");
			if (values==null) return null;
		}
		if (values.length == 1)
			return values[0];
		// handle multiple values if we have them
		return StrUtils.join(values, ",");
	}

	public void setNullPolicy(KNullPolicy nullPolicy) {
		this.nullPolicy = nullPolicy;
	}

	public String toString(Collection<X> value) {
		if (value.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		for (X x : value) {
			assert x != null : value;
			String s = elementConverter == null ? (String) x 
					: elementConverter.toString(x);
			// Problem characters?
			if (s.contains("\r") || s.contains("\n") || s.contains("\"")
					|| s.contains(",")) {
				// Just altering these causes bugs with slugs.
				// Encode them!
				s = WebUtils.urlEncode(s);
				// HACK: add a ? to mark this out for decoding
				sb.append("?");
				sb.append(s);
				sb.append(", ");
				continue;
			}
			// quoted for whitespace?
			if (s.contains(" ") || s.contains("\t")) {
				s = '"' + s + '"';
			}
			sb.append(s);
			sb.append(", ");
		}
		StrUtils.pop(sb, 2);
		return sb.toString();
	}

	@Override
	public String toString(List<X> value) {
		return toString((Collection<X>) value);
	}

	/**
	 * Override the default (which is to use {@link StrUtils#split(String)})
	 * @param splitPattern
	 * @return
	 */
	public ListField<X> setSplitPattern(String splitPattern) {
		this.splitPattern = splitPattern;
		assert splitPattern==null || ! splitPattern.isEmpty();
		return this;
	}
}
