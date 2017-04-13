package com.winterwell.nlp.classifier;

import org.junit.Test;

import com.winterwell.nlp.corpus.SimpleDocument;
import com.winterwell.nlp.docmodels.WordFreqDocModel;

public class WordFreqDocModelTest {

	@Test
	public void testPrep() {
		WordFreqDocModel model = new WordFreqDocModel();
		model.setUnseenWordHandling(1);
		model.train1(new SimpleDocument("cats are cute"));
		double p = model.prob(new SimpleDocument("cats are cute"));
		model.finishTraining();
		SimpleDocument cats = new SimpleDocument("cats are cute");
		double p2 = model.prob(cats);
		assert p2 == 1.0 / 27;
	}

	// @Test
	// public void testFactory() {
	// WordFreqDocModel model = new WordFreqDocModel();
	// model.setUnseenWordHandling(17);
	// Random src = new Random();
	// model.setRandomSource(src);
	//
	// WordFreqDocModel model2 = model.factory(null);
	//
	// String xml1 = WebUtils.serialiseToXml(model);
	// String xml2 = WebUtils.serialiseToXml(model2);
	//
	// assertEquals(xml1, xml2);
	// }

}
