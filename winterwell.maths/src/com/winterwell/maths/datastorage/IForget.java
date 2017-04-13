package com.winterwell.maths.datastorage;

public interface IForget<K, V> {

	public abstract void setTrackPrunedValue(boolean track);

	public abstract void addListener(IPruneListener<K,V> listener);

	public abstract int getPrunedCount();

	/**
	 * @return The sum of all pruned values, if tracked. NaN by default.
	 * @see #setTrackPrunedValue(boolean)
	 */
	public abstract double getPrunedValue();

	public abstract void removeListener(IPruneListener<K,V> listener);

}