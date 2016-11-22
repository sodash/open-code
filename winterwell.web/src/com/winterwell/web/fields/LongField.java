package com.winterwell.web.fields;

import com.winterwell.web.WebInputException;

public class LongField extends AField<Long> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LongField(AField fieldName) {
		this(fieldName.getName());
	}

	public LongField(String name) {
		this(name, "text");
	}

	public LongField(String name, String type) {
		super(name, type);
	}

	public LongField(String name, String type, boolean required) {
		super(name, type, required);
	}

	@Override
	public Long fromString(String v) {
		try {
			return Long.valueOf(v);
		} catch (NumberFormatException e) {
			throw new WebInputException(v
					+ " is not in the correct form (a number)");
		}
	}

	@Override
	public Class<Long> getValueClass() {
		return Long.class;
	}

}
