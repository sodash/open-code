/**
 * 
 */
package com.winterwell.maths.datastorage;

import com.google.common.hash.BloomFilter;

/**
 * TODO this is just a stub sketch
 * 
 * Test whether you've already seen something.
 * Uses Guava's {@link BloomFilter}
 * 
 * Alternatives: {@link HalfLifeMap}
 * @author daniel
 *
 */
public class ForgetfulBloomFilter<T> {

	BloomFilter<T> old;
	BloomFilter<T> next;
	
	public ForgetfulBloomFilter() {
		
	}
}
