package com.winterwell.nlp.simpleparser;

import java.util.Map;

import com.winterwell.utils.Environment;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Slice;
import com.winterwell.utils.web.IHasJson;

/**
 * These are auto-set -- use 
 * @author daniel
 *
 */
public class ParseFail extends RuntimeException implements IHasJson {
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

//	private ParseState input;
	public int lineNum;

	private String message;

	/**
	 * 
	 */
	public final Slice slice;

	public ParseFail(ParseResult<?> pr, String message) {
		this(pr.ast.getValue(), message);
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
		this(new Slice(input.text, input.posn, failPosn), message);
//		this.input = input;
	}

	/**
	 * Fail with message (but no parse state)
	 * 
	 * @param failedToParse
	 * @param message
	 */
	public ParseFail(Slice failedToParse, String message) {
		this.slice = failedToParse;
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

	public static ParseFail getParseFail() {
		return Environment.get().get(Parser.ERROR);
	}

	@Override
	public Map toJson2()  {
		return new ArrayMap(
			"@type", "ParseFail",
			"line", lineNum,
			"message", message,
			"slice", slice
		);
	}

}
