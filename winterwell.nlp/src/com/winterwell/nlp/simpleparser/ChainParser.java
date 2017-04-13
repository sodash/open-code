package com.winterwell.nlp.simpleparser;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Slice;

/**
 * Match e.g. a list of items "a, b, c"
 * 
 * @author daniel
 * 
 */
public class ChainParser extends SeqnParser {

	Parser element;
	private int maxOccur;
	private int minOccur;
	Parser separator;

	/**
	 * 
	 * @param element
	 *            must occur at least once
	 * @param separator
	 */
	public ChainParser(Parser element, Parser separator) {
		this(element, separator, 1, Integer.MAX_VALUE);
	}

	public ChainParser(Parser element, Parser separator, int minOccur,
			int maxOccur) {
		super(new Parser[] { element });
		this.element = element;
		this.separator = separator;
		this.minOccur = minOccur;
		this.maxOccur = maxOccur;
	}

	@Override
	ParseResult parse2(final ParseState state, int seqn, AST tree) {
		int cnt = 0;
		int endPosn = state.posn;
		ParseResult sepResult = null;
		while (true) {
			ParseState s = new ParseState(element, state,
					sepResult == null ? endPosn : sepResult.posn);
			ParseResult r = element.parse0(s);
			if (r == null) {
				break;
			}
			cnt++;
			if (cnt == maxOccur) {
				break;
			}
			if (sepResult != null) {
				close2_add(tree, sepResult.ast);
			}
			close2_add(tree, r.ast);
			endPosn = r.posn;
			// a separator?
			ParseState s3 = new ParseState(separator, state, r.posn);
			sepResult = separator.parse0(s3);
			if (sepResult == null) {
				break;
			}
		}
		if (cnt < minOccur)
			return null;
		// success
		Slice parsed = new Slice(state.text, state.posn, endPosn);
		tree.setValue(parsed);
		ParseResult result = new ParseResult(state, tree, state.text, endPosn);
		// optionally process the chain for an output object
		Object x = process(result);
		if (x != null) {
			tree.setX(x);
		}
		return result;
	}

	protected Object process(ParseResult parsed) {
		return null;
	}

	@Override
	public String toString() {
		return Utils.isBlank(name) ? "" + element + separator + "+" : name;
	}
}
