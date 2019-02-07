package com.winterwell.nlp.simpleparser;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.utils.containers.Slice;

public class ParseResult<X> {

	public final AST<X> ast;
	public final ParseState input;
	/**
	 * the parse position ie the start of unparsed
	 */
	final int posn;
	
	final String text;

	public ParseResult(ParseState input, AST<X> ast, String text, int posn) {
		this.input = input;
		this.ast = ast;
		this.text = text;
		this.posn = posn;
		assert input.posn <= posn;
		assert input.down == ast.parser : input.down + " vs " + ast.parser
				+ " from " + input;
		assert posn <= text.length() : text;
		assert text.endsWith((parsed() + unparsed())) : text + " !match ...["
				+ parsed() + "]+" + unparsed();
	}

	public List getLeafValues() {
		List<AST> ls = getLeaves();
		ArrayList vs = new ArrayList(ls.size());
		for (AST ast : ls) {
			vs.add(ast.getX());
		}
		return vs;
	}

	public List<AST> getLeaves() {
		List lvs = ast.getLeaves();
		return lvs;
	}

	public <X> AST<X> getNode(Parser<X> p) {
		if (p instanceof Ref) {
			p = ((Ref) p).p;
		}
		return ast.getNode(p);
	}

	public AST getNode(String label) {
		return ast.getNode(label);
	}

	public X getX() {
		return ast.getX();
	}

	/**
	 * 
	 * @param parser
	 * @return processed result for the named node from this parser
	 */
	public <X2> X2 getX(Parser<X2> parser) {
		AST<X2> node = getNode(parser);
		if (node == null)
			return null;
		return node.getX();
	}

	public String parsed() {
		return ast.getValue().toString();
	}

	@Override
	public String toString() {
		return "ParseResult[" + parsed() + "]" + unparsed();
	}

	public Slice unparsed() {
		return new Slice(text, posn);
	}

}
