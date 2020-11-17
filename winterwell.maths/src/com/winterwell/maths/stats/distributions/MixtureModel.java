package com.winterwell.maths.stats.distributions;

import java.util.List;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.stats.distributions.discrete.DiscreteMixtureModel;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.Range;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

/**
 * A generic mixture model. This is a probabilistic OR. This supports using any
 * underlying models as the components in the mixture. If using Gaussians, see
 * the subclass GMM which is more efficient for some methods.
 * <p>
 * The mixture is fitted using the Expectation Maximisation (EM) algorithm.
 * 
 * @see VariableClusterModel
 * @see DiscreteMixtureModel
 * @see ProductModel?? for the AND equivalent
 * @author daniel
 * @testedby  MixtureModelTest}
 */
public class MixtureModel<D extends IDistribution> extends
		MixtureModelBase<Vector, D> implements ITrainable.Unsupervised<Vector>,
		IDistribution {

	/**
	 * Create and train a mixture model using {@link AxisAlignedGaussian}s.
	 * 
	 * @param numClusters
	 * @param data
	 * @return
	 */
	public static MixtureModel createModel(int numClusters,
			List<? extends Vector> data) {

		assert numClusters > 0;
		assert data.size() > 0; // ??We need to be able to calculate variances
								// too

		// I think initializing the clusters here may be superfluous
		Vector mean = StatsUtils.mean(data);
		Vector var = StatsUtils.var(data);
		IGaussian g = new AxisAlignedGaussian(mean, var);
		MixtureModel gmm = new MixtureModel<AxisAlignedGaussian>(mean.size());
		for (int i = 0; i < numClusters; i++) {
			Vector a = mean.copy();
			// step away from the mean
			Vector offset = g.sample();
			a.add(offset);
			gmm.addDistribution(new AxisAlignedGaussian(a, var.copy()),
					1.0 / numClusters);
		}
		gmm.resetup();
		gmm.train(data);
		gmm.finishTraining();
		return gmm;
	}

	private final int dim;

	private Vector globalVariance;

	/**
	 * Much of the time you can let density stand in for probability. But it is
	 * dodgy - and plain wrong if point masses are used.
	 */
	boolean useDensityAsProbability = false;

	private Vector window;

	public MixtureModel(int dim) {
		this.dim = dim;
	}

	@Override
	public double density(Vector x) {
		assert DataUtils.isFinite(x) : x;
		gaussians.normalise();
		double total = 0;
		for (D g : gaussians) {
			double p = g.density(x);
			double w = gaussians.prob(g);
			total += p * w;
		}
		return total;
	}

	@Override
	protected void finishTraining2_init() {
		super.finishTraining2_init();
		globalVariance = StatsUtils.var(trainingData);
		window = globalVariance.scale(0.0001);
		// Vector mean = StatsUtils.mean(trainingData);
		// Vector var = StatsUtils.var(trainingData);
		// // do some small random initialisation training in the right area
		// IGaussian g = new AxisAlignedGaussian(mean, var);
		// for(int i=0; i<distributions.length; i++) {
		// ITrainable.Unsupervised<Vector> di =
		// ((ITrainable.Unsupervised<Vector>) distributions[i]);
		// // Resample - to ensure that each cluster sees some data
		// for(int j=0; j<10; j++) {
		// Vector x = g.sample();
		// di.train1(x);
		// }
		// di.finishTraining();
		// }
	}

	@Override
	public Matrix getCovar() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getDim() {
		return dim;
	}

	@Override
	/**
	 * Returned value cannot be used to set the mean -- use getGaussians() for that
	 */
	public Vector getMean() {
		Vector total = DataUtils.newVector(getDim());
		for (D g : gaussians) {
			Vector x = g.getMean();
			double w = gaussians.prob(g);
			total.add(w, x);
		}
		return total;
	}

	@Override
	public Range[] getSupport() {
		Range[][] supports = new Range[distributions.length][];
		for (int i = 0; i < distributions.length; i++) {
			Range[] si = ((IDistribution) distributions[i]).getSupport();
			supports[i] = si;
		}
		Range[] support = new Range[getDim()];
		for (int d = 0; d < support.length; d++) {
			support[d] = supports[0][d];
			for (int i = 1; i < distributions.length; i++) {
				Range r = supports[i][d];
				support[d] = r.max(support[d]);
			}
		}
		return support;
	}

	/**
	 * @return variance of each dimension. Uses sampling to give approximate
	 *         answers!
	 */
	@Override
	public Vector getVariance() {
		// I don't think there is a nice way to get the variance of a general
		// mixture model
		// ...which means we probably should have a GMM sub-class
		return ADistribution.getVariance(this);
	}

	@Override
	protected boolean isSingularity(D g) {
		return DataUtils.max(g.getVariance()) < MathUtils.getMachineEpsilon();
	}

	@Override
	public double logDensity(Vector x) {
		// I think we cannot do better: log(a+b) has no nice expansion
		double d = density(x);
		return Math.log(d);
	}

	public double prob(Vector center) {
		Vector tl = center.copy();
		Vector br = center.copy();
		tl.add(-1, window);
		br.add(1, window);
		return prob(tl, br);
	}

	@Override
	public double prob(Vector topLeft, Vector bottomRight) {
		gaussians.normalise();
		double total = 0;
		for (D g : gaussians) {
			double p = g.prob(topLeft, bottomRight);
			double w = gaussians.prob(g);
			total += p * w;
		}
		return total;
	}

	/**
	 * Sampled points are labelled with the mixture component that they came
	 * from. The timestamp is gibberish.
	 */
	@Override
	public Datum sample() {
		IDistribution g = gaussians.sample();
		Vector v = g.sample();
		return new Datum(v, g);
	}

	/**
	 * @param x
	 * @param gi
	 *            index into {@link #distributions}
	 * @return P(x | source_gi)
	 */
	@Override
	protected double trainOneIteration3_prob(Vector x, int gi) {
		IDistribution distI = (IDistribution) distributions[gi];
		if (useDensityAsProbability)
			return distI.density(x);
		// take the prob over a small box
		Vector minCorner = x.copy();
		Vector maxCorner = x.copy();
		minCorner.add(-1, window);
		maxCorner.add(1, window);
		double claim = distI.prob(minCorner, maxCorner);
		if (claim == 0) {
			// FIXME: Sometimes we're getting an infinity here
			// TODO: Check this tallies with line and box
			// NB prob() squashed close to zero to zero a bit too aggressively?
			double density = distI.density(x);
			claim = distI.prob(minCorner, maxCorner);
			// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
			// assert MathUtils.equalish(density, 0) : density;
		}
		return claim;
	}

}
