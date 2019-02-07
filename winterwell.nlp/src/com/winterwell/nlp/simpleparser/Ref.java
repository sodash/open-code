package com.winterwell.nlp.simpleparser;

/**
 * Refer to a parser by label -- use this to avoid circular loops when initialising parsers
 * @author daniel
 *
 * @param <PT>
 */
public final class Ref<PT> extends Parser<PT> {
	Parser<PT> p;

	public Ref(String name) {
		super();
		label(name);
		assert name != null;
		// better safe than sorry
		canBeZeroLength = true;
	}

	public final Parser lookup() {
		return Parser.parsers.get(name);
	}

	@Override
	public ParseResult doParse(ParseState state) {
		// if (p==null) { // if we want over-rides, then we have to do a lookup
		// :(
		p = lookup();
		if (p == null) throw new IllegalArgumentException("No parser named: "+name);
		assert !(p instanceof Ref) : p;
		assert p.getName().equals(name) : p + " where " + p.name + " != "
				+ name;
		canBeZeroLength = p.canBeZeroLength;
		// }
		assert p != null : name;
		return p.doParse(new ParseState(p, state));
	}
}