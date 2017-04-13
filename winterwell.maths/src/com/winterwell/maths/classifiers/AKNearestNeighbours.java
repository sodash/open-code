package com.winterwell.maths.classifiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.stats.distributions.ATrainableBase;
import com.winterwell.utils.containers.ListMap;

public abstract class AKNearestNeighbours<T, X> extends ATrainableBase<T, X> {

	private boolean debug = false;

	protected int k;

	private boolean quickClassifyOnExactMatch;

	protected AKNearestNeighbours(int k) {
		this.k = k;		
	}
	
	@Override
	public void train1(T x, X tag, double weightIgnored) {
		super.train1(x, tag, weightIgnored);
	}

	/**
	 * Given a map of classifications => potencies, pick the most potent.
	 * 
	 * @param potencies
	 * @return
	 */
	private X bestClassification(Map<X, Double> potencies) {
		double bestPotency = 0;
		X bestCandidate = null;
		for (Map.Entry<X, Double> entry : potencies.entrySet()) {
			X candidate = entry.getKey();
			Double potency = entry.getValue();
			printFindBestDebugging(bestPotency, bestCandidate, candidate,
					potency);
			if (potency > bestPotency) {
				bestCandidate = candidate;
				bestPotency = potency;
			}
		}
		return bestCandidate;
	}

	/**
	 * Given a map of the distances to each point in the training set and its
	 * classification, find the k closest neighbours and construct a hashmap of
	 * potencies for each possible classification.
	 * 
	 * @param distances
	 * @return
	 */
	private Map<X, Double> calculatePotencies(ListMap<Double, X> distances) {
		List<Double> distList = new ArrayList<Double>(distances.keySet());
		Collections.sort(distList);
		int seen = 0;
		Map<X, Double> potencies = new HashMap<X, Double>();
		findk: for (Double distance : distList) {
			for (X classification : distances.get(distance)) {
				if (debug) {
					System.out.println("Considering point with classification "
							+ classification.toString() + " at distance "
							+ distance);
				}
				Double potency = potencies.get(classification);
				if (potency == null) {
					potency = 0.0;
				}
				potencies.put(classification, potency + (1 / distance));
				if (++seen == k) {
					break findk;
				}
			}
		}
		return potencies;
	}

	public X classify(T x) {
		if (debug) {
			System.out.println("Classifying " + x.toString());
		}
		assert trainingData.size() != 0;
		ListMap<Double, X> distances = new ListMap<Double, X>();
		// calculate distance to all entries in training data
		for (int i = 0; i < trainingData.size(); i++) {
			T exemplar = trainingData.get(i);
			double distance = distance(exemplar, x);
			X classification = trainingDataLabels.get(i);
			if (quickClassifyOnExactMatch && distance == 0.0)
				return classification;
			distances.add(distance, classification);
		}
		if (debug) {
			System.out.println("\tdistances are " + distances.toString());
		}
		// find the k nearest and calculate potencies
		Map<X, Double> potencies = calculatePotencies(distances);
		if (debug) {
			System.out.println("\tpotencies are " + potencies.toString());
		}
		X classification = bestClassification(potencies);
		if (debug) {
			System.out.println("\tclassified as " + classification.toString()
					+ "\n");
		}
		return classification;
	}

	protected abstract double distance(T x, T y);

	@Override
	public final void finishTraining() {
		// do nothing
	}

	/**
	 * @return the debug
	 */
	public boolean isDebug() {
		return debug;
	}

	@Override
	public final boolean isReady() {
		return !trainingData.isEmpty();
	}

	/**
	 * Long and messy debugging print statement that I fear I'm going to need
	 * again.
	 * 
	 * @param bestPotency
	 * @param bestCandidate
	 * @param candidate
	 * @param potency
	 */
	private void printFindBestDebugging(double bestPotency, X bestCandidate,
			X candidate, Double potency) {
		if (debug) {
			System.out.print("Considering " + candidate.toString()
					+ " with potency " + potency);
			if (bestCandidate != null) {
				System.out.println(", best so far " + bestCandidate.toString()
						+ " with potency " + bestPotency + ".");
			} else {
				System.out.println(".");
			}
		}
	}

	@Override
	public void resetup() {
		super.resetup();
	}

	/**
	 * @param debug
	 *            the debug to set
	 */
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	/**
	 * If true, the classifier will return as soon as it finds an example with
	 * distance zero. False by default
	 */
	public void setQuickClassifyOnExactMatch(boolean quickClassifyOnExactMatch) {
		this.quickClassifyOnExactMatch = quickClassifyOnExactMatch;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[k=" + k + ", |data|="
				+ trainingData.size() + "]";
	}

	@Deprecated
	@Override
	public void train(Iterable<? extends T> data)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	@Override
	public final void train1(T x) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}


}