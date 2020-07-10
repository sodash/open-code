package com.winterwell.maths;

import java.util.Map;

import com.google.common.cache.CacheBuilder;

/**
 * Convenience for duplicate filtering
 * @author daniel
 *
 * @param <Key>
 */
public class NoDupes<Key> {
	@Override
	public String toString() {
		return "NoDupes["+seen.size()+"]";
	}
	
	/**
	 * TODO make this non-transient 
	 *  => saner serialisation for the google cache
	 */
	private transient Map seen;
	private int memorySize;

	public NoDupes() {
		this(10000);
	}
	
	/**
	 * 
	 * @param memorySize e.g. 10000
	 */
	public NoDupes(int memorySize) {
		this.memorySize = memorySize;
	}
	
	/**
	 * This checks *and updates* the seen set.
	 * @param x
	 * @param reaction
	 * @return true if this is a duplicate
	 */
	public boolean isDuplicate(Key e) {
		boolean oldNews = seen().containsKey(e);
		seen.put(e, true);
		return oldNews;		
	}
	
	/**
	 * Checks whether the item is in the seen set.
	 * Does not update anything.
	 * @param item
	 */
	public boolean containsKey(Key item) {
		return seen().containsKey(item);
	}

	private Map seen() {
		if (seen==null) {
			// TODO if we used a BloomFilter we could fit more in		
			seen = (Map) CacheBuilder.newBuilder().maximumSize(memorySize).build().asMap();
		}
		return seen;
	}
	
}
