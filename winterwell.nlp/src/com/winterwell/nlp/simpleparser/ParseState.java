package com.winterwell.nlp.simpleparser;

import java.util.Stack;

import com.winterwell.utils.containers.Slice;

public class ParseState {

	final Stack<ParseState> agenda;

	/**
	 * The parser that is working down from here.
	 * @deprecated Used for code state sanity checks.
	 */
	final Parser down;

	final ParseState higher;
	AST partial;

	public final int posn;

	int seqn;

	final public String text;

	/**
	 * Convenience for simply passing down the chain
	 * 
	 * @param trying
	 * @param state
	 */
	public ParseState(Parser trying, ParseState higher) {
		this(trying, higher, higher.posn);
	}

	ParseState(Parser down, ParseState higher, int posn) {
		this.text = higher.text;
		this.posn = posn;
		this.down = down;
		this.higher = higher;
		agenda = null;
	}

	ParseState(Parser down, String text) {
		this.text = text;
		this.posn = 0;
		this.down = down;
		this.higher = null;
		agenda = new Stack<ParseState>();
	}

	public Stack<ParseState> getAgenda() {
		return agenda == null ? higher.getAgenda() : agenda;
	}

	int getDepth() {
		if (higher == null)
			return 0;
		return 1 + higher.getDepth();
	}

	boolean isLoopy() {
		if (higher == null)
			return false;
		return higher.isLoopy(this);
	}

	private boolean isLoopy(ParseState state) {
		assert this != state;
		if (this.down != null && this.down == state.down
				&& this.posn == state.posn && this.text.equals(state.text)) 
		{
			return true;
		}
		if (higher == null)
			return false;
		return higher.isLoopy(state);
	}

	@Override
	public String toString() {
		StringBuilder stack = new StringBuilder();
		return getDepth() + ") " + "[" + new Slice(text, 0, posn) + "]"
				+ unparsed() + (stack.length() == 0 ? "" : " stack:" + stack);
	}

	public Slice unparsed() {
		return new Slice(text, posn);
	}

}
