package com.winterwell.ical;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.FastDateFormat;

import com.winterwell.utils.time.Time;

public class ICalWriter {

	StringBuilder ical = new StringBuilder();
	
	public ICalWriter() {
		this(null);
	}
	
	public ICalWriter(String name) {
		ical.append("BEGIN:VCALENDAR\r\n");
		ical.append("VERSION:2.0\r\n");
		ical.append("PRODID:-//sodash//NONSGML SoDashv0.1//EN\r\n");
		ical.append("X-WR-TIMEZONE:UTC" + "\r\n");
		if (name != null){
			ical.append("X-WR-CALNAME;VALUE=TEXT:" + name + "\r\n");
		}
	}

	/**
	 * 
	 * @param start
	 * @param end can be null, interpreted as 1 minute
	 * @param summary
	 */
	public void addEvent(Time start, Time end, String summary) {
		addEvent(new ICalEvent(start, end, summary));
	}
	
	public void addEvent(ICalEvent event) {		
		ical.append(event.toString());
	}
	
	/**
	 * Escape bits of text.
	 * https://tools.ietf.org/html/rfc5545#section-3.3.11
	 * 
	 * Rules:
	 * 1. a double quote becomes \"
	 * 2. a comma becomes \,
	 * 3. a semicolon becomes \;
	 * 4. a backslash becomes \\
	 * 5. a newline becomes \n.
	 *
	 * ESCAPED-CHAR = ("\\" / "\;" / "\," / "\N" / "\n")
	 * ; \\ encodes \, \N or \n encodes newline
	 * ; \; encodes ;, \, encodes ,
	 * 
	 * TSAFE-CHAR = WSP / %x21 / %x23-2B / %x2D-39 / %x3C-5B / %x5D-7E / NON-US-ASCII
	 * 
	 * FIXME: colons are used as property values delimiters. If we want to use different 
	 * property values in property parameters, then values must be surrounded in quotes
	 * 
	 * @param text
	 * @return the text in a proper ical format
	 */
	public static String formatText(String text) {
		Pattern p = Pattern.compile("[\",;\\\\\n]");
		Matcher m = p.matcher(text);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String repl = "\\\\\\" + m.group();
			m.appendReplacement(sb, repl);
		}
		m.appendTail(sb);
				
		String escapedText = sb.toString();
		

//		 Lines of text SHOULD NOT be longer than 75 octets, excluding the line
//		   break.  Long content lines SHOULD be split into a multiple line
//		   representations using a line "folding" technique.  That is, a long
//		   line can be split between any two characters by inserting a CRLF
//		   immediately followed by a single linear white-space character (i.e.,
//		   SPACE or HTAB).  Any sequence of CRLF followed immediately by a
//		   single linear white-space character is ignored (i.e., removed) when
//		   processing the content type.
		
		if (escapedText.length()<72) {
			return escapedText;
		}
		// Break into 70 char lines
		StringBuilder sum2 = new StringBuilder(escapedText.length()+5);
		int i=0;
		for(char c : escapedText.toCharArray()) {
			sum2.append(c);
			i++;
			if (i%70 == 0) {
				sum2.append("\r\n ");
			}
		}
		String choppedText = sum2.toString();

		return choppedText;
	}

	/**
	 * Date-time format -- see https://tools.ietf.org/html/rfc5545#section-3.8.2
	 */
	static String format(Time start) {
		// date-value         = date-fullyear date-month date-mday
		
		//FORM #2: DATE WITH UTC TIME

//	      The date with UTC time, or absolute time, is identified by a LATIN
//	      CAPITAL LETTER Z suffix character, the UTC designator, appended to
//	      the time value.  
	      
//	      For example, the following represents January 19,
//	      1998, at 0700 UTC:
//
//	       19980119T070000Z		
		return sdf.format(start.getDate());
	}
	
	public static FastDateFormat sdf = FastDateFormat.getInstance("yyyyMMdd'T'HHmmss'Z'");	

	public String getICal() {
		return ical+"END:VCALENDAR\r\n";
	}
	
	/**
	 * Equivalent to {@link #getICal()}
	 */
	@Override
	public String toString() {
		return getICal();
	}


}