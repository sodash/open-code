package com.winterwell.nlp.simpleparser;

import java.util.logging.Level;

import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;

/**
 * PP, short for Processing Parser, converts parses into objects.
 * 
 * @author daniel
 * 
 */
abstract public class PP<Out> extends Parser<Out> {

	Parser<?> parser;

	public PP(Parser<?> p) {
		super(p);
		// not necc wrong but definitely a bit weird
		if (p instanceof Ref) {
			Log.report("Using a ref parser in a production rule: " + p,
					Level.WARNING);
		}
		this.parser = p;
		// make sure this parser's AST nodes don't get eaten by a higher parser
		// label(Utils.isBlank(p.getName())? "PP" : p.getName()); This buggers
		// up label-to-parser Refs
		canBeZeroLength = p.canBeZeroLength;
	}

	@Override
	public PP<Out> label(String name) {
		super.label(name);
		return this;
	}

	@Override
	protected ParseResult<Out> doParse(ParseState state) {
		ParseState ps = new ParseState(parser, state);
		ParseResult<?> r = parser.doParse(ps);
		if (r == null)
			return null;
		try {
			Out obj = process(r);
			AST<Out> ast = new AST<Out>(this, r.ast.getValue());
			// r.ast.setParent(ast); Discard the lower parse! This processed
			// node is now the leaf node
			ast.setX(obj);
			return new ParseResult<Out>(state, ast, state.text, r.posn);
		} catch (ParseFail e) {
			ParseFail.setParseFail(e);
			return null;
		}
	}

	/**
	 * @param r
	 * @return processed object for this node
	 * @throws ParseFail
	 *             if you wish to reject this ParseResult. The calling
	 *             {@link #doParse(ParseState)} method will log this and return
	 *             null.
	 */
	protected abstract Out process(ParseResult<?> r) throws ParseFail;

	@Override
	public String toString() {
		return "PP" + (Utils.isBlank(name) ? parser : name);
	}

}
