package com.winterwell.nlp.io.pos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.hmm.FlexiHMM;
import com.winterwell.maths.stats.distributions.ADistributionBase;
import com.winterwell.maths.stats.distributions.IDistributionBase;
import com.winterwell.maths.stats.distributions.cond.AFiniteCondDistribution;
import com.winterwell.maths.stats.distributions.cond.FrequencyCondDistribution;
import com.winterwell.maths.stats.distributions.cond.IFiniteCondDistribution;
import com.winterwell.nlp.corpus.ICorpus;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.corpus.brown.BrownCorpus;
import com.winterwell.nlp.corpus.brown.BrownDocument;
import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.nlp.io.Tkn;

/**
 * @testedby {@link POSTaggerTest}
 * @author daniel
 * 
 */
public class POSTagger extends HMMTagger<String> {

	private static FlexiHMM<Tkn, String> getPOSHMM(ICorpus corpus) {
		IFiniteCondDistribution<String, String> pTrans = new FrequencyCondDistribution<String, String>();
		IFiniteCondDistribution<Tkn, String> pEmit = new WrapperDist();
		FlexiHMM<Tkn, String> hmm = new FlexiHMM<Tkn, String>(pEmit, pTrans) {

		};
		for (IDocument doc : corpus) {
			BrownDocument bdoc = (BrownDocument) doc;
			bdoc.setSimplify(2);
			List<String> hidden = new ArrayList<String>();
			List<Tkn> observed = new ArrayList<Tkn>();
			for (Tkn token : bdoc.getTokenStream()) {
				hidden.add(token.get(Tkn.POS));
				observed.add(token);
			}
			hmm.trainSeqn(observed, hidden);
		}
		return hmm;
	}

	public POSTagger(ITokenStream base) {
		this(base, new BrownCorpus());
	}

	/**
	 * 
	 * @param base
	 * @param corpus
	 *            Must produce
	 */
	public POSTagger(ITokenStream base, ICorpus corpus) {
		super(base, Tkn.POS, getPOSHMM(corpus));
	}

}

class WrapperDist extends AFiniteCondDistribution<Tkn, String> implements
		ITrainable.CondUnsupervised<String, Tkn> {

	FrequencyCondDistribution<String, String> pEmitString = new FrequencyCondDistribution<String, String>();

	public WrapperDist() {
	}

	@Override
	public void finishTraining() {
		pEmitString.finishTraining();
	}

	@Override
	public IDistributionBase<Tkn> getMarginal(String context) {
		final IDistributionBase<String> m = pEmitString.getMarginal(context);
		return new ADistributionBase<Tkn>() {
			@Override
			public Tkn sample() {
				String w = m.sample();
				return new Tkn(w);
			}
		};
	}

	@Override
	public Collection<String> getPossibleCauses(Tkn outcome) {
		return pEmitString.getPossibleCauses(outcome.getText());
	}

	@Override
	public boolean isReady() {
		return pEmitString.isReady();
	}

	@Override
	public double prob(Tkn outcome, String context) {
		String w = outcome.getText();
		return pEmitString.prob(w, context);
	}

	@Override
	public void resetup() {
		pEmitString.resetup();
	}

	@Override
	public void train1(String x, Tkn tag, double weight) {
		pEmitString.train1(x, tag.getText(), weight);
	}

}
