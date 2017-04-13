package com.winterwell.nlp.simpleparser;

import java.util.Arrays;

import com.winterwell.utils.Printer;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Slice;

class SeqnParser extends Parser<Object> {

	public SeqnParser(Parser a, Parser b) {
		super(new Parser[] { a, b });
		assert subs.length != 0 : name;
		assert subs.length > 1 : name;
		assert !Arrays.asList(subs).contains(null);
	}

	public SeqnParser(Parser[] parsers) {
		super(parsers);
		canBeZeroLength = true;
		for (Parser p : parsers) {
			if (!p.canBeZeroLength) {
				canBeZeroLength = false;
			}
		}
	}

	@Override
	protected ParseResult close(ParseResult r) {
		// TODO
		// SeqnParseState partialState = r.input.higher;
		// parse2()
		// return close();
		return null;
	}

	protected void close2_add(AST<?> tree, AST<?> part) {
		// only add named nodes to the tree
		if (part.getName() == null)
			return;
		// discard this part and eat its children instead?
		// NB: this can cause AST.getNode(Parser) to fail in weird ways
		if ("".equals(part.getName()) && !part.getChildren().isEmpty()) {
			// mm, tasty children
			for (Object kid : part.getChildren().toArray()) {
				close2_add(tree, (AST) kid);
			}
			return;
		}
		// add
		part.setParent(tree);
		Slice v = tree.getValue();
		assert v != null : tree;
		tree.setValue(new Slice(v, 0, part.getValue().length()));
	}

	@Override
	public ParseResult parse(final ParseState state) {
		AST tree = new AST(this, new Slice(state.text, state.posn, state.posn));
		return parse2(state, 0, tree);
	}

	ParseResult parse2(final ParseState state, int seqn, AST tree) {
		int posn = state.posn;
		for (; seqn < subs.length; seqn++) {
			Parser p = subs[seqn];
			ParseState partialState = state; // TODO new SeqnParseState(state,
												// seqn, tree);
			ParseState state2 = new ParseState(p, partialState, posn);
			ParseResult r = p.parse0(state2);
			if (r == null)
				return null;
			close2_add(tree, r.ast);
			posn = r.posn;
		}
		Slice parsed = new Slice(state.text, state.posn, posn);
		tree.setValue(parsed);
		return new ParseResult(state, tree, state.text, posn);
	}

	@Override
	public String toString() {
		return Utils.isBlank(name) ? Printer.toString(Arrays.asList(subs))
				: name;
	}

}