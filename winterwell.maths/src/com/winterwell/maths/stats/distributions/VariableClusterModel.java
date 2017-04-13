package com.winterwell.maths.stats.distributions;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.log.Log;

import no.uib.cipr.matrix.Vector;

/**
 * A MixtureModel which selects how many elements to use based on likelihood and
 * a prior. Uses {@link AxisAlignedGaussian}s
 * 
 * Prior: needs to be super-exponential. Why? P(data|model) = product P(x|model)
 * So a new cluster can score well by fitting a single outlier. To
 * counter-balance this we want Prior(n+1 clusters) < k.Prior(n clusters) for
 * any reasonable k. Which means Prior(n+1) < k^-n Suggestion: use k^(-n^2) or
 * similar, e.g. e^(-n^2)
 * 
 * TODO test and tune on some sample data
 * 
 * TODO a more general version using a {@link MixtureModelBase}??
 * 
 * @author Joe, Daniel
 * @testedby {@link VariableClusterModelTest}
 */
public class VariableClusterModel extends ADistribution implements
		ITrainable.Unsupervised<Vector> {

	/**
	 * 
	 * @param distribution
	 * @param data
	 * @return average log-likelihood, number of impossible data points
	 */
	public static double[] getLogLikelihood(MixtureModel distribution,
			Iterable<? extends Vector> data) {
		double lp = 1;
		int count = 0, impossible = 0;
		for (Vector datum : data) {
			double p = distribution.prob(datum);
			assert MathUtils.isProb(p);
			if (p == 0) {
				impossible++;
				continue;
			}
			lp += Math.log(p);
			count++;
			assert lp > -Double.MAX_VALUE : lp + " " + count;
		}
		double mlp = count == 0 ? 0 : lp / count;
		return new double[] { mlp, impossible };
	}

	private MixtureModel bestModel;

	private final int dimension; // Not actually used
	/**
	 * controls the prior on number of clusters
	 */
	private final double lambda = 1.1;
	private int maxClusters = 25;

	public VariableClusterModel(int dim) {
		super();
		this.dimension = dim;
	}

	@Override
	public double density(Vector x) {
		assert isReady();
		return bestModel.density(x);
	}

	@Override
	public void finishTraining() {
		if (trainingData == null || trainingData.size() < 2)
			// Do something sensible?
			throw new FailureException(
					"Insufficient data to train mixture model");
		double bestScore = -Double.MAX_VALUE;
		double fewestImpossibles = Double.MAX_VALUE;
		int plateau = 0;
		for (int numClusters = 1; numClusters < maxClusters; numClusters++) {
			assert trainingData.size() != 0;
			// this is equivalent to a prior of e^discount
			// -- since log(p*prior) = log(prior) + log(p)
			// ??Gives prior expected number of clusters = 1/lambda??
			double discount = -(numClusters * lambda);

			MixtureModel candidate = finishTraining2_createAndTrain(numClusters);
			if (candidate == null) {
				continue; // FAIL!
			}

			double[] logLikelihoodImpossibles = getLogLikelihood(candidate,
					trainingData);

			assert logLikelihoodImpossibles[0] > Double.NEGATIVE_INFINITY : Printer
					.toString(logLikelihoodImpossibles);
			Log.report(numClusters + " cluster model log likelihood "
					+ logLikelihoodImpossibles[0] + " discount " + discount
					+ " impossibles: " + logLikelihoodImpossibles[1]);

			if (logLikelihoodImpossibles[1] <= fewestImpossibles
					&& discount + logLikelihoodImpossibles[0] > bestScore) {
				bestScore = discount + logLikelihoodImpossibles[0];
				fewestImpossibles = logLikelihoodImpossibles[1];
				bestModel = candidate;
				plateau = 0;
				continue;
			} else {
				assert bestModel != null;
			}
			// If we've plateaued for too long break out
			plateau++;
			if (plateau > 1) {
				break;
			}
		}
		// TODO: Consider handling training failures with exceptions
		if (bestModel == null)
			throw new FailureException(
					"Failed to generate model, see log for details");
		trainingData = null;
		Log.i("ai.train", "Selecting " + bestModel.getComponents().size()
				+ " cluster model");
		super.finishTraining();
	}

	/**
	 * 
	 * @param numClusters
	 * @return null on failure
	 */
	private MixtureModel finishTraining2_createAndTrain(int numClusters) {
		try {
			return MixtureModel.createModel(numClusters, trainingData);
		} catch (Throwable e) {
			Log.report(e);
			return null;
		}
	}

	public MixtureModel getBestModel() {
		assert bestModel != null;
		return bestModel;
	}

	@Override
	public int getDim() {
		return dimension;
	}

	@Override
	public Vector getMean() {
		assert isReady();
		return bestModel.getMean();
	}

	@Override
	public boolean isReady() {
		return super.isReady();
	}

	@Override
	public void resetup() {
		super.resetup();
	}

	@Override
	public Datum sample() {
		return bestModel.sample();
	}

	/**
	 * 25 by default
	 * 
	 * @param maxClusters
	 */
	public void setMaxClusters(int maxClusters) {
		this.maxClusters = maxClusters;
	}

	@Override
	public String toString() {
		if (bestModel == null)
			return "VariableClusterModel(untrained)";
		return "VariableClusterModel(" + bestModel.gaussians.size() + "):"
				+ StrUtils.ellipsize(bestModel.toString(), 200);
	}

	@Override
	public void train(Iterable<? extends Vector> data) {
		super.train(data);
	}

	@Override
	public void train1(Vector data) throws UnsupportedOperationException {
		super.train1(data);
	}

	@Override
	public void train1(Vector x, Object tag, double weight) {
		super.train1(x, tag, weight);
	}
}
