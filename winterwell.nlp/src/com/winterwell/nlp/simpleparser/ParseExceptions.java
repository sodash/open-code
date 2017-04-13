package com.winterwell.nlp.simpleparser;

import java.util.List;

import com.winterwell.utils.Printer;

public class ParseExceptions extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private List<ParseFail> errors;

	public ParseExceptions(List<ParseFail> errors) {
		super(Printer.toString(errors));
		this.errors = errors;
	}

	public List<ParseFail> getErrors() {
		return errors;
	}

}
