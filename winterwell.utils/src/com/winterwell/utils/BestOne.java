package com.winterwell.utils;

/**
 * Simple utility class to find the highest-scoring item.
 * 
 * NB: The update step favours the incumbent -- i.e. you have to score higher to
 * replace them; equal isn't enough.
 * 
 * @see Best which keeps a top list
 * @author daniel
 */
public final class BestOne<T> {

	private T best;
	private double bestScore = Double.NEGATIVE_INFINITY;

	public BestOne() {
		//
	}

	/**
	 * @return the current best. The first such element in the event of a tie.
	 *         null if no elements have been shown
	 */
	public T getBest() {
		return best;
	}

	public double getBestScore() {
		return bestScore;
	}

	/**
	 * Set if this score is higher than the previous best
	 * 
	 * @param score
	 */
	public void maybeSet(T eg, double score) {
		if (score > bestScore || best == null) {
			best = eg;
			bestScore = score;
		}
	}

	@Override
	public String toString() {
		return "Best:" + best + ":" + bestScore;
	}
}
