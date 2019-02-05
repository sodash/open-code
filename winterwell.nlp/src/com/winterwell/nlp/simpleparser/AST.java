package com.winterwell.nlp.simpleparser;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ITree;
import com.winterwell.utils.containers.Slice;
import com.winterwell.utils.containers.Tree;

/**
 * A simple abstract syntax tree. Use {@link #getName()} for token-type. Use
 * {@link #getValue()} for the matched String
 * 
 * @author daniel
 * 
 */
public class AST<X> extends Tree<Slice> {

	private final String name;

	public final Parser<X> parser;

	private Object x;

	public AST(Parser<X> parser, AST<?> onlyChild) {
		this(parser, onlyChild.getValue());
		onlyChild.setParent(this);
	}

	public AST(Parser<X> parser, Slice value) {
		this.name = parser.name;
		this.parser = parser;
		setValue(value);
	}

	public String getName() {
		if (name == null)
			return null;
		if (name.length() != 0)
			return name;
		if (getChildren().size() == 1)
			return ((AST) getOnlyChild()).getName();
		return name;
	}

	// public AST() {
	// }

	/**
	 * This ONLY recovers from the first level of children. TODO Although it can
	 * follow through Refs. ??needed?
	 * 
	 * @param <Y>
	 * @param p
	 * @return
	 * @deprecated need to think about how to do better
	 */
	@Deprecated
	public <Y> AST<Y> getNode(Parser<Y> p) {
		if (getNode2_yes(p, parser))
			return (AST<Y>) this;
		for (ITree ast : getChildren()) {
			AST kid = (AST) ast;
			Parser kp = kid.parser;
			while (kp instanceof Ref) {
				// Can this happen? Don't Ref's never appear in the parse tree?
				kp = ((Ref) kp).p;
			}
			if (getNode2_yes(p, kp))
				return kid;
			// recurse - but only through un-named nodes
			// TODO we need breadth first search to recurse or we could get
			// subtle bugs :(
			if (Utils.isBlank(kp.name)) {
				AST match = kid.getNode(p);
				if (match != null)
					return match;
			}
		}
		return null;
	}

	/**
	 * 
	 * @param nme
	 * @return first node of this name, or null
	 */
	public AST getNode(String nme) {
		if (nme.equals(this.name))
			return this;
		for (ITree ast : getChildren()) {
			AST kid = (AST) ast;
			AST n = kid.getNode(nme);
			if (n != null)
				return n;
		}
		return null;
	}

	private boolean getNode2_yes(Parser a, Parser b) {
		assert a != null && b != null;
		if (a == b)
			return true;
		String na = a.getName();
		if (!Utils.isBlank(na) && na.equals(b.getName()))
			return true;
		return false;
	}

	public Parser<X> getParser() {
		return parser;
	}

	/**
	 * 
	 * @return the processed single child, or null
	 */
	public X getX() {
		if (x != null)
			return (X) x;
		if (getChildren().size() == 1)
			return ((AST<X>) getOnlyChild()).getX();
		return null;
	}

	public boolean isNamed(String string) {
		return Utils.equals(name, string);
	}

	public String parsed() {
		return getValue().toString();
	}

	public void setX(Object x) {
		this.x = x;
	}

	@Override
	protected String toString4_nodeName() {
		return name == null || name.length() == 0 ? "_" : name;
	}
}
