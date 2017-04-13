package com.winterwell.maths.hmm;

import java.util.Arrays;

import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.utils.BestOne;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

/**
 * A standard Hidden Markov Model NOT TESTED!!
 * 
 * @author Daniel
 */
public final class HMM {

	/**
	 * P(out|hidden), ie. row=output, col=hidden
	 */
	private final Matrix emissionProbs;
	Vector startProbs;

	/**
	 * P(row|col), ie. row=new, col=old
	 */
	private final Matrix transitionProbs;

	public HMM(Matrix transition, Matrix emission) {
		this.transitionProbs = transition;
		this.emissionProbs = emission;
		// TODO check they are normalised
		setStartProbs(DataUtils.filledVector(transitionProbs.numColumns(), 1));
	}

	/**
	 * P(row|col), ie. row=new, col=old
	 */
	public Matrix getTransitionMatrix() {
		return transitionProbs;
	}

	double PEmit(int obs, int hidden) {
		return emissionProbs.get(obs, hidden);
	}

	/**
	 * @param h
	 * @return starting prob for h
	 */
	private double PStart(int h) {
		return startProbs.get(h);
	}

	double PTrans(int newH, int oldH) {
		return transitionProbs.get(newH, oldH);
	}

	/**
	 * Sample from the HMM. Pick a random starting point and take a random walk
	 * forwards (with probabilities set by the observed sequence). Should
	 * produce a mediocre interpretation of the observed sequence.
	 * 
	 * @param observed
	 * @return hidden sequence
	 */
	@SuppressWarnings("unchecked")
	public int[] sample(int[] observed) {
		int[] hidden = new int[observed.length];
		for (int i = 0, n = observed.length; i < n; i++) {
			int o = observed[i];
			ObjectDistribution<Integer> d = new ObjectDistribution<Integer>();
			for (int h = 0, nh = transitionProbs.numRows(); h < nh; h++) {
				// P(h) = P(o|h)P(h|prev)
				double priorH;
				if (i == 0) {
					priorH = PStart(h);
				} else {
					int prev = hidden[i - 1];
					priorH = PTrans(h, prev);
				}
				double Po_h = PEmit(o, h);
				double Ph = Po_h * priorH;
				d.setProb(h, Ph);
			}
			int newH = d.sample();
			hidden[i] = newH;
		}
		return hidden;
	}

	private void setStartProbs(Vector probVector) {
		this.startProbs = probVector;
	}

	@Override
	public String toString() {
		return "HMM[\n" + transitionProbs + "\n" + emissionProbs + "]\n";
	}

	public int[] viterbi(int[] observed) {
		int n = observed.length;
		int numHiddens = transitionProbs.numColumns();
		double[] probs = new double[numHiddens];
		double[] prevProbs = new double[numHiddens];
		// token, time
		int[][] prevPaths = new int[numHiddens][n];
		int[][] bestPaths = new int[numHiddens][n];
		// Initialise from start Points and first observation
		for (int h = 0; h < numHiddens; h++) {
			int o0 = observed[0];
			double pStart = PStart(h);
			prevProbs[h] = PEmit(o0, h) * pStart;
			prevPaths[h][0] = h;
		}
		// For each observation
		for (int i = 1; i < n; i++) {
			int oi = observed[i];
			// update best path to each hidden
			for (int h = 0; h < numHiddens; h++) {
				// Find best connection from the previous paths
				BestOne<int[]> bestPathToHi = new BestOne<int[]>();
				for (int prevh = 0; prevh < numHiddens; prevh++) {
					int[] path = prevPaths[prevh];
					double Phi = prevProbs[prevh] * PTrans(h, prevh);
					bestPathToHi.maybeSet(path, Phi);
				}
				// Extend the path
				int[] path = bestPathToHi.getBest();
				int[] extPath = Arrays.copyOf(path, i + 1);
				extPath[i] = h;
				bestPaths[h] = extPath;
				double PHi = bestPathToHi.getBestScore() * PEmit(oi, h);
				probs[h] = PHi;
			}
			prevProbs = Arrays.copyOf(probs, probs.length);
			prevPaths = Arrays.copyOf(bestPaths, bestPaths.length);
		}
		// Take the best
		BestOne<int[]> bestPath = new BestOne<int[]>();
		for (int h = 0; h < numHiddens; h++) {
			int[] path = bestPaths[h];
			bestPath.maybeSet(path, probs[h]);
		}
		int[] best = bestPath.getBest();
		return best;
	}

	// public double PStart(Hidden h) {
	// if (!normalised) normalise();
	// return normStartPoints[hidden2int(h)];
	// }

}
