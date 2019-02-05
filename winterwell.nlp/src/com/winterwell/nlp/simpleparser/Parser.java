package com.winterwell.nlp.simpleparser;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.winterwell.utils.Key;
import com.winterwell.utils.Printer;
import com.winterwell.utils.log.Log;

/**
 * Not fast or clever, but easy to use. A parser combinator for recursive
 * descent parsers with loop-checking. Use<br>
 * <code>import static com.winterwell.nlp.simpleparser.Parser.*;</code><br>
 * To get easy access to the combinators. Loosely inspired by the Scala Parser
 * class.
 * 
 * TODO back-tracking
 * 
 * @param PT
 *            the type of object produced and set as {@link AST#getX()}
 * @testedby {@link ParserTest}
 * @author daniel
 */
public abstract class Parser<PT> {

	public static boolean DEBUG = false;

	public static final Key<ParseFail> ERROR = new Key<ParseFail>("Parser.Fail");

	static final Map<String, Parser> parsers = new HashMap<String, Parser>();

	public static void clearGrammar() {
		parsers.clear();
	}

	/**
	 * If true, this parser can successfully match on "" - or should at least be
	 * tried. true by default for safety
	 */
	protected boolean canBeZeroLength = true;

	IDebug<ParseState> debug;

	private String failMessage;

	/**
	 * Set to null to ignore nodes from this parser.
	 */
	protected String name = "";

	protected Parser[] subs;

	public Parser(Parser... subParsers) {
		subs = subParsers;
		for (Parser sub : subParsers) {
			assert sub != null : this;
		}
	}

	protected ParseResult close(ParseResult<PT> r) {
		ParseState state = r.input.higher;
		assert state.down == this : state;
		AST ast = new AST(this, r.ast.getValue());
		r.ast.setParent(ast);
		ParseResult r2 = new ParseResult(state, ast, r.text, r.posn);
		if (state.higher == null)
			return r2;
		return state.higher.down.close(r2);
	}

	/**
	 * Test this parser with an example. For inline documentation & testing.
	 * 
	 * @param example
	 * @return this
	 */
	public Parser eg(String example) {
		ParseResult<PT> pr = parse(example);
		assert pr != null : '"' + example + "\" = FAIL";
		return this;
	}

	public final String getName() {
		return name;
	}

	/**
	 * Set the name. Associate this parser with a parser-label. This will
	 * replace any previous label-to-parser mapping.
	 * 
	 * @param name
	 *            Can be null or blank
	 * @return this
	 */
	public Parser label(String name) {
		this.name = name;
		if (name == null || name == "" || this instanceof Ref)
			return this;
		if (parsers.containsKey(name)) {
			Parser old = parsers.get(name);
			Log.report("Replacing parser for " + name + "!", Level.WARNING);
		}
		parsers.put(name, this);
		return this;
	}

	public Parser<PT> onFail(String message) {
		this.failMessage = message;
		return this;
	}

	/**
	 * Attempt to parse from the current position.
	 * 
	 * @param state
	 * @return null on failure
	 */
	protected abstract ParseResult<PT> doParse(ParseState state);

	public ParseResult<PT> parse(String string) {
		ParseSearch ps = new ParseSearch(this);
		return ps.parse(string);
	}

	/**
	 * This calls {@link #doParse(ParseState)}. It adds in debugging &
	 * loop-checking
	 * 
	 * @param ps
	 * @return
	 */
	ParseResult parse0(ParseState ps) {
		assert ps.down == this : ps;
		if (DEBUG) {
			// ignore space
			if (this != Parsers.space && this != Parsers.optSpace) {
				Printer.out("...>" + ps + "\n\tparse with " + ps.down);
			}
		}
		if (ps.isLoopy()) {
			if (DEBUG) {
				Printer.out("...>	loop!");
			}
			return null;
		}
		// for breakpoints
		if (debug != null) {
			debug.call(ps);
		}
		ParseResult r = doParse(ps);
		if (r == null && failMessage != null) {
			ParseFail.setParseFail(new ParseFail(ps, ps.posn, failMessage));
		}
		return r;
	}

	public ParseResult<PT> parseOut(String string) {
		if (DEBUG) {
			Printer.out("Parsing " + string + "...");
		}
		ParseResult<PT> pr = parse(string);
		assert pr != null : '"' + string + "\" = FAIL "+ParseFail.getParseFail();
		if (DEBUG) {
			Printer.out('"' + string + "\" =\n" + pr.ast);
		}
		assert pr != null : string;
		return pr;
	}

	protected ParseResult resume(ParseState state) {
		assert state.down == this : state;
		ParseResult r = doParse(state);
		if (r == null)
			return null;
		return state.higher.down.close(r);
	}

	public Parser<PT> setCanBeZeroLength(boolean canBe) {
		canBeZeroLength = canBe;
		return this;
	}

	/**
	 * For inserting breakpoints into parsers. E.g. you could stick a breakpoint
	 * inside here: <code><pre>
	 * parser = parser.setDebug(new IDebug<ParseState>() {public void call(ParseState state) {
							assert state != null;
						}});
	</pre></code>
	 * 
	 * @param dbg
	 * @return
	 */
	public Parser<PT> setDebug(IDebug<ParseState> debug) {
		this.debug = debug;
		return this;
	}

	@Override
	public String toString() {
		return name + "-Parser";
	}

}
