package com.winterwell.web;

import java.util.Collections;
import java.util.List;

import com.winterwell.web.fields.AField;

public class WebInputException extends WebEx.E40X {

	private static final long serialVersionUID = 1L;


	protected List<AField> fields;

	public WebInputException(AField badField) {
		this(badField.toString());
		fields = Collections.singletonList(badField);
	}

	public WebInputException(AField badField, String msg) {
		this(msg);
		fields = Collections.singletonList(badField);
	}

	public WebInputException(String string) {
		super(400, string);
	}

	public WebInputException(String string, Exception e) {
		super(400, string, e);
	}

	/**
	 * @see WebEx.E40X and subclasses
	 * @param string
	 * @param code
	 */
	public WebInputException(String string, int code) {
		super(code, string);		
	}

}
