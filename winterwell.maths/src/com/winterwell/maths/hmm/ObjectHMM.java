package com.winterwell.maths.hmm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.winterwell.maths.matrix.ObjectMatrix;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.utils.BestOne;

/**
 * A non-numerical Hidden Markov Model
 * 
 * @author Daniel
 * 
 * @param <Hidden>
 * @param <Observed>
 */
public class ObjectHMM<Hidden, Observed> {
	
	public void setHiddenStates(List<Hidden> hiddenStates) {
		this.hiddenStates = hiddenStates;
		this.hiddenStateCount.clear();
		for (Hidden hidden : hiddenStates) {
			hiddenStateCount.train1(hidden);
		}
		this.hiddenStateCount.normalise();
	}

	// HACK: quick fix because ConcurrentHashMap doesn't accept null keys
	// Looks the class can be rewritten not to require them
	// OTOH the whole thing should probably be ported to maths.stats
	// style
	private final static Object UNSEEN_STATE = new Object();
	/**
	 * If false, a previously unseen observation will trigger an exception
	 * during Viterbi sequencing. Default is true.
	 */
	private boolean allowUnseenObservations = true;
	/**
	 * A Dirichlet prior on outputs
	 */
	private double anyOutputIsPossible = 1;

	/**
	 * Corresponds to a Dirichlet prior on transitions
	 */
	private double anyTransitionIsPossible = 1;

	private final ObjectDistribution<Hidden> hiddenStateCount = new ObjectDistribution<Hidden>();
	
	private transient List<Hidden> hiddenStates;

	private boolean normalised;

	/** Observed=row, hidden=col */
	private transient double[][] normOutputProbs;
	private transient double[] normStartPoints;

	private transient double[][] normTransitionProbs;
	/**
	 * Index 0=null, for "not seen before" in Viterbi sequencing
	 */
	private transient List<Observed> observedStates = new ArrayList<Observed>();
	/**
	 * P(out|hidden), ie. row=output, col=hidden
	 */
	private final ObjectMatrix<Observed, Hidden> outputProbs = new ObjectMatrix<Observed, Hidden>();
	private final ObjectDistribution<Hidden> startPoints = new ObjectDistribution<Hidden>();
	/**
	 * P(row|col), ie. row=new, col=old
	 */
	private final ObjectMatrix<Hidden, Hidden> transitionProbs = new ObjectMatrix<Hidden, Hidden>();

	public ObjectHMM() {
		// Add a null entry for "not seen before" in viterbi
		((List) observedStates).add(UNSEEN_STATE);
	}

	/**
	 * P(out|hidden), ie. row=output, col=hidden
	 */
	public ObjectMatrix<Observed, Hidden> getEmissionMatrix() {
		return outputProbs;
	}

	public Collection<Hidden> getHiddenStates() {
		return hiddenStateCount.getSortedObjects();
	}

	public Collection<Observed> getObservedStates() {
		return observedStates;
	}

	/**
	 * P(row|col), ie. row=new, col=old
	 */
	public ObjectMatrix<Hidden, Hidden> getTransitionMatrix() {
		return transitionProbs;
	}

	private int hidden2int(Hidden h) {
		return hiddenStates.indexOf(h);
	}

	public void normalise() {
		// Collect hidden and observed
		hiddenStates = hiddenStateCount.getSortedObjects();
		observedStates.clear();
		observedStates.addAll(outputProbs.getRowValues());		
		// Start (includes anyTransitiion prob)
		normStartPoints = new double[hiddenStates.size()];
		ObjectDistribution<Hidden> pStart = new ObjectDistribution<Hidden>();
		for (Hidden h : hiddenStates) {
			pStart.setProb(h, startPoints.prob(h) + anyTransitionIsPossible);
		}
		pStart.normalise();
		for (Hidden h : hiddenStates) {
			normStartPoints[hidden2int(h)] = pStart.prob(h);
		}
		// Transition
		normTransitionProbs = new double[hiddenStates.size()][hiddenStates
				.size()];
		for (Hidden oldH : hiddenStates) {
			int ohi = hidden2int(oldH);
			ObjectDistribution<Hidden> pTrans = new ObjectDistribution<Hidden>();
			for (Hidden newH : hiddenStates) {
				pTrans.setProb(newH, transitionProbs.get(newH, oldH)
						+ anyTransitionIsPossible);
			}
			try {
				pTrans.normalise();
				for (Hidden newH : hiddenStates) {
					normTransitionProbs[hidden2int(newH)][ohi] = pTrans
							.prob(newH);
				}
			} catch (Exception e) {
				// Probability zero!
				assert anyTransitionIsPossible == 0;
				for (Hidden newH : hiddenStates) {
					normTransitionProbs[hidden2int(newH)][ohi] = 0;
				}
			}
		}
		// Emission
		normOutputProbs = new double[observedStates.size()][hiddenStates.size()];
		for (Hidden h : hiddenStates) {
			ObjectDistribution<Observed> pOut = new ObjectDistribution<Observed>();
			for (Observed o : observedStates) {
				pOut.setProb(o, outputProbs.get(o, h) + anyOutputIsPossible);
			}
			int hi = hidden2int(h);
			try {
				pOut.normalise();				
				for (int oi = 0; oi < observedStates.size(); oi++) {
					normOutputProbs[oi][hi] = pOut.prob(observedStates.get(oi));
				}
			} catch (Exception e) {
				// Probability zero!
				assert anyOutputIsPossible == 0;
				for (int oi = 0; oi < observedStates.size(); oi++) {
					normOutputProbs[oi][hi] = 0;
				}
			}
		}
		// Done
		normalised = true;
	}

	private int observed2int(Object obs) {
		int i = observedStates.indexOf(obs);
		return i == -1 ? 0 : i; // the first (null) observed state
	}

	double PEmit(int obs, int hidden) {
		assert normalised;
		return normOutputProbs[obs][hidden];
	}

	public double PEmit(Observed obs, Hidden hidden) {
		if (!normalised) {
			normalise();
		}
		int hi = hidden2int(hidden);
		int oi = observed2int(obs);
		return PEmit(oi, hi);
	}

	/**
	 * 
	 * @param numToKeep
	 * @return The pruned states
	 */
	public List<Hidden> pruneHiddenStates(int numToKeep) {
		if (numToKeep >= hiddenStateCount.size())
			return Collections.emptyList();
		normalised = false;
		// assert hiddenStateCount.size() == hiddenStates.size();
		List<Hidden> keys = hiddenStateCount.getSortedObjects();
		List<Hidden> prune = keys.subList(numToKeep, keys.size());
		for (Hidden hidden : prune) {
			hiddenStateCount.setProb(hidden, 0);
			transitionProbs.removeRow(hidden);
			transitionProbs.removeColumn(hidden);
			outputProbs.removeColumn(hidden);
		}
		hiddenStates = null;
		return prune;
	}

	public double PStart(Hidden h) {
		if (!normalised) {
			normalise();
		}
		return normStartPoints[hidden2int(h)];
	}

	public double PTrans(Hidden newH, Hidden oldH) {
		if (!normalised) {
			normalise();
		}
		int ni = hidden2int(newH);
		int oi = hidden2int(oldH);
		double p = normTransitionProbs[ni][oi];
		return p;
	}

	double PTrans(int newH, int oldH) {
		assert normalised;
		return normTransitionProbs[newH][oldH];
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
	public List<Hidden> sample(List<Observed> observed) {
		if (!normalised) {
			normalise();
		}
		Object[] hidden = new Object[observed.size()];
		for (int i = 0, n = observed.size(); i < n; i++) {
			Observed o = observed.get(i);
			ObjectDistribution<Hidden> d = new ObjectDistribution<Hidden>();
			for (Hidden h : hiddenStates) {
				// P(h) = P(o|h)P(h|prev)
				double priorH;
				if (i == 0) {
					priorH = PStart(h);
				} else {
					Hidden prev = (Hidden) hidden[i - 1];
					priorH = PTrans(h, prev);
				}
				double Po_h = PEmit(o, h);
				double Ph = Po_h * priorH;
				d.setProb(h, Ph);
			}
			Hidden newH = d.sample();
			hidden[i] = newH;
		}
		return (List<Hidden>) Arrays.asList(hidden);
	}

	/**
	 * If false, a previously unseen observation will trigger an exception
	 * during Viterbi sequencing. Default is true.
	 */
	public void setAllowUnseenObservations(boolean allow) {
		allowUnseenObservations = allow;
	}

	public void setAnyOutputIsPossible(double p) {
		anyOutputIsPossible = p;
		normalised = false;
	}

	public void setAnyTransitionIsPossible(double p) {
		anyTransitionIsPossible = p;
		normalised = false;
	}

	// /**
	// * Row=Hidden, Column=Observed
	// */
	// public ObjectMatrix<Hidden, Observed> getEmissionMatrix() {
	// return outputProbs;
	// }

	@Override
	public String toString() {
		return "ObjectHMM[\n" + transitionProbs + "\n" + outputProbs + "]\n";
	}

	/**
	 * 
	 * @param hidden
	 *            A sequence of hidden states
	 * @param observed
	 *            A sequence of observed states
	 */
	public void trainOneExample(List<Hidden> hidden, List<Observed> observed) {
		assert hidden.size() == observed.size();
		// Start
		startPoints.addProb(hidden.get(0), 1);
		// Output
		for (int i = 0; i < hidden.size(); i++) {
			Hidden h = hidden.get(i);
			Observed o = observed.get(i);
			outputProbs.plus(o, h, 1);
		}
		// Transition
		for (int i = 0, n = hidden.size() - 1; i < n; i++) {
			Hidden h1 = hidden.get(i);
			Hidden h2 = hidden.get(i + 1);
			transitionProbs.plus(h2, h1, 1);
		}
		// Update indexes
		for (Observed o : observed) {
			if (observedStates.contains(o)) {
				continue;
			}
			observedStates.add(o);
		}
		for (Hidden h : hidden) {
			hiddenStateCount.count(h);
		}
		// Done
		normalised = false;
	}

	public List<Hidden> viterbi(List<Observed> visible) {
		if (!normalised) {
			normalise();
		}
		int n = visible.size();
		int numHiddens = hiddenStates.size();
		// Convert observed into integers
		int[] observed = new int[n];
		for (int i = 0; i < n; i++) {
			observed[i] = observed2int(visible.get(i));
			// Test for unseens
			if (!allowUnseenObservations && observed[i] == 0)
				throw new IllegalArgumentException(
						"Previously unseen observation: " + visible.get(i));
		}
		double[] probs = new double[numHiddens];
		double[] prevProbs = new double[numHiddens];
		// token, time
		int[][] prevPaths = new int[numHiddens][n];
		int[][] bestPaths = new int[numHiddens][n];
		// Initialise from start Points and first observation
		for (int h = 0; h < numHiddens; h++) {
			int o0 = observed[0];
			double pStart = normStartPoints[h];
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
					// TODO if we wanted to allow for (complex) programmatic filters
					// Then we would extend the path here, and check. But that means coping 
					// all paths so less efficient.
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
		for (int h = 0; h < hiddenStates.size(); h++) {
			int[] path = bestPaths[h];
			bestPath.maybeSet(path, probs[h]);
		}
		int[] best = bestPath.getBest();
		assert best != null;
		List<Hidden> hiddens = new ArrayList<Hidden>();
		for (int i : best) {
			hiddens.add(hiddenStates.get(i));
		}
		return hiddens;
	}

}
