package com.winterwell.maths.graph;

import java.util.Set;

import com.winterwell.utils.containers.ArraySet;

/**
 * Base class for labelled edges.
 * 
 * @author daniel
 * 
 * @param <N>
 */
public class AEdge<N> implements IEdge<N>, IHasValue {

	protected final N end;

	protected final N start;

	Object value;

	public AEdge(N start, N end) {
		this.start = start;
		this.end = end;
		assert start!=null && end!=null : start+"-"+end;
	}

	@Override
	public Set<N> getEndPoints() {
		return new ArraySet<N>(start, end);
	}

	@Override
	public N getOtherEnd(N n) {
		assert n == start || n == end;
		if (n == start)
			return end;
		return start;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public boolean isAnEnd(N n) {
		return n == start || n == end;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + start + " - " + end + "]";
	}

}