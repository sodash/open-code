package com.winterwell.nlp.vectornlp;

import static org.junit.Assert.*;

import java.util.Set;

import no.uib.cipr.matrix.Vector;

import org.junit.Test;

import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.nlp.vectornlp.GloveWordVectors.KGloveSource;

import com.winterwell.utils.containers.TopNList;

public class GloveWordVectorsTest {

	@Test
	public void testGloveWordVectors() {
		GloveWordVectors gwv = new GloveWordVectors();
		gwv.init();
		System.err.println(KGloveSource.Wikipedia);
		output(gwv);
	}
	
	@Test
	public void testGloveTwitterWordVectors() {
		GloveWordVectors gwv = new GloveWordVectors(KGloveSource.Twitter);
		gwv.init();
		System.err.println(KGloveSource.Twitter);
		output(gwv);
	}
	
	@Test
	public void testGloveCommonCrawlWordVectors() {
		GloveWordVectors gwv = new GloveWordVectors(KGloveSource.CommonCrawl);
		gwv.init();
		System.err.println(KGloveSource.CommonCrawl);
		output(gwv);
	}


	private void output(GloveWordVectors gwv) {
		Set<String> stop = NLPWorkshop.get().getStopwords();
		for(String word : new String[]{"frog", "love"}) {
			Vector frog = gwv.getVector(word);
			TopNList<String> best = new TopNList<>(10);
			for(String w : gwv.word2vec.keySet()) {
				if (stop.contains(w)) {
					continue;
				}
				Vector v = gwv.getVector(w);
				if (v==null) {
					// weird! 
					continue;
				}
				double score = frog.dot(v);
				best.maybeAdd(w, score);
			}
			System.out.println(word+" ->"+best);
		}
	}

}
