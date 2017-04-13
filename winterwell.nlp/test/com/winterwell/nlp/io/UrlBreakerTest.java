package com.winterwell.nlp.io;

import static com.winterwell.utils.Printer.out;

import java.util.List;

import org.junit.Test;

import com.winterwell.utils.containers.Containers;

public class UrlBreakerTest {

	@Test
	public void testCornerCases() {
		{
			WordAndPunctuationTokeniser ts = new WordAndPunctuationTokeniser(
					"http://soda.sh");
			UrlBreaker ub = new UrlBreaker(ts);
			List<Tkn> tokens = Containers.getList(ub);
			out(tokens);
			assert tokens.size() == 1;
		}
		{
			WordAndPunctuationTokeniser ts = new WordAndPunctuationTokeniser(
					"foo http://sod- what");
			UrlBreaker ub = new UrlBreaker(ts);
			List<Tkn> tokens = Containers.getList(ub);
			out(tokens);
			assert tokens.size() == 3;
		}
	}
}
