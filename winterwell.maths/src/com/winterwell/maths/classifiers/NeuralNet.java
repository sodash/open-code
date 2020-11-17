package com.winterwell.maths.classifiers;

import java.util.Random;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.distributions.ATrainableBase;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.Utils;

import no.uib.cipr.matrix.Vector;

/**
 * Status: Experimental! Buggy!
 * 
 * TODO more testing TODO
 * 
 * Multilayer feed forward neural net, trained by back-propagation.
 * 
 * @author daniel
 * @testedby  NeuralNetTest}
 */
public class NeuralNet extends ATrainableBase<Vector, Vector> implements
		ITrainable.Supervised<Vector, Vector> {

	private static final int EPOCHS = 100;

	/**
	 * 0 = input last = output
	 */
	NeuralNetLayer[] layers;

	private transient Random random;

	/**
	 * Create a simplish 2-layer neural net
	 * 
	 * @param dimIn
	 * @param dimOut
	 */
	public NeuralNet(int dimIn, int dimOut) {
		layers = new NeuralNetLayer[2];
		int nodes = (dimIn + dimOut) / 2;
		layers[0] = new NeuralNetLayer(dimIn, nodes);
		layers[1] = new NeuralNetLayer(nodes, dimOut);
	}

	public NeuralNet(NeuralNetLayer[] layers) {
		this.layers = layers;
	}

	public Vector apply(Vector input) {
		Vector output = null;
		for (NeuralNetLayer layer : layers) {
			output = layer.apply(input);
			input = output;
		}
		return output;
	}

	@Override
	public void finishTraining() {
		assert layers.length != 0;
		// TODO normalise output data
		for (int i = 0; i < EPOCHS; i++) {
			// TODO test for convergence and stop early
			for (int j = 0; j < trainingData.size(); j++) {
				Vector in = trainingData.get(j);
				Vector target = trainingDataLabels.get(j);
				finishTraining2(in, target);
			}
		}
		super.finishTraining();
	}

	// see http://www-speech.sri.com/people/anand/771/html/node37.html
	void finishTraining2(Vector in, Vector target) {
		// apply
		Vector out = apply(in);
		Vector[] delta = new Vector[layers.length];

		// delta for output layer
		NeuralNetLayer layer = layers[layers.length - 1];
		Vector err = out.copy().add(-1, target);
		delta[layers.length - 1] = layer.delta(err);

		// deltas for hidden layers
		Vector prevDelta = delta[layers.length - 1];
		NeuralNetLayer prevLayer = layer;
		for (int li = layers.length - 2; li > -1; li--) {
			layer = layers[li];
			Vector prevDeltaBackSum = DataUtils.newVector(layer.size());
			prevLayer.weights.transMult(prevDelta, prevDeltaBackSum);
			delta[li] = layer.delta(prevDeltaBackSum);
			assert delta[li].size() == layer.size();
			prevDelta = delta[li];
			prevLayer = layer;
		}

		// update
		for (int li = 0; li < layers.length; li++) {
			layers[li].finishTraining3_update(delta[li]);
		}
	}

	@Override
	public boolean isReady() {
		return super.isReady();
	}

	protected final Random random() {
		if (random == null) {
			random = Utils.getRandom();
		}
		return random;
	}

	@Override
	public void resetup() {
		for (NeuralNetLayer layer : layers) {
			layer.resetup(random());
		}
		super.resetup();
	}

	public void setRandomSource(Random randomSrc) {
		this.random = randomSrc;
	}

	@Deprecated
	@Override
	public void train(Iterable<? extends Vector> data)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public synchronized void train1(Vector x)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void train1(Vector input, Vector output, double weight) {
		assert DataUtils.max(output) <= 1 && DataUtils.min(output) >= 0 : output;
		super.train1(input, output, weight);
	}

}
