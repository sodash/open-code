package com.winterwell.maths.datastorage;

import java.util.List;
import java.util.Map;

/**
 * For listening to {@link HalfLifeIndex} prune events.
 * 
 * @author daniel
 */
public interface IPruneListener<K, V> {

	/**
	 * Called just after a pruning has occurred.
	 * 
	 * @param pruned
	 */
	void pruneEvent(List<Map.Entry<K,V>> pruned);

}
