/**
 * 
 */
package com.winterwell.nlp.io;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.utils.Printer;

/**
 * A Token Stream Test Utils
 * 
 * @author Joe Halliwell <joe@winterwell.com>
 * 
 */
public class ATSTestUtils {

	/**
	 * Check that a stream produces the expected words
	 * 
	 * @param stream
	 * @param words
	 */
	public static void assertMatches(ITokenStream stream, String... words) {
		int i = -1;
		List<Tkn> tokens = getTokens(stream);
		List<String> mismatches = new ArrayList<String>();
		for (Tkn token : tokens) {
			i++;
			if (i >= words.length) {
				mismatches.add(i + ": " + token + " != \"\"");
				continue;
			}
			if (token.getText().equals(words[i])) {
				continue;
			}
			mismatches.add(i + ": " + token + " != " + words[i]);
		}
		assert mismatches.isEmpty() : tokens+" led to mismatches "+mismatches;
		assert i == words.length - 1 : (i + 1) + " vs " + words.length + ": "
				+ tokens + " vs " + Printer.toString(words);
	}

	/**
	 * 
	 * @param testCase
	 * @param expected
	 *            just split on whitespace
	 */
	public static void assertTokenisation(ITokenStream testCase, String expected) {
		String[] tokens = expected.split("\\s+");
		assertMatches(testCase, tokens);
	}

	public static List<Tkn> getTokens(ITokenStream stream) {
		ArrayList<Tkn> results = new ArrayList<Tkn>();
		for (Tkn token : stream) {
			results.add(token);
		}
		return results;
	}

}
