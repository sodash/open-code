package com.winterwell.maths.stats.distributions.discrete;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.winterwell.maths.stats.distributions.ADistributionBase;
import com.winterwell.utils.BestOne;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.AbstractMap2;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.TopNList;
import com.winterwell.utils.log.Log;

/**
 * Helper class for building discrete distributions.
 * 
 * @author Daniel
 * 
 */
public abstract class AFiniteDistribution<T> extends ADistributionBase<T>
		implements IFiniteDistribution<T>, Collection<T> {
	
	/**
	 * If >0, the total weight (i.e. the sum of values).<br>
	 * Methods which edit the weight MUST either adjust this or zero it!
	 * 
	 * NB: only a few sub-classes actually use this -- maybe move into them
	 */
	protected double cachedWeight;

	@Override
	public String toString() {
		try {
			return getClass().getSimpleName()+"["+Printer.toString(Containers
					.getValueSortedMap(asMap(), false, 12))+"]";
		} catch(Throwable ex) { // paranoia -- bug seen in ZF
			Log.e("distro", ex);
			return getClass().getSimpleName()+"[size="+size()+"]";
		}
	}
	
	@Override
	public Map<T, Double> asMap() {
		return new DistroMap(this);
	}
	
	/**
	 * TODO where should this go? On StreamClassifier??
	 * 
	 * @param <Label>
	 * @param <Term>
	 * @param classifier
	 * @param nPerModel
	 * @return
	 */
	public static <Label, Term> Map<Label, List<Term>> getTopDistinguishingTerms(
			Map<Label, AFiniteDistribution<Term>> classifier, int nPerModel) {
		// Build list of candidate terms
		HashSet<Term> terms = new HashSet();
		for (AFiniteDistribution<Term> dm : classifier.values()) {
			terms.addAll(dm);
		}
		// what is each one like?
		Map<Label,TopNList<Term>> topTerms = new ArrayMap();
		for(Label lbl : classifier.keySet()) {
			topTerms.put(lbl, new TopNList<Term>(nPerModel));
		}
		for (Term term : terms) {
			ObjectDistribution<Label> od = new ObjectDistribution();
			for (Label label : classifier.keySet()) {
				AFiniteDistribution<Term> model = classifier.get(label);
				od.setProb(label, model.prob(term));
			}
			if (od.getTotalWeight()==0) continue;
			od.normalise();
			Label winner = od.getMostLikely();
			double p = od.prob(winner);
			if (p <= 0.5) {
				if (od.size() == 2) continue;
			}
			TopNList<Term> tops = topTerms.get(winner);
			tops.maybeAdd(term, p);
		}
		return (Map) topTerms;
	}


	public AFiniteDistribution() {
	}

	@Deprecated
	@Override
	public boolean add(T e) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	@Override
	public boolean addAll(Collection<? extends T> c)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Add another distribution to this one (editing this), so afterwards<br>
	 * 		P'_this(a) = P_this(a) + alpha*P_other(a)		<br>
	 * Does not do any normalisation.
	 * <p>
	 * Can be thought of as "create a mixture model of this OR other"
	 * 
	 * @param alpha
	 *            The weight to apply.
	 * @param other
	 *            This is not edited
	 * @see AndDist which is the opposite type of merge.
	 */
	public void addAll(double alpha, IFiniteDistribution<T> other) {
		assert this != other : this;
		assert alpha >= 0;
		if (alpha == 0)
			return;
		Map<T, Double> xmap = other.asMap();
		for (Entry<T, Double> entry : xmap.entrySet()) {
			T key = entry.getKey();
			addProb(key, alpha * entry.getValue());
		}
	}
	
	/**
	 * Does p(x) = p(x) + dx.
	 * 
	 * @param obj
	 * @param dp Must be >= 0
	 *            Should we return the new prob?
	 * @throws UnsupportedOperationException
	 *             if {@link #setProb(Object, double)} is not supported.
	 */
	public void addProb(T obj, double dp) throws UnsupportedOperationException {
		double p = prob(obj) + dp;
		setProb(obj, p);
	}

	@Override
	public void clear() {
		for (Object x : toArray()) {
			setProb((T) x, 0);
		}
	}

	@Override
	public boolean contains(Object o) {
		return prob((T) o) != 0;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object object : c) {
			if (!contains(object))
				return false;
		}
		return true;
	}

	/**
	 * If this were a particle filter, how many particles do we have?
	 * @return from 1 to size()
	 */
	public double getEffectiveParticleCount() {
		double sumSq = 0;
		double totalWeight = getTotalWeight();
		for (T e : this) {
			double p = prob(e);
			sumSq += p * p;
		}
		// normalise
		sumSq = sumSq / (totalWeight * totalWeight);
		// If the particles are evenly weighted with p=1/n, then sumSq = n * (1/n^2) = 1/n
		// So the answer would be n.
		// At the other end of the spectrum, if 1 particle has all the weight, then sumSq=1.
		// So the answer would be 1.
		return 1 / sumSq;
	}

	@Override
	public T getMostLikely() {
		BestOne<T> best = new BestOne<T>();
		for (T t : this) {
			best.maybeSet(t, prob(t));
		}
		return best.getBest();
	}

	@Override
	public List<T> getMostLikely(int n) {
		// compare by probability
		TopNList<T> best = new TopNList<T>(n, new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				if (o1 == o2)
					return 0;
				double v1 = prob(o1);
				double v2 = prob(o2);
				if (v1 == v2)
					// arbitrary: order the keys
					return Containers.compare(o1, o2);
				return -Double.compare(v1, v2);
			}
		});
		for (T t : this) {
			// filter the zeroes
			if (prob(t) == 0) {
				continue;
			}
			best.maybeAdd(t);
		}
		// The comparator gives the TopNList a link to this distribution.
		// This can cause issues for serialisation, or garbage collection.
		// So we create a fresh list.
		return new ArrayList<T>(best);
	}

	
	@Override
	public double getTotalWeight() {
		// this is a slow method - so you may wish to cache the value. See ObjectDistribution which does so.
		double wt = 0;
		for (T x : this) {
			wt += prob(x); // NB: includes pseudo-counts
		}
		return wt;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public double logProb(T x) {
		// crappy implementation - override if you can do better
		return Math.log(prob(x));
	}

	@Override
	public double normProb(T x) {
		if (isNormalised())
			return prob(x);
		// TODO calculate normalisation
		throw new UnsupportedOperationException();
	}

	/**
	 * This MUST be called after any methods that alter the probs but do not
	 * also update the cachedWeight
	 */
	public void recalcTotalWeight() {
		cachedWeight = -1;
		normalised = false;
	}

	/**
	 * Equivalent to {@link #setProb(Object, double)} with p=0, but a bit less
	 * efficient (uses an extra hashmap lookup).
	 */
	@Override
	public boolean remove(Object o) {
		double p = prob((T) o);
		setProb((T) o, 0);
		return p != 0;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		for (Object x : c) {
			setProb((T) x, 0);
		}
		// this breaks the Collections interface contract!
		return true;
	}

	/**
	 * WARNING: beware of the class! e.g. String != Tkn ever
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		ArrayList remove = new ArrayList();
		for (T object : this) {
			if ( ! c.contains(object)) {
				remove.add(object);
			}
		}
		removeAll(remove);
		return ! remove.isEmpty();
	}

	/**
	 * Sample from this distribution. Works fine with un-normalised
	 * distributions.
	 * 
	 * @return an x selected by random weight
	 * @testedby {@link AFiniteDistributionTest#testSample()}
	 */
	@Override
	public T sample() {
		double totalWeight = getTotalWeight();
		if (totalWeight == 0)
			// bummer - pick anything?
			throw new IllegalStateException(
					"Cannot sample from empty distribution");
		double p = random().nextDouble() * totalWeight;
		double sum = 0;
		for (T e : this) {
			sum += prob(e);
			if (sum > p)
				return e;
		}
		// What? must be a rounding issue (or we have zero weight). Return
		// anything
		assert MathUtils.approx(totalWeight, 0) || MathUtils.approx(p, totalWeight) : p + " vs " + totalWeight;
		return Utils.getRandomMember(Containers.getList(this));
	}

	@Override
	public void setProb(T obj, double value)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		// inefficient - but doesn't need to know about how data is stored
		ArrayList<T> list = new ArrayList<T>();
		for (T x : this) {
			list.add(x);
		}
		return list.toArray();
	}

	@Override
	public <T2> T2[] toArray(T2[] a) {
		// inefficient - but doesn't need to know about how data is stored
		ArrayList<T> list = new ArrayList<T>();
		for (T x : this) {
			list.add(x);
		}
		return list.toArray(a);
	}

}


final class DistroMap<T> extends AbstractMap2<T, Double> {

	private final AFiniteDistribution<T> dist;
	
	public DistroMap(AFiniteDistribution<T> dist) {
		this.dist = dist;
	}
	@Override
	public Double get(Object key) {
		return dist.prob((T) key);
	}
	@Override
	public Double put(T key, Double value) {
		double old = dist.prob(key);
		dist.setProb(key, value);
		return old;
	}
	@Override
	public Set<T> keySet() throws UnsupportedOperationException {
		return new HashSet(dist); 
	}
}