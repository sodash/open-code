/**
 * 
 */
package com.winterwell.nlp.cluster;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.datastorage.Vectoriser;
import com.winterwell.maths.matrix.CrudeDimensionReduction;
import com.winterwell.maths.stats.distributions.ATrainableBase;
import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.maths.stats.distributions.MixtureModel;
import com.winterwell.maths.stats.distributions.VariableClusterModel;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.utils.containers.TopNList;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

/**
 * @author daniel
 * @testedby DocClustererTest
 */
public class DocClusterer extends ATrainableBase<IDocument, Object> implements
		ITrainable.Unsupervised<IDocument> {

	private static final int SAMPLES = 5;

	private ObjectDistribution<List<IDocument>> clusters;

	private Vectoriser vectoriser;

	public DocClusterer(Vectoriser vectoriser) {
		this.vectoriser = vectoriser;
	}

	List<Vector> findNeighbours(int n, final Vector x, ArrayList<Vector> pts) {
		TopNList<Vector> list = new TopNList<Vector>(n,
				new Comparator<Vector>() {
					@Override
					public int compare(Vector o1, Vector o2) {
						double d1 = DataUtils.dist(x, o1);
						double d2 = DataUtils.dist(x, o2);
						return -Double.compare(d1, d2);
					}
				});
		for (Vector vector : pts) {
			list.maybeAdd(vector);
		}
		return list;
	}

	@Override
	public void finishTraining() {
		// convert into vectors
		List<Vector> pts = new ArrayList<Vector>();

		Map<Vector, IDocument> vec2doc = new HashMap<Vector, IDocument>();
		for (IDocument doc : trainingData) {
			Vector vec = vectoriser.toVector(doc);
			pts.add(vec);
			vec2doc.put(vec, doc);
		}

		// Squash the vectors using dimension reduction
		// ...create squasher
		int dim = 100;
		CrudeDimensionReduction reducer = new CrudeDimensionReduction(100);
		reducer.resetup();
		reducer.train(pts);
		reducer.finishTraining();
		Matrix m = reducer.getMatrix();

		// ...squash
		Map<Vector, Vector> squash2original = new HashMap<Vector, Vector>();
		ArrayList<Vector> pts2 = new ArrayList<Vector>(pts.size());
		for (Vector v : pts) {
			Vector v2 = DataUtils.newVector(dim);
			m.mult(v, v2);
			pts2.add(v2);
			squash2original.put(v2, v);
		}

		// cluster
		// Find the clusters
		VariableClusterModel mm = new VariableClusterModel(dim);
		mm.resetup();
		mm.train(pts2);
		mm.finishTraining();
		MixtureModel bestModel = mm.getBestModel();

		// identify samples
		ObjectDistribution<IDistribution> comps = bestModel.getComponents();
		// HashMap<IDistribution, IDocument> cluster2samples = new
		// HashMap<IDistribution,IDocument>();
		clusters = new ObjectDistribution<List<IDocument>>();
		for (IDistribution cluster : comps) {
			Vector mean = cluster.getMean();
			List<Vector> ns = findNeighbours(SAMPLES, mean, pts2);
			ArrayList<IDocument> samples = new ArrayList<IDocument>();
			for (Vector v2 : ns) {
				Vector v = squash2original.get(v2);
				IDocument doc = vec2doc.get(v);
				// cluster2samples.put(cluster, doc);
				samples.add(doc);
			}
			double w = comps.prob(cluster);
			clusters.setProb(samples, w);
		}
	}

	public ObjectDistribution<List<IDocument>> getClusters() {
		return clusters;
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
	public void train(Iterable<? extends IDocument> data) {
		super.train(data);
	}

	@Override
	public synchronized void train1(IDocument x) {
		super.train1(x);
	}

}
