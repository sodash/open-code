package com.winterwell.nlp.classifier;

import java.util.Map;

import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.nlp.corpus.SimpleDocument;
import com.winterwell.nlp.docmodels.IDocModel;
import com.winterwell.nlp.docmodels.WordFreqDocModel;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;

import junit.framework.TestCase;

/**
 * @tests {@link BayesDocModelClassifier}
 * @author daniel
 * 
 */
public class BayesDocModelClassifierTest extends TestCase {

	public void testPClassify() {
		WordFreqDocModel m1 = new WordFreqDocModel();
		m1.setUnseenWordHandling(1);
		WordFreqDocModel m2 = new WordFreqDocModel();
		m2.setUnseenWordHandling(1);
		Map<String, IDocModel> models = new ArrayMap<String, IDocModel>("cats",
				m1, "porn", m2);
		BayesDocModelClassifier<String> nb = new BayesDocModelClassifier<String>(
				models);
		nb.resetup();
		ObjectDistribution<String> prior = nb.getPrior();
		assert prior.size() == 2;
		assert !nb.getPrior().isEmpty();
		// test on the two main classes of internet content
		for (int i = 0; i < 5; i++) {
			nb.train1(new SimpleDocument("cats are so cute"), "cats");
			nb.train1(new SimpleDocument(
					"cute girls doing naughty things on camera"), "porn");
		}
		nb.finishTraining();
		{
			IFiniteDistribution<String> dist = nb.pClassify(new SimpleDocument(
					"cats"));
			Printer.out(dist);
			assert dist.getMostLikely() == "cats";
		}
		{
			IFiniteDistribution<String> dist = nb.pClassify(new SimpleDocument(
					"cute"));
			Printer.out(dist);
			// cats will narrowly win, having fewer words so valuing each a bit
			// higher
			assert dist.getMostLikely() == "cats";
			SimpleDocument girls = new SimpleDocument("cute girls");
			double ppg = m2.probWord("girls");
			double pcg = m1.probWord("girls");
			assert ppg > pcg;
			double ppc = m2.probWord("cute");
			double pcc = m1.probWord("cute");
			assert pcc > ppc;
			double catProb = m1.prob(girls);
			double pornProb = m2.prob(girls);
			assert catProb < pornProb;
			dist = nb.pClassify(girls);
			Printer.out(dist);
			assert dist.getMostLikely() == "porn";
		}
		{
			// unknown word handling
			IFiniteDistribution<String> dist = nb.pClassify(new SimpleDocument(
					"marauding elephants"));
			Printer.out(dist);
			// cats should narrowly win as having a tiny bit more weight still
			// assigned to unseen
			assert dist.getMostLikely() == "cats";
		}
	}

	/**
	 * no unseen word pseudo-counts
	 */
	public void testPClassify2() {
		WordFreqDocModel m1 = new WordFreqDocModel();
		m1.setUnseenWordHandling(0);
		WordFreqDocModel m2 = new WordFreqDocModel();
		m2.setUnseenWordHandling(0);
		Map<String, IDocModel> models = new ArrayMap<String, IDocModel>("cats",
				m1, "porn", m2);
		BayesDocModelClassifier<String> nb = new BayesDocModelClassifier<String>(
				models);
		// nb.setPrior()
		// test on the two main classes of internet content
		nb.train1(new SimpleDocument("cats are so cute"), "cats");
		nb.train1(new SimpleDocument(
				"cute girls doing naughty things on camera"), "porn");
		{
			IFiniteDistribution<String> dist = nb.pClassify(new SimpleDocument(
					"cute"));
			dist.normalise();
			Printer.out(dist);
			// cats will narrowly win, having fewer words so valuing each a bit
			// higher
			assert dist.getMostLikely() == "cats";
			double pCat = dist.prob("cats");
			double pc = 0.25 / (0.25 + 1.0 / 7);
			double pp = (1.0 / 7) / (0.25 + 1.0 / 7);
			assert MathUtils.equalish(pCat, pc) : pCat + " vs " + pc;
			dist = nb.pClassify(new SimpleDocument("cute girls"));
			Printer.out(dist);
			assert dist.getMostLikely() == "porn";
			assert dist.prob("cats") == 0;
		}
	}

}
