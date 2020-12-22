package com.winterwell.nlp.simpleparser;

import static com.winterwell.nlp.simpleparser.Parsers.first;
import static com.winterwell.nlp.simpleparser.Parsers.opt;
import static com.winterwell.nlp.simpleparser.Parsers.regex;
import static com.winterwell.nlp.simpleparser.Parsers.seq;
import static com.winterwell.nlp.simpleparser.Parsers.space;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.winterwell.utils.Printer;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * @deprecated
 * @testedby TimeParserTest
 * @author daniel
 * 
 */
public class TimeParser {

	private static TimeParser dflt = new TimeParser();

	public static TimeParser getDefault() {
		return dflt;
	}

	Parser day = regex(
			"(monday|mon|tuesday|tues|tue|wednesday|weds|wed|thursday|thurs|thur|friday|fri|saturday|sat|sunday|sun|today|yesterday|tomorrow)")
			.label("day");
	Parser yr = regex("/?(\\d\\d|')?\\d\\d(ad|bc)?").label("yr");

	Parser longDate = seq(
			regex("(\\d\\d?)(st|nd|rd|th)? (january|jan|february|febuary|feb|march|mar|april|apr|june|jun|july|jly|jul|august|aug|september|sept|sep|october|oct|november|nov|december|dec)"),
			opt(seq(space, yr)));
	Parser shortDate = seq(regex("\\d\\d/\\d\\d"), opt(yr));
	Parser tod = regex("(\\d\\d?)(:\\d\\dam|:\\d\\dpm|:\\d\\d|am|pm)").label(
			"time");
	Parser zone = regex("(gmt|est|utc|bst)((\\+|-)?\\d\\d?)?").label("zone");
	Parser time = seq(opt(tod), opt(space), opt(day), opt(space), opt(tod),
			opt(space), opt(first(longDate, shortDate)), opt(space), opt(zone));

	public Time parseTime(String txt) {
		ParseResult pr = time.parse(txt);
		if (pr == null)
			return null;
		AST<?> ast = pr.ast;
		Calendar cal = new GregorianCalendar(TimeUtils._GMT_TIMEZONE);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		// TODO should go year, month, day to get the day rolls correct
		for (Object n : ast.flatten()) {
			AST<?> node = (AST) n;
			if (node.getValue() == null || node.getValue().length() == 0) {
				continue;
			}
			if (node.isNamed("day")) {
				parseTime2_processDay(node, cal);
				continue;
			}
			if (node.isNamed("yr")) {
				parseTime2_processYear(cal, node);
				continue;
			}
			if (node.isNamed("time")) {
				parseTime2_time(cal, node);
				continue;
			}
			if (node.isNamed("zone")) {
				String zn = node.getValue().toString().toUpperCase();
				TimeZone tz = TimeZone.getTimeZone(zn);
				cal.setTimeZone(tz);
				continue;
			}
		}
		return new Time(cal);
	}

	private void parseTime2_processDay(AST<?> node, Calendar cal) {
		String d = node.getValue().subSequence(0, 3).toString();
		int v = -1;
		if ("mon".equals(d)) {
			v = Calendar.MONDAY;
		} else if ("tue".equals(d)) {
			v = Calendar.TUESDAY;
		} else if ("wed".equals(d)) {
			v = Calendar.WEDNESDAY;
		} else if ("thu".equals(d)) {
			v = Calendar.THURSDAY;
		} else if ("fri".equals(d)) {
			v = Calendar.FRIDAY;
		} else if ("sat".equals(d)) {
			v = Calendar.SATURDAY;
		} else if ("sun".equals(d)) {
			v = Calendar.SUNDAY;
		} else
			throw new IllegalArgumentException(node.toString());
		int old = cal.get(Calendar.DAY_OF_WEEK);
		int dv = v - old;
		if (dv < 0) {
			dv += 7;
		}
		cal.roll(Calendar.DAY_OF_YEAR, dv);
		int nw = cal.get(Calendar.DAY_OF_WEEK);
		Printer.out(nw);
	}

	private void parseTime2_processYear(Calendar cal, AST node) {
		String s = node.getValue().toString();
		boolean bc = false;
		int v = -1;
		if (s.endsWith("ad")) {
			s = s.substring(0, s.length() - 2);
		}

		if (s.endsWith("bc")) {
			bc = true;
			s = s.substring(0, s.length() - 2);
			v = -Integer.parseInt(s);
		} else {
			if (s.length() == 4) {
				v = Integer.parseInt(s);
			} else if (s.length() == 2 && !bc) {
				v = Integer.parseInt(s);
				// HACK: Which century are we in?
				if (v < 51) {
					v += 1900;
				} else {
					v += 2000;
				}
			}
		}
		cal.set(Calendar.YEAR, v);
	}

	private void parseTime2_time(Calendar cal, AST node) {
		String s = node.getValue().toString();
		String hr = s.subSequence(0, Character.isDigit(s.charAt(1)) ? 2 : 1)
				.toString();
		int h = Integer.parseInt(hr);
		assert h < 25 : s;
		// assume 24 hour
		if (s.contains("pm")) {
			h += 12;
		}
		cal.set(Calendar.HOUR_OF_DAY, h);
		int i = s.indexOf(':');
		if (i != -1) {
			String min = s.substring(i + 1, i + 3);
			int m = Integer.parseInt(min);
			assert m < 61 : s;
			cal.set(Calendar.MINUTE, m);
		}
	}

}
