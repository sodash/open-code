package com.winterwell.nlp.languages;

import java.util.Locale;

import junit.framework.Assert;

import org.junit.Test;

import com.mzsanford.cld.CompactLanguageDetector;
import com.mzsanford.cld.LanguageDetectionResult;

/**
 * Test case for the Chromium compact language detection framework.
 * 
 * @author Joe Halliwell <joe@winterwell.com>
 * @tested {@link CompactLanguageDetector}
 */
public class CldTest {

	@Test
	public void basicTest() {
		assertLocale("Hello. How do you do?", Locale.ENGLISH);
		assertLocale("Bonjour tout le monde!", Locale.FRENCH);
		assertLocale("?达?们是赢了还是输", Locale.CHINESE);
		assertLocale("Hola! No hablo espanol.", new Locale("ES"));
		assertNotLocale("Hello. I am terribly British!", Locale.CHINESE);
	}

	private Locale getProbable(String testMessage) {
		CompactLanguageDetector cld = new CompactLanguageDetector();
		LanguageDetectionResult result = cld.detect(testMessage);
		Locale best = result.getProbableLocale();
		System.out.println(testMessage + "\t\t" + best.getDisplayLanguage());
		return best;
	}

	private void assertLocale(String testMessage, Locale expectedLocale) {
		Assert.assertEquals(expectedLocale, getProbable(testMessage));
	}

	private void assertNotLocale(String testMessage, Locale disallowedLocale) {
		Assert.assertNotSame(disallowedLocale, getProbable(testMessage));
	}
}
