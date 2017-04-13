package com.winterwell.maths.stats.distributions.discrete;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.winterwell.maths.datastorage.IIndex;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;

/**
 * Assigns the same probability to everything.
 * @author daniel
 *
 * @param <X>
 */
public final class Uniform<X> extends AFiniteDistribution<X> 
{	

	@Override
	public String toString() {
		return "Uniform[1/"+size()+"]";
	}
	
	/**
	 * @return base set of words. Can be null.
	 */
	public Iterable<X> getBase() {
		return base;
	}
	
	@Override
	public X sample() {
		if (base==null) throw new UnsupportedOperationException();
		
		// special case for a common case
		if (base instanceof IIndex) {
			IIndex<X> index = (IIndex) base;
			int n = index.size();
			// never loop forever
			for(int i=0; i<1000; i++) {
				int r = random().nextInt(n);
				X item = index.get(r);
				if (item != null) {
					return item;
				}
			}
		}
		
		// potentially slow!
		List<X> items = Containers.getList(base);
		return Utils.getRandomMember(items);
		
	}
	
	@Override
	public void normalise() throws UnsupportedOperationException {
		return;
	}
	
	
	final int size;
	final Iterable<X> base;
	
	public Uniform(Iterable<X> elements) {
		base = elements;
		normalised = true;
		size = -1;
	}
	
	/**
	 * P(x) = 1/size
	 * @param size
	 */
	public Uniform(int size) {
		this.size = size;
		assert size >= 1;
		normalised = true;
		this.base = null;
	}
	
	@Override
	public Iterator<X> iterator() throws UnsupportedOperationException {
		if (base!=null) {
			return base.iterator();
		}
		if (size==0) return Collections.EMPTY_LIST.iterator();
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		if (base==null) return size;
		// special case for a common case
		if (base instanceof IIndex) {
			return ((IIndex) base).size();
		}
		return Containers.size(base);
	}

	/**
	 * @param x If we can test for membership (e.g. if the Uniform was
	 * constructed using an IIndex or Collection), then this will return 0
	 * for non-members.
	 * 
	 * @return 1 / size, or 0 
	 */
	@Override
	public double prob(X x) {
		// not a member?
		if (base != null) {
			if (base instanceof IIndex) {
				if ( ! ((IIndex) base).contains(x)) return 0;
			} else if (base instanceof Collection) {
				if ( ! ((Collection) base).contains(x)) return 0;
			} 
		}
		
		return 1.0/size();
	}

}
