package com.winterwell.maths.stats.distributions.cond;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.winterwell.maths.vector.X;

public class ListSitnStreamTest {

	@Test
	public void testIterator() {
		String[] sig = new String[] {"doc"};
		List<Sitn<String>> list = Arrays.asList(
				new Sitn("Dear", new Cntxt(sig, "letter")),
				new Sitn("Alice", new Cntxt(sig, "letter"))
				);
		ListSitnStream<String> lss = new ListSitnStream<>(list);
		
		String s = "";
		for (Sitn<String> sitn : lss) {
			s += sitn.outcome;
		}
		
		assert s.equals("DearAlice") : s;
	}

	@Test
	public void testEmptyList() {
		ListSitnStream<String> lss = new ListSitnStream<>(Arrays.asList());
		String s = "";
		for (Sitn<String> sitn : lss) {
			s += sitn.outcome;
		}

		assertEquals(s, "");
	}

}
