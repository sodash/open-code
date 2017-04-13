package com.winterwell.nlp.simpleparser;

import com.winterwell.utils.Environment;

import com.winterwell.utils.containers.Slice;

public class ParseFail extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Set the (thread-local) ParseFail explanation. This will do nothing if a
	 * "better" explanation has already been set.
	 * 
	 * @param pf
	 *            Can be null to clear the previous setting.
	 * @return true if set
	 */
	public static boolean setParseFail(ParseFail pf) {
		Environment env = Environment.get();
		ParseFail old = env.get(Parser.ERROR);
		if (old == null || pf == null || old.slice.end < pf.slice.end) {
			env.put(Parser.ERROR, pf);
			return true;
		}
		if (old.slice.end == pf.slice.end) {
			// ??
		}
		return false;
	}

	private ParseState input;
	public int lineNum;

	private String message;

	/**
	 * 
	 */
	public final Slice slice;

	public ParseFail(ParseResult<?> pr, String message) {
		this.input = pr.input;
		slice = pr.ast.getValue();
		this.message = message;
	}

	/**
	 * 
	 * @param input
	 *            The unsuccessful parser
	 * @param failPosn
	 *            How far did it get?
	 * @param message
	 */
	public ParseFail(ParseState input, int failPosn, String message) {
		this.input = input;
		this.slice = new Slice(input.text, input.posn, failPosn);
		this.message = message;
	}

	/**
	 * Fail with message (but no parse state)
	 * 
	 * @param failedToParse
	 * @param message
	 */
	public ParseFail(Slice failedToParse, String message) {
		this.slice = failedToParse;
		input = null;
		this.message = message;
	}

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		String s = "ParseFail [lineNum=" + lineNum + ", slice=" + slice + "]";
		return message == null ? s : s + " " + message;
	}

}
