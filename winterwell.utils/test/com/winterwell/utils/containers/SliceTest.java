package com.winterwell.utils.containers;

import org.junit.Test;

public class SliceTest {

	@Test
	public void testOverlap() {
		String s = "hello world";
		{
			Slice s1 = new Slice(s, 1, 4);
			Slice s2 = new Slice(s, 0, 1);
			assert s1.overlap(s2) == null;
			assert s2.overlap(s1) == null;
		}
		{ // zero length special case
			Slice s1 = new Slice(s, 1, 4);
			Slice s2 = new Slice(s, 1, 1);
			assert s1.overlap(s2).length() == 0;
			assert s2.overlap(s1).length() == 0;
		}
		{ // normal overlap
			Slice s1 = new Slice(s, 1, 4);
			Slice s2 = new Slice(s, 3, 7);
			assert s1.overlap(s2).equals(new Slice(s, 3, 4));
			assert s2.overlap(s1).equals(new Slice(s, 3, 4));
		}
		{ // contained
			Slice s1 = new Slice(s, 1, 7);
			Slice s2 = new Slice(s, 3, 5);
			assert s1.overlap(s2).equals(new Slice(s, 3, 5));
			assert s2.overlap(s1).equals(new Slice(s, 3, 5));
		}
	}
}
