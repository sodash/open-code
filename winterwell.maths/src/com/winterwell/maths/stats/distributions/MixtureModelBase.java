package com.winterwell.maths.stats.distributions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import com.winterwell.maths.ITrainable;

import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.log.Log;

import no.uib.cipr.matrix.DenseMatrix;

public abstract class MixtureModelBase<X, D extends IDistributionBase<X>>
		extends ADistributionBase<X> {

	/**
	 * Are distributions allowed to collapse to point infinities? false by
	 * default If false, singularities are declared dead.
	 */
	private final boolean allowSingularities = false;

	private ArrayList<D> deadClusters;

	/**
	 * same as the keyset of {@link #gaussians}
	 */
	protected IDistributionBase[] distributions;

	ObjectDistribution<D> gaussians = new ObjectDistribution<D>();

	private final int maxIterations = 50;

	private boolean trackWinners;

	private int unclaimedCount;

	/**
	 * Creates a blank mixture model. Use
	 * {@link #addDistribution(IGaussian, double)} to fill it with models.
	 * 
	 * @param dim
	 */
	public MixtureModelBase() {
	}

	/**
	 * This is used for adding (!) a new distribution to the model
	 * 
	 * @param g
	 *            - must be {@link ITrainable.Unsupervised}
	 * @param weight
	 *            - greater than zero, does not have to be normalised
	 */
	public void addDistribution(D g, double weight) {
		assert g != null;
		assert weight >= 0 : weight; // 0 will be ignored
		//assert (g instanceof Unsupervised) : g;
		gaussians.setProb(g, weight);
	}

	@Override
	public void finishTraining() {
		// assign randomly at first
		finishTraining2_init();

		// iterate
		for (int i = 0; i < maxIterations; i++) {
			int numClusters = gaussians.size();
			// TODO test for convergence and stop early!
			trainOneIteration();
			if (gaussians.size() < numClusters) {
				Log.report("Dead clusters at iteration " + i, Level.FINE);
			}
			if (gaussians.size() == 0) {
				Log.report("All clusters dead at iteration " + i, Level.FINE);
				break;
			}
		}

		gaussians.normalise();
		// Clear off training data etc.
		super.finishTraining();
	}

	/**
	 * Assign data points to random clusters. Then train the clusters. This
	 * means each cluster starts from a position which is "in the space".
	 */
	protected void finishTraining2_init() {
		Set<D> ds = gaussians.asMap().keySet();
		distributions = ds.toArray(new IDistributionBase[0]);
		assert distributions.length != 0 : this;

		// assign each datum to a random cluster
		gaussians.normalise();
		for (int xi = 0, n = trainingData.size(); xi < n; xi++) {
			X datum = trainingData.get(xi);
			D g = gaussians.sample();
			ITrainable.Unsupervised<X> dist = (ITrainable.Unsupervised) g;
			dist.train1(datum);
		}

		for (IDistributionBase dist : distributions) {
			((ITrainable.Unsupervised) dist).finishTraining();
		}
	}

	/**
	 * @return The underlying distributions, with their weights
	 */
	public ObjectDistribution<D> getComponents() {
		return gaussians;
	}

	@Override
	public boolean isReady() {
		if (pleaseTrainFlag)
			return false;
		for (D g : gaussians) {
			if (!(g instanceof ITrainable)) {
				continue;
			}
			if (!((ITrainable) g).isReady())
				return false;
		}
		return true;
	}

	protected boolean isSingularity(D g) {
		return false;
	}

	@Override
	public void normalise() {
		gaussians.normalise();
		for (D d : gaussians) {
			d.normalise();
		}
		normalised = true;
	}

	@Override
	public void resetup() {
		assert gaussians.size() != 0 : "call #addDistribution() first";
		trackWinners = false;
		for (D g : gaussians) {
			// Can our boys be trained?
			if (!(g instanceof ITrainable)) {
				Log.report("	Component "
						+ g
						+ " is not trainable. It's weight in the mixture will be fitted, but not it.");
				continue;
			}
			// ((ITrainable) g).resetup();
			// And do we need to track the winners of each point?
			if (!(g instanceof ITrainable.Unsupervised.Weighted)) {
				trackWinners = true;
			}
		}
		gaussians.resetup();
		super.resetup();
	}

	@Override
	public X sample() {
		IDistributionBase<X> g = gaussians.sample();
		X x = g.sample();
		// TODO label??
		return x;
	}

	@Override
	public void setRandomSource(Random randomSrc) {
		gaussians.setRandomSource(randomSrc);
		for (D g : gaussians) {
			g.setRandomSource(randomSrc);
		}
		super.setRandomSource(randomSrc);
	}

	@Override
	public String toString() {
		return gaussians.toString();
	}

	@Override
	public void train(Iterable<? extends X> x) {
		super.train(x);
	}

	@Override
	public void train1(X x) {
		super.train1(x);
	}

	@Override
	public void train1(X x, Object tag, double weightIgnored) {
		train1(x); // ignore tag
	}

	public void trainOneIteration() {
		assert trainingData.size() != 0;
		int numGaussians = distributions.length;

		// Sanity check: no singularities!
		if (!allowSingularities) {
			for (IDistributionBase dist : distributions) {
				assert !isSingularity((D) dist) : dist;
			}
		}

		// assign data points to source distributions
		// rows = data-points, columns = distributions
		DenseMatrix dataClaims = new DenseMatrix(trainingData.size(),
				numGaussians);
		// this holds the gi number for the winner, or -1 in the event of a tie
		int[] winners = trackWinners ? new int[trainingData.size()] : null;
		unclaimedCount = 0; // for debugging insight
		for (int xi = 0, n = trainingData.size(); xi < n; xi++) {
			trainOneIteration2_assignDataPoint(numGaussians, dataClaims,
					winners, xi);
		}
		// FIXME: Should this really be an assert?
		assert unclaimedCount != trainingData.size() : trainingData.size(); // nobody
																			// got
																			// anything?!
																			// What
																			// to
																			// do??

		// Fit the distributions, if they can be fitted
		deadClusters = new ArrayList<D>();
		for (int gi = 0; gi < numGaussians; gi++) {
			D g = (D) distributions[gi];
			double[] weights = new double[trainingData.size()];
			double wj = 0;
			for (int xi = 0; xi < weights.length; xi++) {
				double p = dataClaims.get(xi, gi);
				weights[xi] = p;
				wj += p;
			}
			// adjust weight
			gaussians.setProb(g, wj);
			if (wj == 0) { // no weight = dead, no longer part of the mixture
				deadClusters.add(g);
				continue;
			}

			// fit it to the data
			trainOneIteration2_fitDistribution(gi, g, weights, winners);
		}

		// Update done
		if (deadClusters.isEmpty())
			return;

		// Prune dead clusters
		for (D d : deadClusters) {
			gaussians.setProb(d, 0);
		}
		List<IDistributionBase> dists = new ArrayList(
				Arrays.asList(distributions));
		dists.removeAll(deadClusters);
		distributions = dists.toArray(new IDistributionBase[0]);
	}

	/**
	 * 
	 * @param numGaussians
	 * @param dataClaims
	 * @param winners
	 *            identifies for each point which distribution "won" in a
	 *            first-past-the-post way. Only the xith entry will be set by
	 *            this method, and then only if trackWinners is on.
	 * @param claims
	 * @param xi
	 */
	private void trainOneIteration2_assignDataPoint(int numGaussians,
			DenseMatrix dataClaims, int[] winners, int xi) {
		// what claim does each element make for this point? ie - what
		// likelihood does it give it
		double[] claims = new double[numGaussians]; // we could recycle this
													// object
		X x = trainingData.get(xi);
		double sum = 0;
		for (int gi = 0; gi < numGaussians; gi++) {
			double claim = trainOneIteration3_prob(x, gi); // = P(x | source_gi)
			assert MathUtils.isFinite(claim) : distributions[gi];
			claims[gi] = claim;
			sum += claim;
		}
		if (sum == 0) {
			unclaimedCount++;
			// TODO ?? we could repeat with a wider window.
			// Or we could use log-prob which is more resistant to
			// very-low-probs
			// This will especially be an issue for high dimensions where probs
			// are naturally lower
			sum = 1; // avoid infinities, the probs will all be zero
		}
		// Note: given how DenseMatrix works, an array copy could be done here
		for (int gi = 0; gi < numGaussians; gi++) {
			dataClaims.set(xi, gi, claims[gi] / sum);
		}
		// winners? (needed for distributions which can't handle weighted
		// training data)
		if (!trackWinners)
			return;
		int besti = -1;
		double bestp = 0;
		for (int gi = 0; gi < numGaussians; gi++) {
			double claim = claims[gi];
			if (claim < bestp) {
				continue;
			}
			if (claim == bestp) {
				besti = -1;
			} else {
				besti = gi;
			}
		}
		winners[xi] = besti;
	}

	private void trainOneIteration2_fitDistribution(int gi, D g,
			double[] weights, int[] winners) {
		// Adjust the distributions
		if (!(g instanceof ITrainable))
			return;
		try {
			ITrainable.Unsupervised<X> dist = (ITrainable.Unsupervised) g;
			dist.resetup();
			if (dist instanceof ITrainable.Unsupervised.Weighted) {
				((ITrainable.Unsupervised.Weighted<X>) dist).train(weights, trainingData);
				dist.finishTraining();
				return;
			}
			int a = 2;
			boolean won1 = false;
			// only train on points where this distribution won
			for (int xi = 0; xi < weights.length; xi++) {
				if (winners[xi] == gi) {
					dist.train1(trainingData.get(xi));
					won1 = true;
				}
			}
			dist.finishTraining();
			if (!won1) {
				// won nothing? declare it dead
				deadClusters.add(g);
				return;
			}
		} finally {
			// What to do if the distribution has collapsed to a singularity?
			// This can happen if e.g. a gaussian claims a single point
			if (isSingularity(g)) {
				if (allowSingularities) {
					Log.report("Warning! Singularity " + g, Level.FINER);
				} else {
					Log.report("Warning! Killing singularity " + g, Level.FINER);
					deadClusters.add(g);
				}
			}
		}
	}

	/**
	 * @param x
	 * @param gi
	 *            index into {@link #distributions}
	 * @return P(x | source_gi)
	 */
	protected abstract double trainOneIteration3_prob(X x, int gi);

}
