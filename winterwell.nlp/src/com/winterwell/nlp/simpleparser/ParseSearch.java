package com.winterwell.nlp.simpleparser;

import java.util.Stack;

import com.winterwell.utils.Printer;

/**
 * 
 * @author daniel
 *
 */
public class ParseSearch {

	private Parser parser;

	public ParseSearch(Parser parser) {
		this.parser = parser;
	}

	public ParseResult parse(String string) {
		// clear any ParseFail
		ParseFail.setParseFail(null);
		ParseState ps = new ParseState(parser, string);
		// parse!
		ParseResult r = parser.parse0(ps); // doParse(ps);
		if (r != null && r.unparsed().length() == 0)
			return r;
		// Or...
		Stack<ParseState> agenda = ps.getAgenda();
		while ( ! agenda.isEmpty()) {
			ps = agenda.pop();
			assert ps != null;
			assert ps.down != null;
			ParseResult pr = ps.down.resume(ps);
			if (pr == null) {
				if (Parser.DEBUG) {
					Printer.out("\tfailed: " + ps);
				}
				continue;
			}
			if (pr.parsed().equals(ps.text))
				return pr;
			if (Parser.DEBUG) {
				Printer.out("\tignoring partial parse: " + pr + " from " + ps);
			}
		}
		// fail
		if (r != null) {
			ParseFail pf = new ParseFail(r, "incomplete parse: "+r.unparsed());
			ParseFail.setParseFail(pf);
		}
		return null;
	}

	@Override
	public String toString() {
		return "ParseSearch:" + parser;
	}

}
