package com.winterwell.nlp.simpleparser;

import java.util.Arrays;

import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;

public class First<PT> extends Parser<PT> {

	public First(Parser[] parsers) {
		super(parsers);
		assert parsers.length > 0;
		for (Parser p : parsers) {
			if (p.canBeZeroLength) {
				canBeZeroLength = true;
			}
		}
	}

	@Override
	protected ParseResult parse(ParseState state) {
		boolean len0 = state.unparsed().length() == 0;
		for (Parser p : subs) {
			if (len0 && !p.canBeZeroLength) {
				continue;
			}
			ParseState s = new ParseState(p, state);
			ParseResult r = p.parse0(s);
			if (r == null) {
				continue;
			}
			// special case: do not create a possibly named node
			// for a for-discarding lower node
			// BUT: lit("a", "b").label("x") -- which uses first -- should
			// create a named node
			// if (r.ast.getName() == null) return r;
			AST ast = new AST(this, r.ast);
			return new ParseResult(state, ast, r.text, r.posn);
		}
		return null;
	}

	@Override
	public String toString() {
		return Utils.isBlank(name) ? "["
				+ Printer.toString(Arrays.asList(subs), "/") + "]" : name;
	}

}
