package com.winterwell.maths.datastorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.winterwell.utils.containers.Containers;

public class ChainedIterableTest {

	@Test
	public void testSimple() {
		List<String> a = Arrays.asList("A", "B");
		List<String> b = new ArrayList<String>();
		List<String> c = Arrays.asList("C", "D");
		List<String> d = new ArrayList<String>();
		ChainedIterable<String> ci = new ChainedIterable<String>(a, b, c, d);

		List<String> list = Containers.getList(ci);
		assert list.equals(Arrays.asList("A", "B", "C", "D")) : list;
	}
}
