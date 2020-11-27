package com.winterwell.nlp.simpleparser;

import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Slice;

/**
 * Parse a fixed bit of text. Sets the parsed word as the AST.getX() value.
 * 
 * Use
 * import static com.winterwell.nlp.simpleparser.Parsers.*;
 * 
 * @author daniel
 * 
 */
class Lit extends Parser<String> {

	private String word;

	public Lit(String word) {
		super();
		this.word = word;
		canBeZeroLength = word.length() == 0;
	}
	


	@Override
	protected String sample() {
		return word;
	}

	@Override
	public ParseResult doParse(ParseState state) {
		assert state.down == this;
		Slice text = state.unparsed();
		if (!text.startsWith(word))
			return null;
		Slice parsed = new Slice(text, 0, word.length());
		assert parsed.toString().equals(word) : parsed + " != " + word;
		AST<String> tree = new AST<String>(this, parsed);
		tree.setX(word);
		ParseResult r = new ParseResult(state, tree, state.text, state.posn
				+ word.length());
		return r;
	}

	@Override
	public String toString() {
		return word;
	}

}

/**
 * Not fast or clever, but easy to use. A parser combinator for recursive
 * descent parsers with loop-checking. Use<br>
 * <code>import static com.winterwell.nlp.simpleparser.Parser.*;</code><br>
 * To get easy access to the combinators. Loosely inspired by the Scala Parser
 * class.
 * 
 * TODO back-tracking
 * 
 * @testedby  ParserTest}
 * @author daniel
 */
public class Parsers {

	/**
	 * Find by label, or null
	 * @param name
	 * @return
	 */
	public static Parser getParser(String name) {
		return Parser.parsers.get(name);
	}
	
	static final class Opt<PT> extends First<PT> {
		public Opt(Parser p) {
			super(new Parser[] { p, lit("").label(null) });
		}

		@Override
		public String toString() {
			return Utils.isBlank(name) ? subs[0] + "?" : name;
		}		
	}

	static final class Word extends Parser<String> {
		private final String word;

		Word(String word) {
			this.word = word;
			canBeZeroLength = word.length() == 0;
		}

		@Override
		protected ParseResult<String> doParse(ParseState state) {
			Slice unp = state.unparsed();
			if (!unp.startsWith(word))
				return null;
			AST<String> ast = new AST<String>(this, new Slice(unp, 0,
					word.length()));
			ast.setX(word);
			ParseResult<String> r = new ParseResult<String>(state, ast,
					state.text, state.posn + word.length());
			if (unp.length() == word.length())
				return r;
			char c = unp.charAt(word.length());
			if (Character.isLetterOrDigit(c))
				return null;
			return r;
		}

		@Override
		public String toString() {
			return word; // '"'+word+'"';
		}
		
		@Override
		protected String sample() {
			return word;
		}
	}
	/**
	 * A mandatory space. Does not create a node in the AST
	 */
	public static final Parser space = regex("\\s+").label(null);

	public static final Parser optSpace = opt(space);


	static {
		space.canBeZeroLength = false;
	}

	/**
	 * Matches *optional* brackets, e.g. "(body)" or plain "body", or
	 * "((body))". For obligatory brackets, just use {@link #seq(Parser...)}
	 * 
	 * @param open
	 * @param body
	 * @param close
	 * @return
	 * @testedby  ParserTest#testBracketed()}
	 */
	public static <PT> Parser<PT> bracketed(String open, Parser<PT> body,
			String close) {
		// try for a nice name
		String bs = Utils.isBlank(body.name) ? "_r" + Utils.getId() : body.name;
		String name = open + bs + close;
		if (Parser.parsers.containsKey(name)) {
			// oh well - something unique
			name = "_" + Utils.getUID();
		}
		Parser _open = lit(open).label(null);
		Parser _close = lit(close).label(null);
		Parser<PT> bp = first(
				seq(_open, first(body, ref(name)), _close).label(name), body)
				.setDesc("?brackets");
		return bp;
	}

	public static ChainParser chain(Parser element, Parser separator) {
		return new ChainParser(element, separator, 1, Integer.MAX_VALUE);
	}

	/**
	 * Like {@link #or(Parser...)} but without backtracking. The first match
	 * wins. This is more efficient (and easier to debug) than or.
	 */
	public static <X> Parser<X> first(Parser... parsers) {
		return new First<X>(parsers);
	}

	/**
	 * Convenience for {@link #lit(String...)} with label null. This parser is
	 * NOT optional - one of the words must be present. But the nodes will not
	 * be added to the abstract syntax tree (AST), i.e. the results from the
	 * parse are ignored.
	 * 
	 * @param words
	 * @return
	 */
	public static Parser<String> ignore(final String... words) {
		return lit(words).label(null);
	}

	/**
	 * An anonymous literal if one String is provided. Otherwise a first() over
	 * the words
	 * 
	 * @param name
	 * @param words
	 * @return
	 */
	public static Parser<String> lit(final String... words) {
		if (words.length == 1)
			return new Lit(words[0]);
		assert words.length != 0 : words;
		Parser[] lits = new Parser[words.length];
		for (int i = 0; i < words.length; i++) {
			String w = words[i];
			Lit lit = new Lit(w);
			lits[i] = lit;
		}
		return first(lits);
	}

	/**
	 * Match a number and create a double. Convenience for a common case.
	 * 
	 * @param name
	 *            'cos you might easily have two of these & need different
	 *            names, e.g. x + y
	 * @return
	 */
	public static PP<Number> num(String name) {
		return new PP<Number>(regex("-?\\d+(\\.\\d+)?")) {
			@Override
			protected Number process(ParseResult<?> pr) {
				return Double.valueOf(pr.parsed());
			}
		}.label(name);
	}

	/**
	 * Make a rule optional. E.g. opt(space) for optional whitespace. This is a
	 * convenience for {@link #first(Parser...)} - it is not a true or (no
	 * backtracking)
	 * 
	 * @param parser
	 * @return A parser which never fails - returns the base result, or a blank
	 *         tree.
	 */
	public static <PT> Parser<PT> opt(final Parser<PT> parser) {
		return new Opt<PT>(parser);
	}

	@Deprecated
	// Buggy! // TODO fix!
	public static Parser or(Parser... parsers) {
		return new Or(parsers);
	}

	/**
	 * Use to build recursive rules. This will lazily load a parser of the same
	 * name. Note: be careful that only *one* such parser gets defined!
	 * 
	 * @param name
	 */
	public static Parser ref(String name) {
		return new Ref(name);
	}

	/**
	 * Match a regular expression. ParseResult.getX() returns the successful
	 * {@link MatchResult} object.
	 * 
	 * @param regex
	 * @return
	 */
	public static RegexParser regex(String regex) {
		return new RegexParser(Pattern.compile("^" + regex));
	}
	
	public static Parser repeat(Parser parser) {
		return repeat(parser, 0, Integer.MAX_VALUE);
	}

	public static Parser repeat(Parser parser, int min, int max) {
		throw new TodoException();
	}

	public static Parser seq(final Parser... parsers) {
		assert parsers.length != 0;
		if (parsers.length == 1)
			return parsers[0];
		return new SeqnParser(parsers);
		// Parser next = parsers[parsers.length-1];
		// for (int i=parsers.length-2; i>=0; i--) {
		// SeqnParser seqn = new SeqnParser(parsers[i], next);
		// next = seqn;
		// }
		// return next;
	}

	/**
	 * Like lit(), but only matches whole-words. E.g. "hell" would match on
	 * "hell." or "hell " but not "hello". Note that "[s]hell" (s already
	 * parsed) would match.
	 * <p>
	 * Uses {@link #first(Parser...)} if there are multiple words.
	 * @return a fresh parser
	 */
	public static Parser<String> word(final String... words) {
		if (words.length == 1)
			return new Word(words[0]);
		Parser[] lits = new Parser[words.length];
		for (int i = 0; i < words.length; i++) {
			String w = words[i];
			Word lit = new Word(w);
			lits[i] = lit;
		}
		return first(lits);
	}

}
