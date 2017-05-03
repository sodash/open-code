package com.winterwell.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple utility class to find the highest-scoring items.
 * 
 * @see BestOne which is faster but only returns the first best item
 * @author daniel
 */
public final class Best<T> {

	private final List<T> best = new ArrayList<T>();
	private double bestScore = Double.NEGATIVE_INFINITY;

	public boolean isEmpty() {
		return best.isEmpty();
	}
	
	/**
	 * @return the highest-scoring item. Ties are broken at random. null if empty
	 */
	public T getBest() {
		if (best.size() == 1) return best.get(0);
		// break ties randomly
		return Utils.getRandomMember(best);
	}

	public List<T> getBestList() {
		return best;
	}

	public double getBestScore() {
		return bestScore;
	}

	public void maybeSet(T eg, double score) {
		if (score > bestScore) {
			best.clear();
			best.add(eg);
			bestScore = score;
		} else if (score == bestScore) {
			best.add(eg);
		}
	}

	@Override
	public String toString() {
		return "Best:" + best + ":" + bestScore;
	}
}
