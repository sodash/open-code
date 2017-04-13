package com.winterwell.maths.stats.distributions;


import java.util.ArrayList;

import com.winterwell.maths.ITrainable;

/**
 * Provides methods for storing training data. Shadows {@link ITrainable} (does
 * not formally implement it 'cos not all sub-classes support training).
 * 
 * @author daniel
 * 
 * @param <X>
 *            Type of data item
 * @param <Tag>
 *            Type of training-label (if appropriate)
 */
public class ATrainableBase<X, Tag> {

	/**
	 * null unless we see some weighted training data. The order matches that of
	 * trainingData.
	 * 
	 * @see #fillInWeights()
	 */
	protected transient ArrayList<Double> dataWeights;

	protected transient boolean pleaseTrainFlag;

	/**
	 * null unless resetup() has been called (as it should be)
	 */
	protected transient ArrayList<X> trainingData;

	/**
	 * null unless we see some
	 */
	protected transient ArrayList<Tag> trainingDataLabels;

	public ATrainableBase() {
		super();
	}

	/**
	 * Weight any unweighted pre-existing data as 1. Also creates the weights
	 * list if needed.
	 */
	protected final void fillInWeights() {
		// no training data? just create an empty list of weights then
		if (trainingData == null) {
			dataWeights = new ArrayList(0);
			return;
		}
		if (dataWeights == null) {
			dataWeights = new ArrayList(trainingData.size());
		}
		if (dataWeights.size() == trainingData.size())
			return;
		assert dataWeights.size() < trainingData.size();
		while(dataWeights.size() < trainingData.size()) {
			dataWeights.add(1.0);
		}
		assert dataWeights.size() == trainingData.size();
//		dataWeights.fill(dataWeights.size(), trainingData.size(), 1);
	}

	protected void finishTraining() {
		pleaseTrainFlag = false;
		noTrainingDataCollection();
	}

	/**
	 * Is this ready to use? The default implementation relies on
	 * {@link #pleaseTrainFlag} as set by default #train1() methods.
	 * 
	 * @see ITrainable#isReady()
	 */
	public boolean isReady() {
		return !pleaseTrainFlag;
	}

	/**
	 * null out all training-data arrays 'cos actually you don't use them.
	 */
	protected void noTrainingDataCollection() {
		dataWeights = null;
		trainingData = null;
		trainingDataLabels = null;
	}

	/**
	 * Sub-classes should over-ride if they need to do something here.
	 * Default version: null out the training-data arrays 'cos actually you don't use them.
	 */
	protected void resetup() {
		noTrainingDataCollection();
	}

	protected void train(double[] weights, Iterable<? extends X> wdata) {
		// weight any unweighted pre-existing data as 1
		fillInWeights();
		// add the new weights
		for (int i = 0; i < weights.length; i++) {
			double wi = weights[i];
			assert wi >= 0;
			dataWeights.add(wi);
		}
		// add the new data
		train(wdata);
	}

	protected void train(Iterable<? extends X> data) {
		for (X x : data) {
			train1(x);
		}
	}

	protected synchronized void train1(X x) {
		pleaseTrainFlag = true;
		if (trainingData == null) {
			trainingData = new ArrayList<X>();
		}
		trainingData.add(x);
	}

	protected void train1(X x, Tag tag, double weightIgnored) {
		pleaseTrainFlag = true;
		if (trainingData == null) {
			trainingData = new ArrayList<X>();
		}
		trainingData.add(x);
		if (trainingDataLabels == null) {
			trainingDataLabels = new ArrayList<Tag>();
		}
		trainingDataLabels.add(tag);
	}

	protected void train1weighted(double weight, X data) {
		fillInWeights();
		dataWeights.add(weight);
		train1(data);
	}

}