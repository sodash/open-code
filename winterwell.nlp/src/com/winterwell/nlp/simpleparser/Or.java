package com.winterwell.nlp.simpleparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Stack;

import com.winterwell.utils.Printer;

class Or extends Parser {

	public Or(Parser[] parsers) {
		super(parsers);
		assert parsers.length > 1 : parsers;
		assert !Arrays.asList(parsers).contains(null);
		label(null);
		// so push onto stack goes in the right order
		Collections.reverse(Arrays.asList(subs));
		for (Parser p : parsers) {
			if (p.canBeZeroLength) {
				canBeZeroLength = true;
			}
		}
	}

	@Override
	public ParseResult doParse(ParseState state) {
		assert state.down == this : state;
		Stack<ParseState> agenda = state.getAgenda();
		boolean len0 = state.unparsed().length() == 0;
		for (int i = 0; i < subs.length; i++) {
			Parser p = subs[i];
			if (len0 && !p.canBeZeroLength) {
				continue;
			}
			ParseState s = new ParseState(p, state);
			agenda.push(s);
		}
		return null;
	}

	@Override
	public String toString() {
		ArrayList list = new ArrayList(Arrays.asList(subs));
		Collections.reverse(list);
		return Printer.toString(list, " | ");
	}

}
