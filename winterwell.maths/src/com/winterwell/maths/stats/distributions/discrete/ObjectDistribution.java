package com.winterwell.maths.stats.distributions.discrete;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.chart.FiniteDistributionChart;
import com.winterwell.maths.datastorage.IForget;
import com.winterwell.maths.datastorage.IPruneListener;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Mutable;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.utils.web.SimpleJson;

/**
 * A discrete distribution over a set of objects.
 * <p>
 * By default this assigns zero probability to items it hasn't seen. This can be
 * over-ridden using {@link #setPseudoCount(double)}.
 * <p>
 * Use {@link FiniteDistributionChart} to view this.
 *
 * @author daniel
 *
 * @testedby {@link ObjectDistributionTest}
 * @param <X>
 */
public class ObjectDistribution<X> extends AFiniteDistribution<X> implements
		Serializable, ITrainable.Unsupervised.Weighted<X>, IHasJson  {

	/**
	 * If true, it is an error to call {@link #normalise()}.
	 * This is a safety flag to protect the original data counts.
	 * You can still get normalised proabilities from {@link #normProb(Object)}
	 */
	private Boolean doNotNormalise;
	
	/**
	 * False by default. If true, it is an error to call {@link #normalise()}.
	 * This is a safety flag to protect the original data counts.
	 * You can still get normalised probabilities from {@link #normProb(Object)}
	 */
	public void setDoNotNormalise(boolean doNotNormalise) {
		this.doNotNormalise = doNotNormalise;
	}
	
	private static final String OTHER = "other";

	@Override
	public Object toJson2() throws UnsupportedOperationException {
		// a sorted map??
		return asMap();
	}
	
	/**
	 * @param n
	 * @return the top n, sorted highest-first, plus "other" if there is a remainder.
	 */
	public Map toJson2(int n) {
		List<X> keys = getMostLikely(n);
		Map map = new ArrayMap();
		double ttl=0;
		for (X x : keys) {
			double p = get(x);
			ttl += p;
			map.put(x, p);
		}
		double wt = getTotalWeight() + pseudoCount*size();
		if (ttl<wt && size() > n) {
			double rem = wt-ttl;
			map.put(OTHER, rem);
		}
		return map;
	}
	
	@Override
	public String toJSONString() {
		return new SimpleJson().toJson(toJson2());
	}
	
	@Override
	public void appendJson(StringBuilder sb) {
		sb.append(toJSONString());
	}

	/**
	 * @return true if this is in the backing map.
	 * 
	 * Note: if a pseudo-count has been set, then x-in-the-collection
	 * is not equivalent to P(x) != 0
	 */
	@Override
	public boolean contains(Object o) {
		return backing.containsKey(o);
	}
	
	private static final long serialVersionUID = 1L;

	/**
	 * This is a synchronized HashMap by default -- but users can provide their own
	 * map via {@link #ObjectDistribution(Map, boolean)}
	 */
	private final Map<X, Double> backing;

	@Override
	public final void train(double[] weights, Iterable<? extends X> wdata) {
		int i=0;
		for (X x : wdata) {
			double wi = weights[i];
			i++;
			train1(x, wi);
		}
	}

	/**
	 * 0 by default
	 */
	private double pseudoCount;

	/**
	 * Use {@link #setProb(Object, double)} to add objects
	 */
	public ObjectDistribution() {
		this(Collections.EMPTY_MAP);
	}

	/**
	 * Copy constructor
	 * @param distro
	 */
	public ObjectDistribution(IFiniteDistribution<X> distro) {
		this(distro.asMap());
	}

	/**
	 * Create a new ObjectDistribution that starts off identical to the given
	 * info, but can then be edited. Effectively this is a copy constructor.
	 *
	 * @param probs
	 *            This is copied
	 */
	public ObjectDistribution(Map<X, ? extends Number> probs) {
		// Used to be a ConcurrentHashMap, but this has
		// too large a memory footprint for heavy use.
		// TODO use TObjectDoubleHashMap<X> instead (test speed)??
		// TODO use Mutable.Dble instead of Double for faster add??
		// TODO allow replacing the map?
		this.backing = Collections
				.synchronizedMap(new HashMap<X, Double>(probs.size()));
		// Using setProb will strip out zeroes and safety check
		for(Map.Entry<X, ? extends Number> e : probs.entrySet()) {
			Number v = e.getValue();
			setProb(e.getKey(), v.doubleValue());
		}
		// save some memory
		noTrainingDataCollection();
	}

	/**
	 * Create a new ObjectDistribution using the given probs map.
	 * E.g. use case: a HalfLifeMap, so the distro won't grow too big.
	 *
	 * @param probs This will be used directly (without a defensive copy).
	 * @param copyMap Must be false.
	 */
	public ObjectDistribution(Map<X, Double> probs, boolean copyMap) {
		assert ! copyMap;
		this.backing = probs;
		// sanity check
		for(Double v : backing.values()) {
			assert MathUtils.isFinite(v) : probs;
		}
		// save some memory
		noTrainingDataCollection();
		// catch pruning events
		if (this.backing instanceof IForget) {
			IForget forgetfulMap = (IForget) backing;
			forgetfulMap.addListener(new PruneListener());
		}
	}
	
	/**
	 * Respond to pruning events by re-calculating the cached weight.
	 */
	private final class PruneListener implements IPruneListener, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public void pruneEvent(List pruned) {
			cachedWeight = -1;
		}		
	}


	/**
	 * Create a new ObjectDistribution using the given tags & probs.
	 */
	public ObjectDistribution(String[] tags, double[] probs) {
		// Create with a suitable backing map
		this(ArrayMap.useMe(tags.length), false);
		double total = 0;
		for (int i = 0; i < probs.length; i++) {
			double pi = probs[i];
			assert MathUtils.isFinite(pi) : tags[i]+" = "+pi;
			backing.put((X)tags[i], pi);
		}
		cachedWeight = total;
	}





	@Override
	public final void addProb(X obj, double dp) {
		if (dp==0) return; //no-op
		normalised = false;
		if (dp<0) {
			throw new IllegalArgumentException("addProb "+dp+" < 0 for "+obj);
		}
		// Note: The super method would add in the pseudo-count as well
		// -- which over-eggs the training slightly.

		// This has some thread-safety
		double newValue = Containers.plus(backing, obj, dp);

		// NB: when caching weights with a forgetful map - prune-events will invalidate the cache
		if (cachedWeight > 0) {
			// NB: cachedWeight=0 could be a new map, or could be a freshly loaded (but not empty) map.
			// cachedWeight=-1 means a cache invalidation
			cachedWeight += dp;
			// Is it a new addition?
			if (newValue != dp) {
				cachedWeight += pseudoCount;	
			}
		}
	}

	/**
	 * Equivalent to using the map constructor with {@link #getMap()}.
	 */
	public ObjectDistribution<X> copy() {
		ObjectDistribution<X> od = new ObjectDistribution<X>(backing);
		od.pseudoCount = pseudoCount;
		return od;
	}

	/**
	 * Count the occurrence of each item. Convenience method for
	 * {@link #addProb(Object, double)} or {@link #setProb(Object, double)}.
	 * <i>This is identical to {@link #train1(Object)}</i>. Used when in
	 * histogram mode.
	 *
	 * @param x
	 */
	public final void count(X x) {
		train1(x);
	}

	/**
	 * No-op (not needed and does nothing).
	 * 
	 * Note: You may wish to call {@link #normalise()}. 
	 */
	// NB: It is handy in some use-cases for this class to retain the raw counts.
	// Hence why this method does not call normalise itself.
	@Override
	public void finishTraining() {
		super.finishTraining();
	}

	/**
	 * Equivalent to {@link #prob(Object)}. Provided for similarity with Map.
	 */
	public final double get(X item) {
		return prob(item);
	}

	/**
	 * @return If pseudocount=0, then this is the backing map itself. 
	 * If pseudocount !=0, then we wrap the backing map to include pseudocounts. 
	 * Do NOT edit!
	 * @see #getBackingMap() 
	 */
	public Map<X, Double> asMap() {
		if (pseudoCount==0) return
				// Don't wrap this, as WWModel & others may need to know if this is an IForget map
				backing;
		return super.asMap();
	}
	
	/**
	 * @return the underlying map. 
	 */
	public Map<X, Double> getBackingMap() {
		return backing;
	}

	@Override
	public X getMostLikely() {
		assert size() != 0;
		return getSortedObjects().get(0);
	}

	
	/**
	 * @return The objects in the space, ordered with the most probable first.
	 */
	public List<X> getSortedObjects() {
		List<X> keys = Containers.getSortedKeys(backing);
		Collections.reverse(keys);
		return keys;
	}

	@Override
	public final double getTotalWeight() {
		// does NOT include pseudo-counts
		// this is a slow method - so cache the value
		if (cachedWeight > 0)
			return cachedWeight;
		double _cachedWeight = 0; //residue;
		Collection<Double> vs = backing.values();
		for (Double p : vs) {
			assert p!=null && p>=0 : p+" "+this;
			_cachedWeight += p;
		}
		_cachedWeight += pseudoCount * size();
		cachedWeight = _cachedWeight;
		assert MathUtils.isFinite(_cachedWeight) : _cachedWeight+" "+this;
		return cachedWeight;
	}

	@Override
	public boolean isReady() {
		return true;
	}

	/**
	 * Iterate over the known values.
	 * 
	 * Note: if a pseudo-count has been set, then x-in-the-collection
	 * is not equivalent to P(x) != 0
	 */
	@Override
	public Iterator<X> iterator() {
		return backing.keySet().iterator();
	}

	public final Set<X> keySet() {
		return backing.keySet();
	}

	/**
	 * Convenience method to set P(object) *= a
	 * 
	 * @param initial If P(object) is unset, what valuie should it start at?
	 * Normally either 0 (if you aren't in, then you don't get added), or 1 (start all-in and decay). 
	 */
	public void multiply(X obj, double a, double initial) {
		assert pseudoCount==0 : pseudoCount;
		Double p = backing.get(obj);
		if (p==null) {
			// obj is new
			if (initial==0) return;
			setProb(obj, initial * a);
			return;
		}		
		setProb(obj, p * a);
	}
	
	/**
	 * Multiply all stored values.
	 * So P(x)-after = a * P(x)-before
	 * @param a If 0, this will clear out the entries.
	 */
	public void multiply(double a) {
		assert pseudoCount==0 : pseudoCount;
		if (a==0) {
			clear();
			return;
		}		
		for(Map.Entry<X,Double> xp : backing.entrySet()) {
			double p2 = xp.getValue() * a;
			xp.setValue(p2);
		}
		// mark cached weight as out of date
		normalised = false;
		cachedWeight = -1;
	}

	@Override
	public final synchronized void normalise() {
		if (isNormalised())
			return;
		if (doNotNormalise!=null && doNotNormalise) {
			throw new IllegalStateException("This distribution is set to 'doNotNormalise' to protect counts.");
		}
		// NB: This method does not modify pseudocount.
		// if the pseudo-count is high, you'll never get probabilities out
		assert pseudoCount < 1.0 / size() : "pseudocount too high: "+pseudoCount;

		normalised = true;
		double sum = 0, count = 0;
		for (Double d : backing.values()) {
			sum += d;
			count++;
		}
		assert count == size();
		if (count == 0) {
			cachedWeight = 0;
			return;
		}
		if (sum == 0) {
			// no prob weight! - split evenly
			double split = (1.0 - pseudoCount * count) / count;
			for (Map.Entry<X, Double> e : backing.entrySet()) {
				e.setValue(split);
			}
			cachedWeight = 1;
			return;
		}

		// how much is a "unit weight" now?
		double n = (1.0 - pseudoCount * count) / sum;

		for (Map.Entry<X, Double> e : backing.entrySet()) {
			e.setValue(e.getValue() * n);
		}
		cachedWeight = 1;
	}

	/**
	 * The normalised probability (i.e. an actual probability).
	 *
	 * @param obj
	 * @return in [0,1]
	 */
	@Override
	public double normProb(X obj) {
		double po = prob(obj);
		double wt = getTotalWeight();
		if (wt==0) {
			// Wot no prob weight? So this is a completely untrained map.
			// TODO return nan??
//			return Double.NaN;
			// Assume all is fine.
			assert po==0 || pseudoCount > 0;
			assert backing.isEmpty() : backing;
			if (pseudoCount>0) {
				// How many possible values to count in?
				return po / (2*pseudoCount);
			}
			assert MathUtils.isProb(po) : po;
			return po;
		}
		double p = po / wt;
		assert MathUtils.isProb(p) : p+" = "+po+"/"+wt+" for "+obj;
		return p;
	}

	/**
	 * @param obj
	 *            Cannot be null
	 * @return the probability given to this object. This need not be
	 *         normalised! A common use case it to never normalise, and to use
	 *         this class as a counter. To get a normalised prob, use
	 *         {@link #normProb(Object)}
	 */
	@Override
	public final double prob(X obj) {
		assert obj != null;
		Double p = backing.get(obj);
		return p == null ? pseudoCount : p + pseudoCount;
	}

	/**
	 * Prune the histogram, keeping only the more common entries
	 *
	 * @param buckets
	 *            The number of buckets to keep
	 * @return The pruned keys (ie the entries just removed)
	 */
	public List<X> prune(int buckets) {
		return prune(buckets, null);
	}

	/**
	 * Prune the histogram, keeping only the more common entries
	 *
	 * @param buckets
	 *            The number of buckets to keep
	 * @param prunedWeight
	 *            Can be null. Provides a secondary output: the total weight
	 *            that has been removed. This can be used to create an "other"
	 *            slice in pie-charts. E.g. <code>
		Mutable.Dble prunedWeight = new Mutable.Dble();
		dist.prune(10, prunedWeight);
		dist.setProb("other", prunedWeight.value);
		</code> (nb: the previous value of
	 *            prunedWeight will be discarded).
	 * @return The pruned keys (ie the entries just removed)
	 */
	public List<X> prune(int buckets, Mutable.Dble prunedWeight) {
		if (prunedWeight != null) {
			prunedWeight.value = 0;
		}
		int pruneCount = backing.size() - buckets;
		// Do we need to prune?
		if (pruneCount < 1)
			return Collections.emptyList();
		// prunedFlag = true;
		// Get the lowest ranking keys
		List toPrune = new ArrayList(pruneCount);
		for (Object k : Containers.getSortedKeys(backing)) {
			toPrune.add(k);
			if (toPrune.size() == pruneCount) {
				break;
			}
		}
		// Remove them
		for (Object k : toPrune) {
			Double v = backing.remove(k);
			assert v >= 0 : k+"="+v;
			if (prunedWeight != null) {
				prunedWeight.value += v;
			}
		}
		normalised = false;
		return toPrune;
	}

	public ArrayList<X> pruneBelow(double minProb) {
		return pruneBelow(minProb, null);
	}

	/**
	 * Remove all entries which have probability below the threshold. Note: this
	 * does not normalise either before or after.
	 *
	 * @param minProb
	 *            or min-count if un-normalised.
	 * @param prunedWeight
	 *            Can be null. Provides a secondary output: the total weight
	 *            that has been removed. This can be used to create an "other"
	 *            slice in pie-charts. E.g. <code>
		Mutable.Dble prunedWeight = new Mutable.Dble();
		dist.pruneBelow(0.05, prunedWeight);
		dist.setProb("other", prunedWeight.value);
		</code> (nb: the previous value of
	 *            prunedWeight will be discarded).
	 * @return The pruned keys (ie the entries just removed).
	 */
	public ArrayList<X> pruneBelow(double minProb, Mutable.Dble prunedWeight) {
		if (prunedWeight != null) {
			prunedWeight.value = 0;
		}
		ArrayList<X> removed = new ArrayList();
		for (Object k : backing.keySet().toArray()) {
			X x = (X) k;
			double px = prob(x);
			if (px >= minProb) {
				continue;
			}
			backing.remove(k);
			removed.add(x);
			if (prunedWeight != null) {
				prunedWeight.value += px;
			}
		}
		return removed;
	}

	/**
	 * Equivalent to {@link #setProb(Object, double)}. Provided for similarity
	 * with Map.
	 */
	public final void put(X tag, double prob) {
		setProb(tag, prob);
	}

	/**
	 * Set to uniform: all info is discarded (including the keyset!)
	 */
	@Override
	public void resetup() {
		super.resetup();
		backing.clear();
		// double p = 1.0/size();
		// for(Entry<X, Double> e : backing.entrySet()) {
		// e.setValue(p);
		// }
	}

	/**
	 * Set the probability for this object. Will mark the distribution as
	 * un-normalised. Setting a zero probability will remove the object from the
	 * distribution.
	 * <p>
	 * WARNING: cannot be called with zero values while looping over the objects
	 * in this distribution, as the removal will create a
	 * {@link ConcurrentModificationException}. Create an array-copy of the keys
	 * using getMap().keySet().toArray() if you need to do this.
	 * <p>
	 * WARNING: either value=0 or pseudocount MUST = 0
	 *
	 * @param obj
	 * @param value
	 *            Does not need to be normalised
	 */
	@Override
	public final void setProb(X obj, double value) {
		if ( ! MathUtils.isFinite(value) || value < 0) {
			throw new IllegalArgumentException("Prob must be >= 0: P("+obj+") = "+ value + "\t in " + this);
		}
		normalised = false;
		if (value == 0) {
			Double old = backing.remove(obj);
			if (old != null && cachedWeight>0) {
				cachedWeight -= old;
			}
			return;
		}
		// setProb with pseudocount suggests a bug -- except this is how count()/train2() works!
//			assert pseudoCount==0 : pseudoCount+" "+obj+" "+value;
		Double old = backing.put(obj, value);
		if (cachedWeight>0) {
			if (old != null) {
				cachedWeight -= old;
			}
			cachedWeight += value;
		}
	}

	/**
	 * A simple method for handling unseen items. The pseudo-count is added to
	 * all values returned by {@link #prob(Object)}. size() * pseudoCount is
	 * added to {@link #getTotalWeight()}.
	 * <p>
	 * Users of {@link ObjectDistribution} may implement their own (better)
	 * pseudocount system.
	 *
	 * @param pseudoCount
	 *            0 (the default) for no pseudo-count
	 * @return this
	 */
	public ObjectDistribution<X> setPseudoCount(double pseudoCount) {
		if ( ! MathUtils.isFinite(pseudoCount) || pseudoCount <0) {
			throw new IllegalArgumentException("pseudocount should be >= 0: "+pseudoCount);
		}
		this.pseudoCount = pseudoCount;		
		// clear cache
		normalised = false;
		cachedWeight = -1;
		return this;
	}
	
	public double getPseudoCount() {
		return pseudoCount;
	}

	/**
	 * @return number of objects in this distribution.
	 * @see #getTotalWeight()
	 * @see #getEffectiveParticleCount()
	 */
	@Override
	public int size() {
		return backing.size();
	}

	@Override
	public Object[] toArray() {
		return backing.keySet().toArray();
	}

	/**
	 * In keeping with being almost a Collection, you can get an array of the
	 * elements.
	 *
	 * @param array
	 * @return things this distribution is over
	 */
	@Override
	public <X2> X2[] toArray(X2[] array) {
		return backing.keySet().toArray(array);
	}

	

	@Override
	public void train(Iterable<? extends X> data) {
		super.train(data);
	}

	/**
	 * Equivalent to {@link #addProb(Object, double)} with dp = 1
	 */
	@Override
	public final void train1(X data) {
		assert data != null;
		// assert ! isNormalised(); training after normalisation can be
		// inefficient (need to renormalise)
		// -- but it's legitimate
		addProb(data, 1);
	}

	/**
	 * Equivalent to {@link #addProb(Object, double)}
	 */
	@Override
	public final void train1(X data, double weight) {
		assert data != null;
		addProb(data, weight);
	}

	/**
	 * Return a set of randomly selected elements of the specified
	 * collection. Equality in the set is determined in the usual way i.e. by
	 * calling equals(). If num is greater than choices, then return all the
	 * choices in a fresh object.
	 * <p>
	 * This has a time-out which can lead to it stopping early & short.
	 *
	 * @param num Can be 0, but not lower (throws IllegalArgumentException)
	 * @return set of randomly selected choices.
	 */
	public Set<X> sample(int num) {
		if (num >= size()) {
			// the lot
			return new HashSet<X>(this);
		}
		if (num < 0)
			throw new IllegalArgumentException(num+" must be >= 0");
		Set<X> selected = new HashSet<X>(num);
		int iter = 0;
		while (selected.size() < num) {
			X x = sample();
			selected.add(x);
			// time out if we get stuck
			iter++;
			if (iter == num * 100) {
				Log.report("Breaking out of random-selection loop early: Failed to select a full "
						+ num + " from " + size());
				break;
			}
		}
		return selected;
	}

	

}
