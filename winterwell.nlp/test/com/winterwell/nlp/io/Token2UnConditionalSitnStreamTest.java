package com.winterwell.nlp.io;

import java.util.List;

import org.junit.Test;

import com.winterwell.maths.stats.distributions.cond.ISitnStream;
import com.winterwell.maths.stats.distributions.cond.Sitn;
import com.winterwell.nlp.corpus.SimpleDocument;
import com.winterwell.utils.containers.Containers;

public class Token2UnConditionalSitnStreamTest {

	@Test
	public void testSimple() {
		WordAndPunctuationTokeniser tok = new WordAndPunctuationTokeniser();
		tok.setLowerCase(true);
		ISitnStream<Tkn> tokeniser = new Token2UnConditionalSitnStream(tok);
		SimpleDocument doc1 = new SimpleDocument("cat being cute");
		ISitnStream<Tkn> tokens = tokeniser.factory(doc1);
		List<Sitn<Tkn>> list = Containers.getList(tokens);
		System.out.println(list);
	}

}
