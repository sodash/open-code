package com.winterwell.maths.classifiers;

import java.util.Random;

import com.winterwell.maths.timeseries.DataUtils;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Vector;

/**
 * One layer in a {@link NeuralNet}
 * 
 * @author daniel
 * 
 */
public class NeuralNetLayer {

	private final DenseVector bias;
	/**
	 * learning speed
	 */
	double lambda = 0.1;

	private Vector lastInput;

	Vector sumIn;
	/**
	 * Each row is a node. So columns = weights to apply to the input
	 */
	final DenseMatrix weights;

	public NeuralNetLayer(int dimIn, int numNodes) {
		weights = new DenseMatrix(numNodes, dimIn);
		bias = new DenseVector(numNodes);
	}

	protected double activation(double sumIn) {
		// sigmoid
		return 1 / (1 + Math.exp(-sumIn));
	}

	public Vector apply(Vector input) {
		assert input.size() == numInputs() : input.size() + " v " + numInputs();
		lastInput = input;
		sumIn = DataUtils.newVector(weights.numRows());
		Vector output = DataUtils.newVector(weights.numRows());
		weights.mult(input, sumIn);
		for (int i = 0; i < sumIn.size(); i++) {
			double sum = sumIn.get(i);
			sum += bias.get(i);
			double value = activation(sum);
			output.set(i, value);
		}
		return output;
	}

	/**
	 * derivative of the activation
	 * 
	 * @return
	 */
	protected double dActivation(double sumIn) {
		double a = activation(sumIn);
		return a * (1 - a);
	}

	/**
	 * delta is dError / dSummedInput
	 * 
	 * @param prevDeltaBackSum
	 *            sum_over_prevNode prev-delta_prevNode *
	 *            weight-from-here-to-prevNode
	 * @return
	 */
	Vector delta(Vector prevDeltaBackSum) {
		Vector del = DataUtils.newVector(size());
		for (int node = 0; node < del.size(); node++) {
			double pds = prevDeltaBackSum.get(node);
			double dn = dActivation(sumIn.get(node)) * pds;
			del.set(node, dn);
		}
		return del;
	}

	public void finishTraining3_update(Vector delta) {
		assert delta.size() == weights.numRows();
		for (int node = 0; node < weights.numRows(); node++) {
			double dn = delta.get(node);
			assert numInputs() == lastInput.size();
			for (int i = 0; i < numInputs(); i++) {
				double dwi = -dn * lastInput.get(i) * lambda;
				weights.add(node, i, dwi);
			}
			double db = -dn * lambda;
			bias.add(node, db);
		}
	}

	int numInputs() {
		return weights.numColumns();
	}

	void resetup(Random random) {
		// random setup
		for (int r = 0; r < weights.numRows(); r++) {
			for (int c = 0; c < weights.numColumns(); c++) {
				double v = (random.nextDouble() - 0.5);
				weights.set(r, c, v);
			}
			double v = (random.nextDouble() - 0.5);
			bias.set(r, v);
		}
	}

	/**
	 * @return number of nodes
	 */
	int size() {
		return weights.numRows();
	}

}
