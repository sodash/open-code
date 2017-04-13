package com.winterwell.nlp.thesaurus;

import org.junit.Test;

public class BigHugeThesaurusClientTest {

	@Test
	public void testGet() {
		BigHugeThesaurusClient tc = new BigHugeThesaurusClient();
		Object foo = tc.get("sitting");
		System.out.println(foo);
	}

}
