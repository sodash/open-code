package com.winterwell.maths.graph;

import java.util.Collection;

import com.winterwell.utils.containers.ArraySet;

/**
 * A node in a directed graph.
 * 
 * @param <X>
 * @testedby {@link DiNodeTest}
 * @author daniel
 */
public class DiNode<X> implements IHasValue<X> {

	final Collection<DiEdge<X>> edgesFrom = new ArraySet<DiEdge<X>>();
	final Collection<DiEdge<X>> edgesTo = new ArraySet<DiEdge<X>>();
	private X value;

	/**
	 * Created via {@link DiGraph#addNode(Object)}
	 * @param x
	 *            Value held at this node. Can be null
	 */
	protected DiNode(X x) {
		this.value = x;
	}

	public X getValue() {
		return value;
	}

	public void setValue(X value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value == null ? super.toString() : "DiNode[" + value + "]";
	}
}
