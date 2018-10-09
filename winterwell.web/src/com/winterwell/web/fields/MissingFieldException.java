package com.winterwell.web.fields;

import java.util.Collections;
import java.util.List;

import com.winterwell.utils.Printer;
import com.winterwell.web.WebInputException;

public class MissingFieldException extends WebInputException {
	private static final long serialVersionUID = 1L;

	public MissingFieldException(AField field) {
		super("Missing field " + field);
		assert field != null;
		this.fields = Collections.singletonList(field);
	}

	public MissingFieldException(List<AField> fields) {
		super("Missing fields " + Printer.toString(fields));
		this.fields = fields;
	}

	public MissingFieldException(AField field, String msg) {
		super("Missing " + field+" "+msg);
		assert field != null;
		this.fields = Collections.singletonList(field);
	}

	public List<AField> getFields() {
		return fields;
	}

}
