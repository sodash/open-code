package com.winterwell.web.fields;

import com.winterwell.utils.web.WebUtils;

/**
 * A field with a fixed value.
 * 
 * @author Daniel
 */
public class UnmodifiableField<X> extends AField<X> {

	private static final long serialVersionUID = 1L;
	private String displayValue;

	private X value;

	/**
	 * 
	 * @param field
	 * @param value
	 *            The text will be blank if this is null
	 */
	public UnmodifiableField(AField<X> field, X value) {
		this(field, value, value == null ? "" : field.toString(value));
	}

	public UnmodifiableField(AField<X> field, X value, String displayValue) {
		this(field.getName(), value, displayValue);
	}

	public UnmodifiableField(String fieldName, X value, String displayValue) {
		super(fieldName, "hidden");
		this.value = value;
		this.displayValue = displayValue;
	}

	public void appendHtmlTo(StringBuilder page, X ignoreMe) {
		super.appendHtmlTo(page, value);
		// some sort of container element?
		page.append("<input type='text' disabled='true' value='"
				+ WebUtils.attributeEncode(displayValue) + "'>");
	}

	@Override
	public Class<X> getValueClass() {
		return value==null? null : (Class<X>) value.getClass();
	}

}
