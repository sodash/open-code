package com.winterwell.nlp.docmodels;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.maths.vector.Cuboid;
import com.winterwell.nlp.classifier.DocVectoriser;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.io.ITokenStream;

import no.uib.cipr.matrix.Vector;

/**
 * Use a vector prob model as a document model.
 * 
 * @author daniel
 * 
 */
public class VectorDocModel extends ADocModel {

	private ITrainable<Vector> model;
	private DocVectoriser vectoriser;

	/**
	 * Note: does *not* call init()
	 * 
	 * @param vectorModel
	 * @param vectoriser
	 */
	public VectorDocModel(ITrainable<Vector> vectorModel,
			DocVectoriser vectoriser) {
		super(vectoriser.getTokeniser(), vectoriser.getIndex());
		this.model = vectorModel;
		this.vectoriser = vectoriser;
		assert vectoriser instanceof IDistribution;
	}

	@Override
	public void finishTraining() {
		model.finishTraining();
	}

	@Override
	public boolean isReady() {
		return model.isReady();
	}

	@Override
	public double prob(IDocument x) {
		Cuboid vec = vectoriser.toCuboid(x);
		double prob = ((IDistribution) model).prob(vec.first, vec.second);
		return prob;
	}

	@Override
	public void pruneToIndex() {
		// ??
	}

	@Override
	public void resetup() {
		model.resetup();
		super.resetup();
	}

	@Override
	public void setTokenizer(ITokenStream tokenizer) {
		super.setTokenizer(tokenizer);
		vectoriser.setTokeniser(tokenizer);
	}

	@Override
	public void train1(IDocument x) throws UnsupportedOperationException {
		Vector vec = vectoriser.toVector(x);
		((Unsupervised<Vector>) model).train1(vec);
	}

}
