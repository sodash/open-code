package com.winterwell.utils.containers;

import java.util.HashSet;

/**
 * HashSet backed variant of {@link ListMap}
 * @author daniel
 *
 * @param <K>
 * @param <V>
 */
public final class SetMap<K,V> extends CollectionMap<K, V, HashSet<V>>{
	private static final long serialVersionUID = 1L;

	@Override
	HashSet<V> newList(int sizeHint) {
		return new HashSet(sizeHint);
	}

}
