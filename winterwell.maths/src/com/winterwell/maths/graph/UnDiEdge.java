package com.winterwell.maths.graph;

/**
 * 
 * @author Daniel
 * 
 * @param <X>
 *            node-type
 */
public class UnDiEdge<X> extends AEdge<UnDiNode<X>> implements
		IEdge.Undirected<UnDiNode<X>> {

	public UnDiEdge(UnDiNode<X> start, UnDiNode<X> end) {
		super(start, end);
		start.edges.add(this);
		end.edges.add(this);
	}

}
