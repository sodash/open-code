package com.winterwell.maths.graph;

import java.util.Collection;

import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArraySet;

public class UnDiNode<V> implements IHasValue<V> {

	final Collection<UnDiEdge<V>> edges = new ArraySet<UnDiEdge<V>>();
	V value;

	public UnDiNode(V v) {
		this.value = v;
	}

	public V getValue() {
		return value;
	}

	public void setValue(V value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "UnDiNode[" + Printer.toString(value) + "](" + edges.size()
				+ ")";
	}

}
