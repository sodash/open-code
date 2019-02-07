package com.winterwell.nlp.simpleparser;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Slice;

/**
 * match a regular expression. .getX() returns the {@link MatchResult} object
 * 
 * @author daniel
 * 
 */
public class RegexParser extends Parser<MatchResult> {

	Pattern regex;

	public RegexParser(Pattern regex) {
		super();
		this.regex = regex;
		// better safe than sorry
		canBeZeroLength = true;
	}

	@Override
	public ParseResult<MatchResult> doParse(ParseState state) {
		Slice text = state.unparsed();
		Matcher m = regex.matcher(text);
		if (!m.find())
			return null;
		assert m.start() == 0;
		Slice parsed = new Slice(text, 0, m.end());
		AST<MatchResult> ast = new AST<MatchResult>(this, parsed);
		ast.setX(m.toMatchResult());
		return new ParseResult<MatchResult>(state, ast, state.text, state.posn
				+ m.end());
	}

	@Override
	public String toString() {
		return Utils.isBlank(name) ? regex.pattern().substring(1) : name;
	}

}
