package com.winterwell.web.fields;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ListFieldTest {

	@Test
	public void testProblemChars() throws Exception {
		ListField<String> lf = new ListField<String>("test", null);
		List<String> list = Arrays.asList("ab", "a b", "\"a b\"", "a & b",
				"a\nb", "a,\tb");
		String s = lf.toString(list);
		List<String> list2 = lf.fromString(s);
		assertEquals(list, list2);
	}
}
