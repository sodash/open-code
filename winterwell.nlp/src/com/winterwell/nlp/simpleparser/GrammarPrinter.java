package com.winterwell.nlp.simpleparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;

/**
 * Print out the structure of a language. Doesn't quite use Backus–Naur Form
 * (BNF)
 * 
 * @author daniel
 * 
 */
public class GrammarPrinter {

	/**
	 * Show First rules as a|b -- otherwise defaults to a/b (non-standard but
	 * unambiguous)
	 */
	public boolean useOrMarkForFirst;
	/**
	 * Show optional elements as x? (regex stylee) -- otherwise defaults to [x]
	 * (EBNF stylee)
	 */
	public boolean useQMarkForOptional;

	public GrammarPrinter() {
	}

	private void addToPrint(Parser p, List<Parser> toPrint, List<Parser> printed) {
		assert p != null;
		if (!printed.contains(p)) {
			toPrint.add(p);
		}
	}

	private String cleanName(String name) {
		return FileUtils.safeFilename(name, false);
	}

	public String print(Parser p) {
		StringBuilder sb = new StringBuilder();
		print2(sb, new ArrayList<Parser>(Arrays.asList(p)),
				new ArrayList<Parser>());
		return sb.toString().trim();
	}

	void print2(StringBuilder sb, List<Parser> toPrint, List<Parser> printed) {
		while (!toPrint.isEmpty()) {
			Parser p = toPrint.remove(0);
			if (printed.contains(p)) {
				continue;
			}
			print3_rule(p, sb, toPrint, printed);
		}
	}

	void print3_rule(Parser p, StringBuilder sb, List<Parser> toPrint,
			List<Parser> printed) {
		assert p != null;
		assert sb != null && toPrint != null && printed != null : p;
		if (p instanceof Ref) {
			Parser p2 = ((Ref) p).lookup();
			assert p2 != null : "Ref failed: "+p.name;
			addToPrint(p2, toPrint, printed);
			return;
		}
		if (printed.contains(p)) {
			Log.report("Repetition? " + p);
			return;
		}
		printed.add(p);
		StrUtils.newLine(sb);
		String name = p.getName();
		name = cleanName(name);
		sb.append("\n<" + name + "> ::= ");
		// skip print4 to make sure we expand the rule. Otherwise get <a> ::=
		// <a>
		print5_body2(p, sb, toPrint, printed);
	}

	private void print4_body(Parser p, StringBuilder sb, List<Parser> toPrint,
			List<Parser> printed) {
		if (!Utils.isBlank(p.getName())) {
			String name = p.getName();
			name = cleanName(name);
			sb.append("<" + name + ">");
			addToPrint(p, toPrint, printed);
			return;
		}
		print5_body2(p, sb, toPrint, printed);
	}

	private void print5_body2(Parser p, StringBuilder sb, List<Parser> toPrint,
			List<Parser> printed) {
		p = resolve(p);
		if (p instanceof Parsers.Opt) {
			// (E)BNF uses [optional] but perhaps optional? is better
			if (!useQMarkForOptional) {
				sb.append("[");
			}
			print4_body(p.subs[0], sb, toPrint, printed);
			if (useQMarkForOptional) {
				sb.append("?");
			} else {
				sb.append("]");
			}
			return;
		} else if (p instanceof First) {
			print6_subs((useOrMarkForFirst ? "|" : "/"), p, sb, toPrint,
					printed);
			return;
		} else if (p instanceof ChainParser) {
			ChainParser cp = (ChainParser) p;
			sb.append("{");
			print4_body(cp.element, sb, toPrint, printed);
			sb.append(", ");
			print4_body(cp.separator, sb, toPrint, printed);
			sb.append("}+");
			return;
		} else if (p instanceof SeqnParser) {
			print6_subs(" ", p, sb, toPrint, printed);
		} else if (p instanceof Lit || p instanceof RegexParser
				|| p instanceof Parsers.Word) {
			sb.append('"' + p.toString() + '"');
		} else if (p instanceof Ref) {
			sb.append('<' + p.name + '>');
			Parser p2 = ((Ref) p).lookup();
			addToPrint(p2, toPrint, printed);
			return;
		} else {
			Log.report(p.getClass() + ": " + p);
			sb.append(p.toString());
		}
	}

	private void print6_subs(String separator, Parser p, StringBuilder sb,
			List<Parser> toPrint, List<Parser> printed) {
		if (p.subs.length == 0)
			return;
		for (Parser sub : p.subs) {
			print4_body(sub, sb, toPrint, printed);
			sb.append(separator);
		}
		StrUtils.pop(sb, separator.length());
	}

	Parser resolve(Parser p) {
		while (true) {
			if (p instanceof PP) {
				PP pp = ((PP) p);
				p = pp.parser;
				// } else if (p instanceof Ref) {
				// p = ((Ref)p).lookup();
			} else {
				break;
			}
		}
		return p;
	}

}
