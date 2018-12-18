package com.winterwell.maths.stats.distributions.cond;

import java.util.Map;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.matrix.ObjectMatrix;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;

/**
 * Use with SitnStream and CommonFeatureFactory
 * @testedby {@link WordMarkovChainTest}
 * 
 * @param <Token> typically ww.nlp's Tkn class, which wraps a String word.
 * @author daniel
 * 
 */
public class WordMarkovChain<Token> extends ACondDistribution<Token, Cntxt>
	implements 
	ITrainable.Supervised<Cntxt,Token>, 	
	ICondDistribution.WithExplanation<Token, Cntxt>
	{

	ObjectMatrix<Cntxt, Token> m = new ObjectMatrix<>();
	
	@Override
	public void train1(Cntxt prev, Token word, double weight) {
		m.plus(prev, word, weight);
	}	
	
	public int getNumStates() {
		return m.size();
	}
	
	@Override
	public ObjectDistribution<Token> getMarginal(Cntxt context) {
		Map<Token, Double> col = m.getRow(context);
		ObjectDistribution<Token> od = new ObjectDistribution<>(col);
		od.setPseudoCount(pseudoCount);
		od.normalise(); // ?? maybe optimise by normalising the whole matrix at finishTraining??
		return od;
	}
	
	@Override
	public double probWithExplanation(Token outcome, Cntxt context, ExplnOfDist explain) {
		ObjectDistribution<Token> od = getMarginal(context);
		double p = od.prob(outcome);
		return p;
	}
	
	@Override
	public void finishTraining() {
		super.finishTraining();
	}
	
	@Override
	public void resetup() {
		super.resetup();
	}

	/**
	 * e.g. 0.000001
	 */
	double pseudoCount;
	
	public void setPseudoCount(double pseudoCount) {
		this.pseudoCount = pseudoCount;
	}
}
