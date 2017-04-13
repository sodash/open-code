package com.winterwell.nlp.dict;

import static org.junit.Assert.*;

import org.junit.Test;

public class RhymeDictionaryTest {

	@Test
	public void testGetMeaning() {
		RhymeDictionary rd = new RhymeDictionary();
		String r1 = rd.getMeaning("Bee");
		String r2 = rd.getMeaning("Free");
		String r3 = rd.getMeaning("Bear");
		assert r1.equals(r2);
		assert ! r1.equals(r3);
	}

}
