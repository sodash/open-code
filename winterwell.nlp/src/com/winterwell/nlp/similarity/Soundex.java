package com.winterwell.nlp.similarity;

/**
 * Compute Soundex codes.
 * 
 * @author daniel
 */
public final class Soundex
// implements ICompareWords
{

	public static String getSoundexCode(String word) {
		assert word.length() != 0;
		word = word.toLowerCase();
		StringBuilder sx = new StringBuilder(4);
		// Initial letter
		char c = word.charAt(0);
		if (!Character.isLetterOrDigit(c))
			throw new IllegalArgumentException(word + " is not a word");
		sx.insert(0, c);
		// next three
		char prev = getSoundexCode2(c);
		for (int i = 1; i < word.length(); i++) {
			// 1. Replace consonants with digits as follows (but do not change
			// the first letter):
			c = word.charAt(i);
			char digit = getSoundexCode2(c);
			// 3. Remove all non-digits after the first letter.
			if (digit == ' ') {
				continue;
			}
			// 2. Collapse adjacent identical digits into a single digit of that
			// value.
			if (digit != prev) {
				sx.append(digit);
			}
			// 4. Return the starting letter and the first three remaining
			// digits.
			if (sx.length() == 4)
				return sx.toString();
		}
		// If needed, append zeroes to make it a letter and three digits.
		for (int i = sx.length(); i < 4; i++) {
			sx.append('0');
		}
		return sx.toString();
	}

	private static char getSoundexCode2(char c) {
		switch (c) {
		case 'b':
		case 'f':
		case 'p':
		case 'v':
			return '1';
		case 'c':
		case 'g':
		case 'j':
		case 'k':
		case 'q':
		case 's':
		case 'x':
		case 'z':
			return '2';
		case 'd':
		case 't':
			return '3';
		case 'l':
			return '4';
		case 'm':
		case 'n':
			return '5';
		case 'r':
			return '6';
		case 'h':
		case 'w':
			return ' '; // TODO h and w rules, though none of the online code
						// samples seem to bother!
		case 'a':
		case 'e':
		case 'i':
		case 'o':
		case 'u':
			return ' ';
		}
		assert !Character.isLetter(c) : c;
		return ' ';
	}

	/**
	 * Crude similarity measure: 1 for same soundex, 0 otherwise
	 */
	public double similarity(String a, String b) {
		return getSoundexCode(a).equals(getSoundexCode(b)) ? 1 : 0;
	}

}
