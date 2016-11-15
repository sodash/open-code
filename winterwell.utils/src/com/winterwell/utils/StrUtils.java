package com.winterwell.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import winterwell.utils.FailureException;
import winterwell.utils.IFn;
import winterwell.utils.IReplace;
import winterwell.utils.reporting.Log.KErrorPolicy;
import winterwell.utils.reporting.WeirdException;

import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;

import winterwell.utils.containers.IntRange;

import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;


/**
 * @see WebUtils for XML-related String handling
 * @testedby {@link StrUtilsTest}
 */
public class StrUtils extends winterwell.utils.StrUtils {


	public static final String APOSTROPHES = "'`’‘’ʼ";

	/**
	 * For use in {@link Collection#toArray(Object[])} to get String[] arrays.
	 */
	public static final String[] ARRAY = new String[0];

	/**
	 * Regex character set for ascii punctuation. A bit like \\W, but includes _
	 * and doesn't include non-Latin characters (\\W barfs on foreign character
	 * sets).
	 */
	public static final Pattern ASCII_PUNCTUATION = Pattern
			.compile("[.<>,@~\\{\\}\\[\\]-_+=()*%?^$!\\\\/|¬:;#`'\"]");

	static final Pattern BLANK_LINE = Pattern.compile("^\\s+$",
			Pattern.MULTILINE);

	/**
	 * Some commonly used bullet characters (for trying to spot lists, and
	 * perhaps converting them into a standard format)
	 */
	public static final String COMMON_BULLETS = "-*o";

	/**
	 * Safety defence against some of the bad characters you meet online.
	 * @param text
	 * @return text, almost certainly unchanged.
	 */
	public static String sanitise(final String text) {
		// UI-destroying LINE-SEPERATOR character of doom. It's really rare, so lets just replace it.
		String _text = text.replace('\u2028', ' ');
		//			Occasionally tweets have zero byte chars - always seems to be the initial char.
		//			 NOTE: I have edited JTwitter to remove them, so this is probably obsolete. But harmless.
		// cause of org.postgresql.util.PSQLException: Zero bytes may not occur in string parameters?
		_text = _text.replace((char)0, '?');
		if (text != _text) {
			Log.w("StrUtils","Bogus char removed from text: "+StrUtils.ellipsize(text, 40));
		}
		return _text;
	}

	/**
	 * Dash characters, including the humble -
	 */
	private static final String DASHES = "‐‑‒–—―-";
	/**
	 * There are a few non-standard space characters! Of which the &nbsp;
	 * unicode character is perhaps the only "common" one.
	 * http://www.fileformat.info/info/unicode/char/a0/index.htm
	 */
	private static final String SPACES = "\u00A0\u2007\u202F\u200B";

	public static final String ENCODING_STD_ISO_LATIN = "ISO-8859-1";

	public static final String ENCODING_UTF8 = "UTF8";

	public static final String LINEEND = Utils.or(
			System.getProperty("line.separator"), "\n");

	public static final Pattern LINEENDINGS = Pattern.compile("(\r\n|\r|\n)");
	/** A paragraph of random latin, used for page layout testing since 1500s. */
	public static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

	public static final String QUOTES = "\"“”„‟❛❜❝❞«»";

	private static final double[] TENS = new double[20];

	static {
		TENS[0] = Math.pow(10, -6);
		for (int i = 1; i < TENS.length; i++) {
			TENS[i] = 10 * TENS[i - 1];
		}
	}

	/**
	 * Convenience for peeking at a character which might be beyond the end of
	 * the sequence.
	 * 
	 * @param chars
	 * @param i
	 * @return the char at index i, if within range, or 0 otherwise.
	 */
	public static char charAt(CharSequence chars, int i) {
		return i < chars.length() ? chars.charAt(i) : 0;
	}

	/**
	 * Trim and compress all whitespace into single spaces. Also removes
	 * whitespace between xml tags.
	 * 
	 * @param txt
	 *            Can be null (which will return null)
	 * @return a string whose only whitespace is single spaces
	 * @see #toCanonical(String)
	 */
	public static String compactWhitespace(String txt) {
		if (txt == null)
			return null;
		txt = txt.trim();
		txt = txt.replaceAll("\\s+", " ");
		// non standard whitespace too -- which doesn't count in \\s :(
		txt = txt.replaceAll("[" + SPACES + "]", " ");
		txt = txt.replaceAll("> <", "><");
		return txt;
	}

	/**
	 * @param pageTitle
	 * @param snippet
	 * @return true if text contains snippet ignoring all capitalisation
	 */
	public static boolean containsIgnoreCase(CharSequence pageTitle,
			String snippet) {
		// TODO more efficient -- avoid the copy(s)?
		String pt = pageTitle.toString().toLowerCase();
		return pt.contains(snippet.toLowerCase());
	}

	/**
	 * Convert a block of text into a Java String constant
	 * 
	 * @param args
	 * @throws IOException
	 */
	private static String convertToJavaString(String txt) {
		String[] lines = splitLines(txt);
		String jtxt = "";
		for (String line : lines) {
			line = line.replace("\\", "\\\\");
			line = line.replace("\"", "\\\"");
			jtxt += "+\"" + line + "\\n\"\n";
		}
		jtxt = jtxt.substring(1);
		return jtxt;
	}

	/**
	 * Truncate a string adding ellipses if necessary. Does a simple test for
	 * word boundary for more elegant start of ellipses
	 * 
	 * @param input
	 *            Can be null (returns null)
	 * @return a string which is maxLength chars or less
	 * @testedby {@link StrUtilsTest#testEllipsize()}
	 */
	public static String ellipsize(String input, int maxLength) {
		if (input == null)
			return null;
		if (input.length() <= maxLength)
			return input;
		if (maxLength < 3)
			return "";
		if (maxLength == 3)
			return "...";
		// simple word boundary detection for nicer strings
		int i = input.lastIndexOf(' ', maxLength - 3);
		if (i < 1 || i < maxLength - 10) {
			i = maxLength - 3;
		}
		return substring(input, 0, i) + "...";
	}

	/**
	 * Extract headers of the form: key: value
	 * 
	 * Ended by a blank line
	 * 
	 * @param txt
	 *            Will be modified to the text minus the header
	 * @return The headers. <i>Keys will be converted to lower-case</i> but can
	 *         contain spaces.
	 */
	public static Map<String, String> extractHeader(StringBuilder txt) {
		assert txt != null;
		String[] lines = StrUtils.splitLines(txt.toString());
		int cnt = 0;
		String key = null;
		StringBuilder value = new StringBuilder();
		Map<String, String> headers = new ArrayMap<String, String>();
		for (; cnt < lines.length; cnt++) {
			String line = lines[cnt];
			// End of header section?
			if (Utils.isBlank(line)) {
				break;
			}
			int i = line.indexOf(":");
			if (i == -1) {
				// Collect bits of a long value
				value.append(LINEEND);
				value.append(line);
				continue;
			}
			// Old key
			if (key != null) {
				headers.put(key, value.toString());
			}
			// New key
			value = new StringBuilder();
			key = line.substring(0, i).toLowerCase();
			i++;
			if (i == line.length()) {
				continue;
			}
			if (line.charAt(i) == ' ') {
				i++;
			}
			if (i == line.length()) {
				continue;
			}
			value.append(line.substring(i));
		}
		// Final key-value pair
		if (key != null) {
			headers.put(key, value.toString());
		}
		// Strip off header
		if (headers.size() == 0)
			return headers;
		Pattern blankLine = Pattern.compile("^\\s*$", Pattern.MULTILINE);
		Matcher m = blankLine.matcher(txt);
		boolean ok = m.find();
		if (ok) {
			txt.delete(0, m.end());
			if (txt.length() != 0) {
				if (txt.charAt(0) == '\r' && txt.charAt(1) == '\n') {
					txt.delete(0, 2);
				} else {
					txt.delete(0, 1);
				}
			}
		}
		return headers;
	}

	/**
	 * Convenience for using regexs. Find the first instance of pattern in
	 * input.
	 * 
	 * @param pattern
	 * @param input
	 * @return the matched groups (0 is the whole match), or null
	 */
	public static String[] find(Pattern pattern, String input) {
		Matcher m = pattern.matcher(input);
		boolean fnd = m.find();
		if (!fnd)
			return null;
		int n = m.groupCount() + 1;
		String[] grps = new String[n];
		grps[0] = m.group();
		for (int i = 1; i < n; i++) {
			grps[i] = m.group(i);
		}
		return grps;
	}

	/**
	 * Convenience method for {@link #find(Pattern, String)}
	 * 
	 * @param regex
	 * @param string
	 * @return the matched groups (0 is the whole match), or null
	 */
	public static String[] find(String regex, String string) {
		return find(Pattern.compile(regex), string);
	}

	/**
	 * Find the position of content within text -- ignoring whitespace and
	 * unicode issues.
	 * 
	 * @param content
	 * @param text
	 * @param start
	 *            Go from here. 0 is the normal value.
	 * @return [start, end) position, or null if not found
	 */
	public static Pair<Integer> findLenient(String content, String text,
			int start) {
		// Note: these edits do not change the offsets
		content = StrUtils.normalise(content, KErrorPolicy.RETURN_NULL);
		text = StrUtils.normalise(text, KErrorPolicy.RETURN_NULL);
		content = content.toLowerCase();
		text = text.toLowerCase();

		// regex escape (can't use Pattern.quote() 'cos we do want flexible
		// whitespace)
		String regex = content.replace("\\", "\\\\");
		String SPECIAL = "()[]{}$^.*+?";
		for (int i = 0; i < SPECIAL.length(); i++) {
			char c = SPECIAL.charAt(i);
			regex = regex.replace("" + c, "\\" + c);
		}

		// turn whitespace in the quote into a whitespace pattern
		regex = regex.replaceAll("\\s+", "\\\\s+");
		// find it
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(text);
		if (m.find(start))
			return new Pair(m.start(), m.end());
		return null;
	}

	public static String getFirstName(String name) {
		name = name.trim();
		assert !name.contains("\n") : name;
		String[] nameBits = name.split("[ \t\\.,_]+");
		String firstName = nameBits[0];
		// don't send a card "Dear Dr"
		firstName = StrUtils.toTitleCase(firstName);
		List<String> titles = Arrays.asList("Mr", "Mrs", "Ms", "Dr", "Doctor",
				"Prof", "Professor", "Sir", "Director");
		if (titles.contains(firstName)) {
			firstName = nameBits[1];
			firstName = StrUtils.toTitleCase(firstName);
		}
		return firstName;
	}

	/**
	 * Inverse of {@link #extractHeader(StringBuilder)}. Not very robust
	 * 
	 * @param header
	 * @return
	 */
	public static String getHeaderString(Map header) {
		StringBuilder sb = new StringBuilder();
		for (Object k : header.keySet()) {
			String ks = k.toString().trim().toLowerCase();
			String vs = header.get(k).toString();
			sb.append(ks + ": " + vs + StrUtils.LINEEND);
		}
		return sb.toString();
	}

	/**
	 * @param text
	 * @return indexes for the first character of each line
	 */
	public static int[] getLineStarts(String text) {
		List<Integer> starts = new ArrayList<Integer>();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			// windows or linux style linebreak
			if (c == '\n') {
				starts.add(i);
			}
			if (c == '\r') {
				int ni = i + 1;
				if (ni == text.length() || text.charAt(ni) != '\n') {
					// Mac style linebreak
					starts.add(i);
				}
			}
		}
		return MathUtils.toIntArray(starts);
	}

	/**
	 * 
	 * @param hashAlgorithm e.g. "MD5"
	 * @param txt
	 * @return
	 */
	public static String hash(String hashAlgorithm, String txt) {
		if ("SHA256".equals(hashAlgorithm)) hashAlgorithm = "SHA-256";
		try {
			java.security.MessageDigest md = java.security.MessageDigest
					.getInstance(hashAlgorithm);
			StringBuffer result = new StringBuffer();
			try {
				for (byte b : md.digest(txt.getBytes(ENCODING_UTF8))) {
					result.append(Integer.toHexString((b & 0xf0) >>> 4));
					result.append(Integer.toHexString(b & 0x0f));
				}
			} catch (UnsupportedEncodingException e) {
				for (byte b : md.digest(txt.getBytes())) {
					result.append(Integer.toHexString((b & 0xf0) >>> 4));
					result.append(Integer.toHexString(b & 0x0f));
				}
			}
			return result.toString();
		} catch (java.security.NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Convenience for a surprisingly common case. Use this to guard statements
	 * like <code>Long.valueOf(String)</code>.
	 * 
	 * @param possNumber
	 * @return true for e.g. "123" or "-2"
	 * @testedby {@link StrUtilsTest#testIsInteger()}
	 */
	public static boolean isInteger(String possNumber) {
		if (possNumber==null || possNumber.isEmpty()) {
			return false;
		}
		for (int i = 0; i < possNumber.length(); i++) {
			char c = possNumber.charAt(i);
			if (Character.isDigit(c))
				continue;
			// an initial - is allowed
			if (i == 0 && c == '-' && possNumber.length() > 1)
				continue;
			return false;
		}
		return true;
	}

	/**
	 * @param x
	 *            Can be null (returns false)
	 * @return true if x is in fact a number
	 * 
	 *         ??Should we support non-standard formats such as "1,000", "10k"?
	 *         Not here!
	 */
	public static boolean isNumber(String x) {
		if (x == null)
			return false;
		try {
			// should we use a regex instead? \\d+(\\.\\d+)?
			Double.valueOf(x);
			// Reject the Java its-a-double or its-a-float syntax
			if (x.endsWith("d") || x.endsWith("f")) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 
	 * @param txt
	 * @param c
	 * @return true if txt only consists of the given character, e.g.
	 *         "--------". false for null and ""
	 */
	public static boolean isOnly(String txt, char c) {
		if (txt == null || txt.length() == 0)
			return false;
		for (int i = 0; i < txt.length(); i++) {
			if (txt.charAt(i) != c)
				return false;
		}
		return true;
	}

	/**
	 * @param txt Can be null (returns false)
	 * @return true if txt is a none-empty single alphanumeric string (with no
	 *         whitespace). Just a convenience for using the \w regex i.e.
	 *         [A-Za-z0-9_]
	 * @testedby {@link StrUtilsTest#testIsWord()}
	 */
	public static boolean isWord(String txt) {
		if (txt==null) return false;
		return WORD.matcher(txt).matches(); 		
	}
	private static final Pattern WORD = Pattern.compile("\\w+"); 

	/**
	 * Does this look like a wordlike token? This is more permissive than
	 * isWord()
	 * 
	 * @param txt
	 * @return true if txt is a non-empty single alphanumeric string possibly
	 *         containing period, underscore and hyphen
	 * @testedby {@link StrUtilsTest#testIsWordlike()}
	 */
	public static boolean isWordlike(String txt) {
		return txt.matches("[A-Za-z0-9_\\-.]+");
	}

	/**
	 * Return a string that is the string representation of the elements of list
	 * separated by separator
	 * <p>
	 * Identical to {@link Printer#toString(Collection, String)}
	 * 
	 * @see #join(StringBuilder, Collection, String)
	 */
	public static String join(Collection list, String separator) {
		return Printer.toString(list, separator);
	}

	/**
	 * Convenience for the case where the list has start & end markers. Return a
	 * string that is start + the string representation of the elements of list
	 * separated by separator + end. E.g. "(", [1,2,], " or ", ")" => "(1 or 2)"
	 * Skips null elements.
	 */
	public static <T> StringBuilder join(String start, Collection<T> list,
			String separator, String end) {
		StringBuilder sb = new StringBuilder(start);
		if (!list.isEmpty()) {
			for (T t : list) {
				if (t == null) {
					continue;
				}
				sb.append(Printer.toString(t));
				sb.append(separator);
			}
			if (sb.length() != 0) {
				pop(sb, separator.length());
			}
		}
		sb.append(end);
		return sb;
	}

	public static String join(String[] array, char separator) {
		return join(array, String.valueOf(separator));
	}

	/**
	 * TODO flip argument order and use ...
	 * 
	 * @param array nulls will be skipped. If the whole array is null, return ""
	 * @param separator
	 * @return
	 */
	public static String join(String[] array, String separator) {
		if (array==null || array.length == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		for (String string : array) {
			if (string == null) {
				continue;
			}
			sb.append(string);
			sb.append(separator);
		}
		if (sb.length() != 0) {
			pop(sb, separator.length());
		}
		return sb.toString();
	}

	/**
	 * TODO flip argument order and use ... instead of Object[]
	 * 
	 * Same as {@link #join(String[], String)}
	 * 
	 * @param array
	 * @param separator
	 * @return
	 */
	public static String join(Object[] array, String separator) {
		if (array.length == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		for (Object string : array) {
			if (string == null) {
				continue;
			}
			sb.append(string);
			sb.append(separator);
		}
		if (sb.length() != 0) {
			pop(sb, separator.length());
		}
		return sb.toString();
	}

	/**
	 * Append the string representation of the elements of list separated by
	 * separator.
	 * <p>
	 * Identical to {@link Printer#append(StringBuilder, Collection, String)}
	 * 
	 * @see #join(Collection, String)
	 */
	public static <T> void join(StringBuilder sb, Collection<T> list,
			String separator) {
		Printer.append(sb, list, separator);
	}

	/**
	 * Convert a block of text (read from the console) into a Java String
	 * constant
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String txt = "";
		BufferedReader in = FileUtils.getReader(System.in);
		while (true) {
			String line = in.readLine();
			if (line.equals("EXIT") || line.equals("QUIT")) {
				break;
			}
			txt += line + "\n";
		}
		String jtxt = convertToJavaString(txt);
		// WebUtils.stripTags(txt);
		System.out.println(jtxt);
	}

	/**
	 * @param txt
	 * @return MD5 hash of txt (32 hex digits). MD5 has known security flaws. But it is fast &
	 *         good for non-security uses.<br>
	 * 
	 *         See http://www.javamex.com/tutorials/cryptography/
	 *         hash_functions_algorithms.shtml
	 * @see #sha1(String)
	 */
	public static String md5(String txt) {
		return hash("MD5", txt);
	}

	/**
	 * @param txt
	 * @return SHA-1 hash of txt
	 */
	public static String sha1(String txt) {
		return hash("SHA1", txt);
	}

	/**
	 * Ensure that the string builder is pointing at a new line. I.e. if the
	 * last character is a line-end, this does nothing. if the last character is
	 * not a line-end, this adds a line end.
	 * 
	 * @param text
	 */
	public static void newLine(StringBuilder text) {
		if (text.length() == 0)
			return;
		char last = text.charAt(text.length() - 1);
		if (last == '\r' || last == '\n')
			return;
		text.append(LINEEND);
	}

	/**
	 * Ensure that the String ends with a new line. I.e. if the last character
	 * is a line-end, this does nothing. if the last character is not a
	 * line-end, this adds a line end.
	 * 
	 * @param text
	 */
	public static String newLine(String text) {
		if (text.length() == 0)
			return text;
		char last = text.charAt(text.length() - 1);
		if (last == '\r' || last == '\n')
			return text;
		return text + LINEEND;
	}

	/**
	 * Convert unicode text into a normalised Ascii form -- if we can. E.g.
	 * strip out umlauts.
	 * 
	 * @param unicode
	 * @return ascii text, contains "?" for unrecognised chars!
	 * @see #normalise(String, KErrorPolicy)
	 */
	public static String normalise(String unicode) {
		return normalise(unicode, KErrorPolicy.RETURN_NULL);
	}

	/**
	 * Convert unicode text into a normalised Ascii form -- if we can. E.g.
	 * strip out umlauts.
	 * 
	 * @param unicode Can be null (returns null)
	 * @param onUnrecognisableChar
	 *            If we cannot recognise a character, what to do?
	 *            KErrorPolicy.IGNORE will skip over un-normalised chars.
	 *            KErrorPolicy.ACCEPT will keep the un-normalised char.
	 *            KErrorPolicy.RETURN_NULL will substitute ? for unrecognised
	 *            chars.
	 * @return ascii text
	 * @testedby {@link StrUtilsTest#testNormalise()}
	 */
	public static String normalise(String unicode, Log.KErrorPolicy onUnrecognisableChar) 
			throws IllegalArgumentException 
	{
		if (unicode==null) return null;
		// all ascii anyway?
		boolean ascii = true;
		for (int i = 0, n = unicode.length(); i < n; i++) {
			char c = unicode.charAt(i);
			if (c > 127 || c == 0) {
				ascii = false;
				break;
			}
		}
		if (ascii)
			return unicode;
		// alternatively, we could use a lookup table
		// c.f. http://www.rgagnon.com/javadetails/java-0456.html which uses 2
		// aligned strings for lookup
		String normed = Normalizer.normalize(unicode, Normalizer.Form.NFD);
		StringBuilder clean = new StringBuilder(normed.length());
		for (int i = 0, n = normed.length(); i < n; i++) {
			char c = normed.charAt(i);
			if (APOSTROPHES.indexOf(c) != -1) {
				clean.append('\'');
				continue;
			}
			if (QUOTES.indexOf(c) != -1) {
				clean.append('"');
				continue;
			}
			if (DASHES.indexOf(c) != -1) {
				clean.append('-');
				continue;
			}
			if (SPACES.indexOf(c) != -1) {
				clean.append(' ');
				continue;
			}
			// ascii?
			// NB: filter out any bogus 0 chars (rarely needed, but a useful
			// safety measure)
			if (c < 128 && c != 0) {
				clean.append(c);
				continue;
			}
			// boolean hs = Character.isHighSurrogate(c);
			// boolean ls = Character.isLowSurrogate(c);
			// boolean vcp = Character.isValidCodePoint(c);
			// boolean scp = Character.isSupplementaryCodePoint(c);
			if (!Character.isLetter(c)) {
				// // ignore non-letters, e.g. umlauts
				// Unfortunately this also swallows non-standard punctuation
				continue;
			}
			switch (onUnrecognisableChar) {
			case DIE:
			case THROW_EXCEPTION:
				throw new FailureException(unicode);
			case IGNORE:
				continue;
			case ACCEPT:
				// filter out any bogus 0 chars (rarely needed, but a useful
				// safety measure)
				clean.append(c == 0 ? ' ' : c);
				break;
			case RETURN_NULL:
				clean.append('?');
				break;
			case REPORT:
				Log.report("Could not normalise to ascii: " + unicode);
				// ignore
			}
		}
		// if ((onUnrecognisableChar == KErrorPolicy.ACCEPT ||
		// onUnrecognisableChar == KErrorPolicy.RETURN_NULL)
		// && clean.length() < unicode.length() / 2) {
		// throw new IllegalArgumentException(unicode +" to "+clean);
		// }
		return clean.toString();
	}

	/**
	 * Delete a number of chars from the end of a {@link StringBuilder}.
	 * 
	 * @param sb
	 * @param chars
	 */
	public static void pop(StringBuilder sb, int chars) {
		sb.delete(sb.length() - chars, sb.length());
	}

	/**
	 * Delete a String from the end of a {@link StringBuilder} -- if it matches
	 * the end. E.g. pop("a+b+", "+") => "a+b" But pop("a", "+") => "a"
	 * 
	 * @param sb
	 * @param remove
	 */
	public static void pop(StringBuilder sb, String remove) {
		if (endsWith(sb, remove)) {
			sb.delete(sb.length() - remove.length(), sb.length());
		}
	}

	public static boolean endsWith(StringBuilder sb, String end) {
		int i = sb.indexOf(end, sb.length() - end.length());
		return i != -1;
	}

	/**
	 * Delete a substring from the end -- if it matches the end. 
	 * E.g. pop("a+b+", "+") => "a+b" But pop("a", "+") => "a"
	 * 
	 * @param string
	 * @param remove
	 */
	public static String pop(String string, String remove) {
		if (string.endsWith(remove)) {
			return string.substring(0, string.length()-remove.length());
		}
		return string;
	}

	/**
	 * Replace regex with null, collecting the cuttings.
	 * 
	 * @param string
	 * @param regex
	 * @param removed
	 *            The cut bits of text will be added to this
	 * @return string after a s/regex/""/ op
	 */
	public static String remove(String string, String regex,
			final Collection removed) {
		String s2 = replace(string, Pattern.compile(regex), new IReplace() {
			@Override
			public void appendReplacementTo(StringBuilder sb, Matcher match) {
				removed.add(match.group());
				return;
			}
		});
		return s2;
	}

	/**
	 * Repeat a character
	 * 
	 * @param c
	 * @param n
	 * @return e.g. '-',5 creates "-----"
	 */
	public static String repeat(char c, int n) {
		char[] chars = new char[n];
		Arrays.fill(chars, c);
		return new String(chars);
	}

	/**
	 * Like a monkey with a miniature symbol. The joy of repetition really is in
	 * you. ?? Does this deserve to be a utility method?
	 * 
	 * @param string
	 * @param n
	 * @return stringstringstring...
	 */
	public static String repeat(String string, int n) {
		StringBuilder sb = new StringBuilder(string.length() * n);
		for (int i = 0; i < n; i++) {
			sb.append(string);
		}
		return sb.toString();
	}

	/**
	 * Use a regex, calling out to a function to compute the replacements.
	 * 
	 * @param string
	 * @param regex
	 * @param replace
	 *            Determines what replacements to make
	 * @return string after all matches of regex have been replaced.
	 */
	public static String replace(String string, Pattern regex, IReplace replace) {
		Matcher m = regex.matcher(string);
		StringBuilder sb = new StringBuilder(string.length() + 16);
		int pos = 0;
		while (m.find()) {
			sb.append(string.substring(pos, m.start()));
			replace.appendReplacementTo(sb, m);
			pos = m.end();
		}
		sb.append(string.substring(pos, string.length()));
		return sb.toString();
	}

	/**
	 * Get a StringBuilder from what might be a String (avoid a copy if we can)
	 * 
	 * @param charSeq
	 * @return a string builder, which will be the input if it already is one.
	 */
	public static StringBuilder sb(CharSequence charSeq) {
		return charSeq instanceof StringBuilder ? (StringBuilder) charSeq
				: new StringBuilder(charSeq);
	}

	/**
	 * A slightly smarter version of split on whitespace, or at least,
	 * different. Splits on whitespace or commas, and supports quoting. To quote
	 * a quote double it up. E.g. tag1, "tag 2" tag-3 E.g. foo,
	 * """Hello world,"" she said.", bar
	 * 
	 * @param line
	 *            Can be null (returns empty list)
	 * @return May be empty if the input is blank
	 * 
	 * @see
	 * @testedby {@link StrUtilsTest#split()}
	 */
	public static List<String> split(String line) {
		if (line == null || line.length() == 0)
			return Collections.emptyList();
		ArrayList<String> row = new ArrayList<String>();
		StringBuilder field = new StringBuilder();
		char quote = '"';
		boolean inQuotes = false;
		for (int i = 0, n = line.length(); i < n; i++) {
			char c = line.charAt(i);
			if (c == quote) {
				// A double quote in a quote is a quote
				if (i < n - 1 && line.charAt(i + 1) == quote) {
					field.append(c);
					i = i + 1;
					continue;
				}
				inQuotes = !inQuotes;
				continue;
			}
			if (inQuotes) {
				// just add it
				field.append(c);
				continue;
			}
			if (Character.isWhitespace(c) || c == ',') {
				if (field.length() == 0) {
					continue;
				}
				// Finished a tag
				row.add(field.toString());
				field = new StringBuilder();
				continue;
			}
			// just add it
			field.append(c);
		}

		// FIXME: If we're still in quotes here, it's really a syntax error
		// if (inQuotes) doSomethingSensibleButWhat();

		// Add last field
		if (field.length() == 0)
			return row;
		String f = field.toString();
		row.add(f);

		return row;
	}

	/**
	 * Cheap version of {@link #split(String)} without whitespace Splitting.
	 * Does support quotes -- and the returned Strings will include the "quote" marks.
	 * 
	 * @param line
	 * @return
	 * @see #splitCSVStyle(String) which is probably better, but swallows "quote" marks.
	 */
	public static List<String> splitOnComma(String line) {
		if (line == null || line.length() == 0)
			return Collections.emptyList();
		ArrayList<String> row = new ArrayList<String>();
		StringBuilder field = new StringBuilder();
		char quote = '"';
		boolean inQuotes = false;
		for (int i = 0, n = line.length(); i < n; i++) {
			char c = line.charAt(i);
			if (c == quote) {	
				field.append(c);
				inQuotes = !inQuotes;
				continue;
			}
			if (inQuotes) {
				// just add it
				field.append(c);
				continue;
			}
			if (c == ',') {
				if (field.length() == 0) {
					continue;
				}
				// Finished a tag
				row.add(field.toString());
				field = new StringBuilder();
				continue;
			}
			// just add it
			field.append(c);
		}
		// Add last field
		if (field.length() == 0)
			return row;
		String f = field.toString();
		row.add(f.trim());
		return row;
	}

	/**
	 * Split into paragraph blocks by looking for an empty line.
	 * 
	 * @param message
	 * @return
	 * @testedby {@link StrUtilsTest#testSplitBlocks()}
	 */
	public static String[] splitBlocks(String message) {
		return message.split("\\s*\r?\n\\s*\r?\n"); // TODO a better regex
	}

	/**
	 * Split a string in half at the first instance of c. E.g.
	 * splitFirst("A:B:C",':') == ("A","B:C")
	 * 
	 * @param line Can be null
	 * @param c
	 * @return the line upto c, and after it. Neither part includes c. Or null
	 *         if c could not be found.
	 */
	public static Pair<String> splitFirst(String line, char c) {
		if (line==null) return null;
		int i = line.indexOf(c);
		if (i == -1)
			return null;
		String end = i == line.length() ? "" : line.substring(i + 1);
		return new Pair<String>(line.substring(0, i), end);
	}

	/**
	 * Split a string in half at the last instance of c. E.g.
	 * splitFirst("A:B:C",':') == ("A:B","C")
	 * 
	 * @param line
	 * @param c
	 * @return the line upto the last occurance of c, and after it. Neither part
	 *         includes c. Or null if c could not be found.
	 */
	public static Pair<String> splitLast(String line, char c) {
		int i = line.lastIndexOf(c);
		if (i == -1)
			return null;
		String end = i == line.length() ? "" : line.substring(i + 1);
		return new Pair<String>(line.substring(0, i), end);
	}

	/**
	 * @param txt
	 * @return txt split into lines. The String values do not include line
	 *         endings. This is just a convenience for a regex with
	 *         cross-platform line-endings. Note that trailing empty lines will
	 *         be discarded.
	 */
	public static String[] splitLines(CharSequence txt) {
		return LINEENDINGS.split(txt);
	}

	/**
	 * A more flexible (and dangerous) version of substring.
	 * 
	 * @param string
	 *            Can be null, in which case null will be returned
	 * @param start
	 *            Inclusive. Can be negative for distance from the end.
	 * @param end
	 *            Exclusive. Can be negative for distance from the end. E.g. -1
	 *            indicates "all but the last character" (zero indicates
	 *            "up to the end"). Can be longer than the actual string, in
	 *            which case it is reduced. If end is negative and too large, an
	 *            empty string will be returned.
	 * @return The chopped string. null if the input was null. The empty string
	 *         if the range was invalid.
	 */
	public static String substring(String string, int _start, int _end) {
		if (string == null)
			return null;
		// keep the original values around for debugging
		int start = _start;
		int end = _end;
		int len = string.length();
		// start from end?
		if (start < 0) {
			start = len + start;
			if (start < 0) {
				start = 0;
			}
		}
		// from end?
		if (end <= 0) {
			end = len + end;
			if (end < start) {
				return "";
			}
		}
		assert end >= 0 : start + " " + end;
		// too long?
		if (end > len) {
			end = len;
		}
		// OK
		if (start == 0 && end == len)
			return string;
		if (end < start) {
			WeirdException we = new WeirdException("Bogus start(" + _start
					+ ")/end(" + _end + " for " + string);
			Log.w("weird", we);
			return "";
		}
		assert start >= 0 && end >= 0 : start + " " + end;
		return string.substring(start, end);
	}

	/**
	 * Perform common "canonicalisation" operations. Often strings are
	 * equivalent if they only differ in case, punctuation, or spacing.
	 * 
	 * @param string
	 *            Can be null (returns "")
	 * @return compact "canonical" version of string: lowercased, compact
	 *         whitespace & trim, normalised (no accents if recognised), and all punctuation
	 *         is converted into spaces. Never null.
	 * 
	 *         TODO should this go further & strip all " ", "-" and "_" chars??
	 * @testedby {@link StrUtilsTest#testToCanonical()}
	 */
	public static String toCanonical(String string) {
		if (string == null)
			return "";
		StringBuilder sb = new StringBuilder();
		boolean spaced = false;
		for (int i = 0, n = string.length(); i < n; i++) {
			char c = string.charAt(i);
			// lowercase letters
			if (Character.isLetterOrDigit(c)) {
				spaced = false;
				// Note: javadoc recommends String.toLowerCase() as being better
				// -- I wonder if it actually is, or if this is aspirational
				// internationalisation?
				c = Character.toLowerCase(c);
				sb.append(c);
				continue;
			}
			// Treat all else as spaces (note - this includes wing-dings)
			// compact whitespace
			// if (Character.isWhitespace(c)) {
			if (spaced || sb.length() == 0) {
				continue;
			}
			sb.append(' ');
			spaced = true;
			// }
			// ignore punctuation!
		}
		if (spaced) {
			pop(sb, 1);
		}
		string = sb.toString();
		// ditch the accents, if we can
		return normalise(string, KErrorPolicy.ACCEPT);
	}

	/**
	 * Convert all line-endings to \n. Convert all blank lines to being empty
	 * lines.
	 * 
	 * @param text
	 * @return
	 */
	public static String toCleanLinux(String text) {
		text = text.replace("\r\n", "\n");
		text = text.replace('\r', '\n');
		text = BLANK_LINE.matcher(text).replaceAll("");
		return text;
	}

	/**
	 * 
	 * @param "Hello world" Can be lower-case, e.g. "hello world"
	 * @return "HW" Always upper-case
	 */
	public static String toInitials(String name) {
		StringBuilder sb = new StringBuilder();
		boolean yes = true;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isWhitespace(c)) {
				yes = true;
				continue;
			}
			if (yes) {
				c = Character.toUpperCase(c);
				sb.append(c);
			}
			yes = false;
		}
		return sb.toString();
	}

	/**
	 * @param x
	 * @param n
	 * @return
	 * @testedby {@link StrUtilsTest#testToNSigFigs()}
	 */
	public static String toNSigFigs(double x, int n) {
		assert n > 0;
		String sign = x < 0 ? "-" : "";
		double v = Math.abs(x);
		double lv = Math.floor(Math.log10(v));
		double keeper = Math.pow(10, n - 1);
		double tens = Math.pow(10, lv);
		int keepMe = (int) Math.round(v * keeper / tens);
		// avoid scientific notation for fairly small decimals
		if (lv < 0) {
			String s = toNSigFigs2_small(n, sign, lv, keepMe);
			if (s != null)
				return s;
		}
		double vt = keepMe * tens / keeper;
		String num = Printer.toStringNumber(vt);
		return sign + num;
	}

	private static String toNSigFigs2_small(int n, String sign, double lv,
			int keepMe) {
		// use scientific notation for very small
		if (lv < -8)
			return null;
		StringBuilder sb = new StringBuilder(sign);
		int zs = (int) -lv;
		String sKeepMe = Integer.toString(keepMe);
		if (sKeepMe.length() > n) {
			assert sKeepMe.charAt(sKeepMe.length() - 1) == '0';
			// we've rounded up from 9 to 10, so lose a decimal place
			zs--;
			sKeepMe = sKeepMe.substring(0, sKeepMe.length() - 1);
			if (zs == 0)
				return null;
		}
		sb.append("0.");
		for (int i = 1; i < zs; i++) {
			sb.append('0');
		}
		sb.append(sKeepMe);
		return sb.toString();
	}

	/**
	 * Converts words and multiple words to lowercase and Uppercase on first
	 * letter only E.g. daniel to Daniel, the monkeys to The Monkeys, BOB to
	 * Bob. Allows apostrophes to be in words. Handles multiple words.
	 * 
	 * @testedby {@link StrUtilsTest#testToTitleCase1()}
	 */
	public static String toTitleCase(String title) {
		if (title.length() < 2)
			return title.toUpperCase();
		StringBuilder sb = new StringBuilder(title.length());
		boolean goUp = true;
		for (int i = 0, n = title.length(); i < n; i++) {
			char c = title.charAt(i);
			if (Character.isLetterOrDigit(c) || c == '\'') {
				if (goUp) {
					sb.append(Character.toUpperCase(c));
					goUp = false;
				} else {
					sb.append(Character.toLowerCase(c));
				}
			} else {
				sb.append(c);
				goUp = true;
			}
		}
		return sb.toString();
	}

	/**
	 * Handles multiple words, camel-case (*if* the first letter is lower-case),
	 * and _. White space is not preserved.
	 * 
	 * @param wouldBeTitle
	 * @return E.g. "Spoon Mc Guffin" from "spoonMcGuffin", or "Spoon Mcguffin"
	 *         from "spoon mcguffin" Hm... could move to NLP where it could also
	 *         know about stop words
	 * 
	 * @testedby {@link StrUtilsTest#testToTitleCasePlus}
	 */
	public static String toTitleCasePlus(String wouldBeTitle) {
		String[] words = wouldBeTitle.split("(_|\\s+)");
		StringBuilder sb = new StringBuilder();
		for (String word : words) {
			if (word.length() == 0) {
				continue;
			}
			// camelCase - is only if starts lower case to avoid mangling
			// McSwinney type names.
			if (Character.isUpperCase(word.charAt(0))) {
				sb.append(word);
				sb.append(' ');
				continue;
			}
			word = replace(word, Pattern.compile("[A-Z]?[^A-Z]+"),
					new IReplace() {
				@Override
				public void appendReplacementTo(StringBuilder sb2,
						Matcher match) {
					String w = match.group();
					w = toTitleCase(w);
					sb2.append(w);
					sb2.append(' ');
				}
			});
			sb.append(word);
		}
		if (sb.length() != 0) {
			pop(sb, 1);
		}
		return sb.toString();
	}

	/**
	Trim out whitespace and punctuation from the beginning and end of
	 * the string.
	 * 
	 * @param string
	 * @return
	 */
	public static String trimPunctuation(String string) {
		Pattern puncwrapper = Pattern.compile("^\\p{Punct}*(.*?)\\p{Punct}*$");
		Matcher m = puncwrapper.matcher(string);
		if ( ! m.find()) {
			return string.trim();
		}
		String trimmed = m.group(1).trim();
		return trimmed;
	}

	/**
	 * Trims a wrapping pair of ''s or ""s. ??does nothing with whistepsace or
	 * if the string has just a leading/trailing '/"
	 * 
	 * @param string
	 * @return string, or string less the wrapping quotes
	 */
	public static String trimQuotes(String string) {
		if (string.charAt(0) != '\'' && string.charAt(0) != '\"')
			return string;
		char c = string.charAt(string.length() - 1);
		if (c != '\'' && c != '\"')
			return string;
		return string.substring(1, string.length() - 1);
	}

	public static final String GTBC_BAD_FORMAT = "%BADLYFORMATTED%";

	/**
	 * Gets "bob" and "alan" out of <"bob"~10 AND "alan"> IncludeChars - include
	 * the delimiting characted: 0: Don't 1: Do 2: Only do so when
	 * 
	 * @return
	 */
	public static List<String> getTermsBetweenChars(String query, char c,
			int includeChars) {

		// Map out locations of "'s
		List<String> output = new ArrayList();
		String modquery = query;
		int counter = 0;
		List<Integer> positions = new ArrayList<Integer>();
		while (modquery.indexOf("\"") != -1) {
			int temposition = modquery.indexOf("\"");
			positions.add(counter + temposition);
			counter = counter + temposition + 1;
			modquery = modquery.substring(temposition + 1);
		}
		if ((positions.size() % 2) != 0) {
			// bad formatting!
			output.add(GTBC_BAD_FORMAT);
			return output;
		}
		for (int x = 0; x < (positions.size()); x = x + 2) {
			String term = query.substring(positions.get(x) + 1,
					positions.get(x + 1));
			term = term.trim();
			if (includeChars == 0 || (includeChars == 2 && !term.contains(" "))) {
				output.add(term);
			} else {
				output.add("\"" + term + "\"");
			}

		}
		return output;
	}

	/**
	 * Simple whitespace based word counter.
	 * 
	 * @param text
	 * @return
	 */
	public static int wordCount(String text) {
		return text.split("\\s+").length;
	}

	/**
	 * This is a static class, but for those users who need an object...
	 */
	public StrUtils() {
	}

	/**
	 * Matches blocks of punctuation. 
	 * 's on the outside of words are matched, but 's inside words (e.g. don't) are not.
	 */
	// ??maybe switch to \\p{Punct}
	public static final Pattern PUNCTUATION = Pattern.compile("([^\\p{L}']|(^|\\s)'|'(\\s|$)|\\)|\\()+");

	/**
	 * [a-zA-Z]+ Handy sometimes as a safety filter.
	 */
	public static final Pattern AZ = Pattern.compile("[a-zA-Z]+");


	/**
	 * toString() as a function. Can handle nulls
	 */
	public static final IFn<Object, String> STR = new StrFn();


	/**
	 * Remove punctuation -- with the exception of 's inside words and urls / domain-names.
	 * @param string E.g. '(don't!)'
	 * @return e.g. don't
	 */
	public static String removePunctuation(String string) {
		// Find and protect the urls
		Matcher m = WebUtils.URL_WEB_DOMAIN_REGEX.matcher(string);				
		final ArrayList<IntRange> startEnd = new ArrayList(); 
		while(m.find()) {
			startEnd.add(new IntRange(m.start(), m.end()));
		}
		// no urls? Keep it simple
		if (startEnd.isEmpty()) {
			return StrUtils.compactWhitespace(PUNCTUATION.matcher(string).replaceAll(" "));
		}
		// Protect the urls
		String rp = replace(string, PUNCTUATION, new IReplace() {			
			@Override
			public void appendReplacementTo(StringBuilder sb, Matcher match) {
				// Domain name or url??				
				for (IntRange se : startEnd) {
					if (se.contains(match.start()) && se.contains(match.end())) {
						sb.append(match.group());
						return;
					}
				}
				sb.append(" ");
			}
		});
		return StrUtils.compactWhitespace(rp);
	}

	/**
	 * Produce a nice ASCII-art tabulation of some data
	 * 
	 * @param headers
	 * @param data
	 * @return
	 */
	public static String tabulate(List<List<? extends Object>> table) {
		// TODO: As params?
		int width = 80;
		String bar = " | ";
		String rule = "-";
		String newline = "\n";

		assert table.size() > 0;
		StringBuffer sb = new StringBuffer();
		List headers = table.get(0);
		// TODO: Plan the layout!
		int fairSize = (width - (bar.length() * (headers.size() - 1)))
				/ headers.size();
		int[] colSize = new int[table.get(0).size()];

		for (int row = 0; row < table.size(); row++) {
			for (int col = 0; col < headers.size(); col++) {
				colSize[col] = Math.max(colSize[col], table.get(row).get(col)
						.toString().length());
			}
		}
		int total = 0, spare = 0;
		for (int col = 0; col < headers.size(); col++) {
			total += colSize[col];
			if (colSize[col] < fairSize)
				spare += (fairSize - colSize[col]);
		}
		if (total > width) {
			fairSize = fairSize + (spare) / headers.size();
			for (int col = 0; col < headers.size(); col++) {
				colSize[col] = Math.min(colSize[col], fairSize);
			}
		}

		// Draw the table
		for (int row = 0; row < table.size(); row++) {
			for (int col = 0; col < headers.size(); col++) {
				if (col > 0)
					sb.append(bar);
				sb.append(padRight(table.get(row).get(col).toString(),
						colSize[col]));
			}
			sb.append(newline);
			// Draw rule
			if (row > 0)
				continue;
			for (int col = 0; col < headers.size(); col++) {
				for (int i = 0; i < colSize[col]
						+ ((col == 0) ? 0 : bar.length()); i++) {
					sb.append(rule);
				}
			}
			sb.append(newline);
		}

		return sb.toString();
	}

	/**
	 * Pad a string with spaces on the right to yield a string of the desired
	 * length. String will be truncated if it is too long.
	 */
	public static String padRight(String s, int n) {
		if (s.length() > n)
			return s.substring(0, n);
		return String.format("%1$-" + n + "s", s);
	}

	/**
	 * Pad a string with spaces on the left to yield a string of the desired
	 * length. String will be truncated if it is too long.
	 */
	public static String padLeft(String s, int n) {
		if (s.length() > n)
			return s.substring(0, n);
		return String.format("%1$" + n + "s", s);
	}


	/**
	 * Splits "alan,bob,charlie" into list of 3 Strings
	 * @param input
	 * @param c
	 * @return
	 */
	public static List<String> getSplitStringList(String input,char c) {
		if (input.indexOf(c) == -1) return Collections.singletonList(input);
		String [] strArray = input.split(""+c);
		List<String> strList = new ArrayList<String>();
		for (String s : strArray){
			strList.add(s);
		}
		return strList;
	}

	/**
	 * Simple little extension to string which counts how many times the substring happens in the string
	 * @param mainString
	 * @param subString
	 * @return number of instances
	 */
	public static int countSubStrings(String mainString, String subString){
		if (mainString.length() == 0|| subString.length() == 0){
			throw new IllegalArgumentException("Can't have blank strings here");
		}
		int output = 0;
		while(mainString.contains(subString)){
			mainString = mainString.substring(mainString.indexOf(subString) + 1);
			output++;
		}
		return output;
	}

	/**
	 * Easy join which handles null/empty objects by skipping them.
	 * @param objects If not Strings, converted using Printer.toString()
	 * @return e.g. object1 sep object2. Never null, can be "".
	 */
	public static String joinWithSkip(String sep, Object... objects) {
		if (objects.length == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		for (Object o : objects) {
			if (o==null) continue;
			String string = Printer.toString(o);
			if (string == null || string.isEmpty()) {
				continue;
			}
			sb.append(string);
			sb.append(sep);
		}
		if (sb.length() != 0) {
			pop(sb, sep.length());
		}
		return sb.toString();
	}

	/**
	 * @param small
	 * @param large
	 * @return true if all of small is in large. Always true for small=[]
	 */
	public static boolean isSubset(String[] small, String[] large) {
		for(String s : small) {
			if ( ! Containers.contains(s, large)) return false;
		}
		return true;
	}

	/**
	 * @param txt
	 * @return true if txt is just punctuation characters
	 */
	public static boolean isPunctuation(String txt) {
		return PUNCTUATION.matcher(txt).matches();
	}

	/**
	 * Convenience for null-or-toString(). You may wish to use via <br><code>import static com.winterwell.utils.StrUtils.str;</code>
	 * 
	 * @param whatever
	 * @return whatever.toString() or null
	 */
	public static String str(final Object whatever) {
		return whatever==null? null : whatever.toString();
	}

	/**
	 * @param plain
	 * @param special
	 * @return plain, where any use of special or escape has been escaped.
	 */
	public static String escape(String plain, char special, char escape) {
		StringBuilder sb = new StringBuilder(plain.length());
		for(int i=0,n=plain.length(); i<n; i++) {
			char c = plain.charAt(i);
			if (c==special || c==escape) sb.append(escape);
			sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * @param plain
	 * @param special
	 * @return plain, where any use of special or escape has been escaped.
	 */
	public static String escape(String plain, String special, char escape) {
		StringBuilder sb = new StringBuilder(plain.length());
		for(int i=0,n=plain.length(); i<n; i++) {
			char c = plain.charAt(i);
			if (special.indexOf(c)!=-1 || c==escape) sb.append(escape);
			sb.append(c);
		}
		return sb.toString();
	}


	/**
	 * Opposite of {@link #escape(String, char, char)}
	 * @param escaped
	 * @param escape
	 * @return
	 */
	public static String unescape(String escaped, char escape) {
		StringBuilder sb = new StringBuilder(escaped.length());
		for(int i=0,n=escaped.length(); i<n; i++) {
			char c = escaped.charAt(i);
			if (c==escape) {
				i++;
				c = escaped.charAt(i);	
			}
			sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Treat txt as a csv row(s), and extract the strings. 
	 * That is, split on comma, and support quoting.
	 * Adds in a trim() on each item.
	 * @param txt
	 * @return bits of txt
	 * @see #split(String)
	 * @see #splitOnComma(String)
	 * @see CSVReader#split(String)
	 */
	public static List<String> splitCSVStyle(String txt) {
		CSVReader r = new CSVReader(new StringReader(txt), ',', '"', (char)0);
		r.setCommentMarker((char)0);
		List<String> strings = new ArrayList();
		for (String[] row : r) {
			for (String bit : row) {
				strings.add(bit.trim());
			}
		}
		r.close();
		return strings;
	}


	public static List<String> findAll(Pattern regex, String text) {
		ArrayList list = new ArrayList();
		if (text==null) return list;
		Matcher m = regex.matcher(text);
		while(m.find()) {
			list.add(m.group());
		}
		return list;
	}

}

/**
 * toString() as a function. Can handle nulls (returns "null")
 * @author daniel
 */
final class StrFn implements IFn<Object,String> {
	@Override
	public String apply(Object value) {
		return String.valueOf(value);
	}	
}
