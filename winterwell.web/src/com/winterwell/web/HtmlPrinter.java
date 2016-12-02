package com.winterwell.web;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.web.IHasHtml;

/**
 * Produce HTML representation for objects such as maps and lists. Strings
 * undergo some simple conversion. Warning: HTML inside a String will be
 * mangled.
 * 
 * @author daniel
 * 
 */
public class HtmlPrinter {

	/**
	 * Actually, sod this. What we should do is JavaScript style extra
	 * functions. E.g. map class to {@link Method} Custom HTML printer, for
	 * extending {@link HtmlPrinter}.
	 */
	public static interface ToHtml<X> {
		/**
		 * @param obj
		 *            Create an html representation for this
		 * @param sb
		 *            Append the html to this
		 * @param depth
		 *            This is (a lower bound on) the current depth in the
		 *            printing operation.
		 */
		void append(X obj, StringBuilder sb, int depth);
	}

	static final HashMap<Class, ToHtml> converters = new HashMap<Class, ToHtml>();

	static int MAX_DEPTH = 4;

	static Pattern RECOVER_SAFE_TAGS = Pattern.compile(
			"&lt;(/?(i|b|br|p|em|strong|code|span))&gt;",
			Pattern.CASE_INSENSITIVE);

	public static void appendMap(StringBuilder sb, Map<?, ?> x, int depth) {
		depth++;
		sb.append("<table>");
		for (Entry e : x.entrySet()) {
			sb.append("<tr><td>");
			toString(e.getKey(), sb, depth);
			sb.append("</td><td>");
			toString(e.getValue(), sb, depth);
			sb.append("</td></tr>");
		}
		sb.append("</table>");
	}

	/**
	 * TODO potentially more efficient base for {@link #toString(Object)}.
	 * 
	 * @param sb
	 * @param x
	 */
	public static void appendTo(StringBuilder sb, Object x) {
		sb.append(toString(x));
	}

	/**
	 * Produce HTML representation for objects such as maps and lists. Strings
	 * undergo some simple conversion. Warning: HTML inside a String will be
	 * mangled.
	 * 
	 * @param x
	 * @return html for x
	 */
	public static String toString(Object x) {
		StringBuilder sb = new StringBuilder();
		toString(x, sb, 0);
		return sb.toString();
	}

	private static void toString(Object x, StringBuilder sb, int i) {
		if (x == null) {
			sb.append(" ");
			return;
		}
		if (i == MAX_DEPTH) {
			sb.append("...");
			return;
		}
		// Do we have a custom converter set?
		ToHtml conv = converters.get(x.getClass());
		if (conv != null) {
			conv.append(x, sb, i);
			return;
		}
		// Does the class have a convertor?
		if (x instanceof IHasHtml) {
			((IHasHtml) x).appendHtml(sb);
			return;
		}
		if (x instanceof Map) {
			appendMap(sb, (Map) x, i);
			return;
		}
		if (x instanceof List) {
			toStringList((List) x, sb, i);
			return;
		}
		if (x instanceof Iterable) {
			toStringSet(Containers.getList((Iterable) x), sb, i);
			return;
		}
		if (x.getClass().isArray()) {
			toStringList(Containers.asList(x), sb, i);
			return;
		}
		if (x instanceof String) {
			// convert to html -- but protect some simple safe tags
			String s = (String) x;
			// Two line breaks into two line breaks
			s = s.replace("\r?\n\r?\n", "<br><br>");
			// Some entities
			s = s.replace("&", "&amp;");
			s = s.replace("£", "&pound;");
			s = s.replace("<", "&lt;");
			s = s.replace(">", "&gt;");

			s = RECOVER_SAFE_TAGS.matcher(s).replaceAll("<$1>");
			sb.append(s);
			return;
		}
		
		// Fallback
		sb.append(toString(Printer.toString(x)));
	}

	private static void toStringList(List x, StringBuilder sb, int depth) {
		depth++;
		sb.append("<ol>");
		for (Object v : x) {
			sb.append("<li><p>");
			toString(v, sb, depth);
			sb.append("</p></li>");
		}
		sb.append("</ol>");
	}

	private static void toStringSet(Collection set, StringBuilder sb, int depth) {
		depth++;
		sb.append("<ul>");
		for (Object v : set) {
			sb.append("<li><p>");
			toString(v, sb, depth);
			sb.append("</p></li>");
		}
		sb.append("</ul>");
	}

}
