package com.winterwell.maths.stats.distributions.cond;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.corpus.SimpleDocument;
import com.winterwell.nlp.corpus.brown.BrownCorpus;
import com.winterwell.nlp.io.SitnStream;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Containers;

public class WordMarkovChainTest {

	@Test
	public void testTrain1SimpleDoc() {
		WordMarkovChain<Tkn> wmc = new WordMarkovChain();
		wmc.setPseudoCount(0);
		
		// create with standard whitespace tokeniser, and one-word memory
		WordAndPunctuationTokeniser tknr = new WordAndPunctuationTokeniser();		
		SitnStream ss = new SitnStream(null, tknr, new String[]{"w-1"});
		
		// train on a doc
		SimpleDocument doc = new SimpleDocument("kittens are cute");
		SitnStream ss2 = ss.factory(doc);		
		for (Sitn<Tkn> sitn : ss2) {
			wmc.train1(sitn.context, sitn.outcome);
		}
		
		Cntxt cntxt = new Cntxt(ss.getContextSignature(), new Tkn("are"));
		double probFreeGivenMy = wmc.prob(new Tkn("cute"), cntxt);
		assert probFreeGivenMy > 0;

		Tkn sampled = wmc.sample(cntxt);
		assert sampled.getText().equals("cute");
	}
	

	@Test
	public void testTrain1SimpleDocPseudoCount() {
		WordMarkovChain<Tkn> wmc = new WordMarkovChain();
		wmc.setPseudoCount(0.01);
		
		// create with standard whitespace tokeniser, and one-word memory
		WordAndPunctuationTokeniser tknr = new WordAndPunctuationTokeniser();		
		SitnStream ss = new SitnStream(null, tknr, new String[]{"w-1"});
		
		// train on a doc
		SimpleDocument doc = new SimpleDocument("kittens are cute");
		SitnStream ss2 = ss.factory(doc);		
		for (Sitn<Tkn> sitn : ss2) {
			wmc.train1(sitn.context, sitn.outcome);
		}
		
		Cntxt cntxt = new Cntxt(ss.getContextSignature(), new Tkn("are"));
		double probFreeGivenMy = wmc.prob(new Tkn("cute"), cntxt);
		assert probFreeGivenMy > 0;

		Tkn sampled = wmc.sample(cntxt);
		assert new ArraySet("kittens","are","cute").contains(sampled.getText());
	}
	

	@Test
	public void testTrainBrownCorpus() {
		WordMarkovChain<Tkn> wmc = new WordMarkovChain();
		wmc.setPseudoCount(0.000001);
		
		// create with standard whitespace tokeniser, and one-word memory
		WordAndPunctuationTokeniser tknr = new WordAndPunctuationTokeniser();		
		SitnStream ss = new SitnStream(null, tknr, new String[]{"w-1"});
		
		// train on Brown
		BrownCorpus bc = new BrownCorpus();
		for (IDocument doc : bc) {
			SitnStream ss2 = ss.factory(doc);		
			for (Sitn<Tkn> sitn : ss2) {
				wmc.train1(sitn.context, sitn.outcome);
			}			
		}
		
		Cntxt cntxt = new Cntxt(ss.getContextSignature(), Tkn.START_TOKEN);
		for(int i=0; i<10; i++) {
			Tkn sampled = wmc.sample(cntxt);
			System.out.println(sampled);
			cntxt = new Cntxt(ss.getContextSignature(), sampled);
		}
		
	}
	


	@Test
	public void testTrainBrownCorpus2() {
		WordMarkovChain<Tkn> wmc = new WordMarkovChain();
		wmc.setPseudoCount(0.000001);
		
		// create with standard whitespace tokeniser, and one-word memory
		WordAndPunctuationTokeniser tknr = new WordAndPunctuationTokeniser();		
		SitnStream ss = new SitnStream(null, tknr, new String[]{"w-1", "w-2"});
		
		// train on Brown
		BrownCorpus bc = new BrownCorpus();
		for (IDocument doc : bc) {
			SitnStream ss2 = ss.factory(doc);		
			for (Sitn<Tkn> sitn : ss2) {
				wmc.train1(sitn.context, sitn.outcome);
			}			
		}
		
		Cntxt cntxt = new Cntxt(ss.getContextSignature(), Tkn.START_TOKEN, Tkn.START_TOKEN);
		for(int i=0; i<10; i++) {
			Tkn sampled = wmc.sample(cntxt);
			System.out.println(sampled);
			cntxt = new Cntxt(ss.getContextSignature(), sampled, cntxt.bits[0]);
		}
		
	}
	

}
