package com.winterwell.nlp.simpleparser;

import static com.winterwell.nlp.simpleparser.Parsers.bracketed;
import static com.winterwell.nlp.simpleparser.Parsers.chain;
import static com.winterwell.nlp.simpleparser.Parsers.first;
import static com.winterwell.nlp.simpleparser.Parsers.lit;
import static com.winterwell.nlp.simpleparser.Parsers.num;
import static com.winterwell.nlp.simpleparser.Parsers.opt;
import static com.winterwell.nlp.simpleparser.Parsers.or;
import static com.winterwell.nlp.simpleparser.Parsers.ref;
import static com.winterwell.nlp.simpleparser.Parsers.regex;
import static com.winterwell.nlp.simpleparser.Parsers.seq;
import static com.winterwell.nlp.simpleparser.Parsers.space;

import java.util.List;
import java.util.regex.MatchResult;

import org.junit.Test;

import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

public class ParserTest {

	@Test
	public void testBackTracking() {
		Parser.clearGrammar();
		Parser num = regex("\\d+");
		Parser p = or(num, lit("2nite"));
		ParseResult pr = p.parse("2nite");
		assert pr != null;
		assert pr.unparsed().length() == 0 : pr;
	}

	@Test
	public void testBracketed() {
		{
			Parser body = lit("body");
			Parser brp = bracketed("(", body, ")");
			brp.parseOut("body");
			brp.parseOut("(body)");
			brp.parseOut("((body))");
			assert brp.parse("(body") == null;
			assert brp.parse("(body))") == null;
		}
	}

	@Test
	public void testBug() {
		Parser dt = regex("(month|day|year)");
		Parser m2 = seq(dt, space, regex("\\d+"));
		Parser time = or(lit("now"), m2);
		dt.parseOut("month");
		m2.parseOut("month 2");
		time.parseOut("month 2");
	}

	@Test
	public void testChain() {
		{
			ChainParser abs = chain(lit("a", "b"), space);
			// abs.parseOut("a");
			abs.parseOut("a a");
			abs.parseOut("a a a");
			abs.parseOut("a b");
			abs.parseOut("b a");
			abs.parseOut("a b b a b");
			assert abs.parse("a ") == null;
			assert abs.parse("aa") == null;
			assert abs.parse("ab") == null;
			assert abs.parse("b ") == null;
			assert abs.parse("ba") == null;
			assert abs.parse("a c") == null;
			assert abs.parse("a ab") == null;
		}
	}

	@Test
	public void testLit() {
		{
			Parser.clearGrammar();
			Parser op = new Lit("+");
			op.parseOut("+");
			assert op.parse("") == null;
			assert op.parse("-") == null;
		}
		{
			Parser.clearGrammar();
			Parser op = lit("op", "+", "-", "*", "/");
			ParseResult pr = op.parse("+");
			assert pr != null;

			pr = op.parse("");
			assert pr == null;
			pr = op.parse("!");
			assert pr == null;
		}
		{
			Parser.clearGrammar();
			Parser colon = lit(":");
			ParseResult pr = colon.parseOut(":");
			assert pr != null;
			assert colon.parse("x") == null;
			assert colon.parse("") == null;
			opt(colon).parseOut("");
		}
	}

	@Test
	public void testLostLiteral() {
		{
			Parser p = lit("+", "*", "-", "/").label("op");
			ParseResult pr = p.parseOut("*");
			assert pr.ast.getName().equals("op") : pr.ast;
		}
		{
			Parser p = seq(lit("a"), lit("+"));
			ParseResult ab = p.parse("a+");
			// actually an unlabelled lit does not create child nodes
			// assert ab.ast.getChildren().size() == 2 :
			// ab.ast+" with "+ab.ast.getChildren();
		}
		{
			Parser op0 = lit("+", "*", "-", "/").label("op");
			ParseResult pr0 = op0.parse("*");
			assert pr0.ast.getName().equals("op");

			Parser op = seq(opt(space), op0, opt(space));
			ParseResult pr = op.parseOut(" * ");
			assert pr.ast.getChildren().size() == 1 : pr.ast;
			Parser aOpb = seq(lit("a").label("a"), op, lit("b").label("b"));
			ParseResult ab = aOpb.parse("a + b");
			assert ab.ast.getChildren().size() == 3 : ab.ast + " with "
					+ ab.ast.getChildren();
		}
	}

	// @Test
	// public void testTime() {
	// Parser.clearGrammar();
	// Parser time = TimeParser.getDefault().time;
	// time.parseOut("3pm tuesday");
	// time.parseOut("2:30 22/10/2010");
	// time.parseOut("2:30 22/10/10");
	// time.parseOut("3pm tuesday 22nd october");
	// time.parseOut("3pm tuesday 22nd october 2001 gmt");
	// // ParseResult pr = time.parseOut("3:15pm 22nd october 2001 gmt+1");
	// ParseResult pr = time.parseOut("3:15pm weds");
	// }

	@Test
	public void testOr() {
		Parser.clearGrammar();
		{
			Parser or = or(lit("+"), lit("-"));
			or.parseOut("+");
			or.parseOut("-");
			assert or.parse("a") == null;
			assert or.parse("") == null;
			assert or.parse("++") == null;
		}
		Parser.clearGrammar();
		{
			Parser or = or(lit("+"), lit("-"), lit("*"), lit("/"));
			or.parseOut("+");
			or.parseOut("-");
			or.parseOut("*");
			or.parseOut("/");
			assert or.parse("a") == null;
			assert or.parse("") == null;
			assert or.parse("++") == null;
		}
	}

	@Test
	public void testPP() {
		final Parser unit = new PP(regex("(month|day|year)s?")) {
			@Override
			protected Object process(ParseResult pr) {
				Object x = pr.ast.getX();
				String us = ((MatchResult) x).group(1).toUpperCase();
				TUnit u = TUnit.valueOf(us);
				return u;
			}
		};
		ParseResult pr0 = unit.parseOut("months");
		assert TUnit.MONTH.equals(pr0.ast.getX()) : pr0.ast;
		{
			ParseResult r = seq(num("n"), space, unit).parseOut("2 months");
			Object u = r.getX(unit);
			assert u instanceof TUnit : u;
			AST n = r.getNode("n");
			Object nm = n.getX();
			assert nm != null : r;
		}
		Parser dt = new PP(seq(num("n"), space, unit)) {
			@Override
			protected Object process(ParseResult pr) {
				AST un = pr.ast.getNode(unit);
				assert un != null : pr;
				TUnit u = (TUnit) un.getX();
				AST numNode = pr.ast.getNode("n");
				assert numNode != null;
				double n = Double.parseDouble(numNode.getValue().toString());
				Double n2 = (Double) numNode.getX();
				assert n == n2 : n + " vs " + n2;
				return new Dt(n, u);
			}
		};
		ParseResult pr = dt.parseOut("2 months");
		assert new Dt(2, TUnit.MONTH).equals(pr.ast.getX()) : pr.ast;
	}

	@Test
	public void testRecursion() {
		{
			Parser.clearGrammar();
			Parser p1 = lit("a").label("p1");
			Parser p1r = ref("p1");
			p1r.parseOut("a");
			Parser p2 = seq(p1r, lit("b")).label("p2");
			p2.parseOut("ab");
			Parser p2r = ref("p2");
			p2r.parseOut("ab");
		}
		{
			Parser.clearGrammar();
			Parser num = num("n");
			Parser p = first(seq(num, lit("+"), ref("formula")), num).label(
					"formula");
			p.parseOut("1+1");
			p.parseOut("1+1+2");
		}
		{ // infinite loop
			Parser.clearGrammar();
			Parser num = num("n");
			Parser p = first(seq(ref("formula"), lit("+"), ref("formula")), num)
					.label("formula");
			assert p.parse("blah") == null;
			p.parseOut("1+1");
			p.parseOut("1+1+2");
		}
	}

	// @Test TODO
	public void testRecursion2WithOr() {
		{ // using or
			Parser.clearGrammar();
			Parser num = regex("\\d+");
			ParseResult npr = num.parse("2");
			assert npr != null;
			Parser p = or(num, seq(num, lit("+"), ref("formula"))).label(
					"formula");
			p.parseOut("2+2");
			p.parseOut("2+2+3");
		}
		{
			Parser.clearGrammar();
			Parser num = regex("\\d+");
			ParseResult npr = num.parse("2");
			assert npr != null;
			Parser p = seq(num, opt(space), lit("+"),
			// lit("op", "+", "-"),
					opt(space), or(ref("formula"), num)).label("formula");
			p.parseOut("2+2");
			p.parseOut("2 + 2 - 3");
		}
		{
			Parser.clearGrammar();
			Parser num = regex("\\d+");
			ParseResult npr = num.parse("2");
			assert npr != null;
			Parser p = seq(or(ref("formula"), num), lit("+"),
					or(ref("formula"), num)).label("formula");
			p.parseOut("2+2");
		}
	}

	@Test
	public void testRegex() {
		{
			Parser.clearGrammar();
			Parser op = regex("a");
			op.parseOut("a");
			assert op.parse("") == null;
			assert op.parse("-") == null;
		}
		{
			Parser.clearGrammar();
			Parser op = regex("a*");
			op.parseOut("a");
			op.parseOut("aaaaa");
			op.parseOut("");
			assert op.parse("-") == null;
			assert op.parse("ax") == null;
		}
	}

	@Test
	public void testSeq() {
		Parser.clearGrammar();
		{
			Parser p = new SeqnParser(lit("a"), lit("b"));
			p.parseOut("ab");
			assert p.parse("wibble") == null;
			assert p.parse("adbc") == null;
			assert p.parse("a") == null : p.parseOut("a");
			assert p.parse("b") == null;
			assert p.parse("aab") == null;
			assert p.parse("aba") == null;
			assert p.parse("abb") == null;
			assert p.parse("abab") == null;
		}
		Parser.clearGrammar();
		{
			Parser p = new SeqnParser(lit("alice"), lit("bob"));
			p.parseOut("alicebob");
			assert p.parse("wibble") == null;
			assert p.parse("alice") == null;
		}
	}

	@Test
	public void testSeq2() {
		{
			Parser a = lit("a");
			Parser b = lit("b");
			Parser c = lit("c");
			Parser bc = new SeqnParser(b, c).label("bc");
			SeqnParser p = new SeqnParser(a, bc);
			p.parseOut("abc");
			assert p.parse("wibble") == null;
			assert p.parse("acb") == null;
			assert p.parse("abcabc") == null;
			assert p.parse("abca") == null;
			assert p.parse("abcbc") == null;
			assert p.parse("abcc") == null;
			assert p.parse("aabc") == null;
			assert p.parse("ac") == null;
			assert p.parse("abbc") == null;
		}
		{
			Parser a = lit("a");
			Parser b = lit("b");
			Parser c = lit("c");
			Parser d = lit("d");
			Parser e = lit("e");
			Parser p = seq(a, b, c, d, e);
			p.parseOut("abcde");
			assert p.parse("wibble") == null;
			assert p.parse("acb") == null;
			assert p.parse("abcabc") == null;
			assert p.parse("abca") == null;
			assert p.parse("abcbc") == null;
			assert p.parse("abcc") == null;
			assert p.parse("aabc") == null;
			assert p.parse("ac") == null;
			assert p.parse("abbc") == null;
		}
		{
			Parser p = seq(or(lit("+"), lit("-")));
			p.parseOut("+");
			p.parseOut("-");
			assert p.parse("a") == null;
		}
	}

	@Test
	public void testSeq3() {
		{
			Parser.clearGrammar();
			Parser a = regex("a+");
			Parser b = regex("b+");
			Parser p = new SeqnParser(a, b);
			p.parseOut("ab");
			p.parseOut("aabbbb");
		}
		{
			Parser.clearGrammar();
			Parser a = regex("a+");
			Parser b = regex("b+");
			Parser c = regex("c+");
			Parser bc = new SeqnParser(b, c).label("bc");
			SeqnParser p = new SeqnParser(a, bc);
			p.parseOut("abc");
			p.parseOut("aabbbbbc");
			p.parseOut("abcccc");
			p.parseOut("aaaaabc");
			assert p.parse("wibble") == null;
			assert p.parse("acb") == null;
			assert p.parse("abcabc") == null;
			assert p.parse("abca") == null;
		}
	}

	@Test
	public void testSeqKids() {
		{
			Parser p = seq(lit("if").label("op"), space,
					lit("true").label("guts"));
			ParseResult r = p.parseOut("if true");
			List ls = r.getLeaves();
			assert ls.size() == 2 : ls;
		}
		{
			Parser p = seq(
					lit("if").label("op"),
					space,
					first(lit("true").label("guts"), lit("false")
							.label("guts2")));
			ParseResult r = p.parseOut("if true");
			List<AST> ls = r.getLeaves();
			assert ls.size() == 2 : ls;
			assert ls.get(0).parsed().equals("if");
			assert ls.get(1).parsed().equals("true");
		}
		{
			Parser t = ref("t");
			Parser t2 = lit("true").label("t");
			Parser p = seq(lit("if").label("op"), space, t);
			ParseResult r = p.parseOut("if true");
			List<AST> ls = r.getLeaves();
			assert ls.size() == 2 : ls;
			assert ls.get(0).parsed().equals("if");
			assert ls.get(1).parsed().equals("true");
		}
	}

	@Test
	public void testSeqWithFirst() {
		Parser.clearGrammar();
		Parser num = regex("\\d+");
		{
			Parser or = first(lit("+"), lit("-"));
			SeqnParser p = new SeqnParser(or, num);
			p.parseOut("+2");
			p.parseOut("-12");
			assert p.parse("+") == null;
			assert p.parse("-") == null;
			assert p.parse("2") == null;
			assert p.parse("a") == null;
		}
		{
			Parser p = seq(lit("a"), opt(space), lit("b"), opt(lit("c")));
			p.parseOut("a bc");
			p.parseOut("a b");
			p.parseOut("ab");
			assert p.parse("a b c") == null;
			assert p.parse("a c") == null;
			assert p.parse("bc") == null;
		}
	}

	// @Test TODO
	public void testSeqWithOr() {
		Parser.clearGrammar();
		Parser num = regex("\\d+");
		{
			Parser or = or(lit("+"), lit("-"));
			SeqnParser p = new SeqnParser(or, num);
			p.parseOut("+2");
			p.parseOut("-12");
			assert p.parse("+") == null;
			assert p.parse("-") == null;
			assert p.parse("2") == null;
			assert p.parse("a") == null;
		}
	}

	// @Test TODO
	public void testSeqWithOr2() {
		Parser.clearGrammar();
		Parser num = regex("\\d+");
		{
			Parser or = or(lit("+"), lit("-"));
			SeqnParser p = new SeqnParser(num, or);
			p.parseOut("2+");
			p.parseOut("12-");
			assert p.parse("2") == null;
			assert p.parse("-") == null;
		}
		{
			Parser p = seq(or(lit("+"), lit("-")), num);
			p.parseOut("+2");
			p.parseOut("-12");
			assert p.parse("a") == null;
		}
		{
			Parser p = seq(num, lit("op", "+", "-"), num);
			p.parseOut("2+2");
		}
		{
			Parser p = seq(lit("a"), opt(space), lit("b"), opt(lit("c")));
			p.parseOut("a bc");
			p.parseOut("a b");
			p.parseOut("ab");
			assert p.parse("a b c") == null;
			assert p.parse("a c") == null;
			assert p.parse("bc") == null;
		}
	}

}
