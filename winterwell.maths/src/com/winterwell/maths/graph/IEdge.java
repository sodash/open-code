package com.winterwell.maths.graph;

import java.util.Set;

/**
 * 
 * @author Daniel
 * 
 * @param <N>
 *            node type
 */
public interface IEdge<N> {

	public interface Weighted {
		double getWeight();
	}

	public static interface Directed<N> extends Undirected<N> {

		N getEnd();

		N getStart();
	}

	public static interface Undirected<N> extends IEdge<N> {

	}

	Set<? extends N> getEndPoints();

	/**
	 * @param n
	 *            Must be an end of this edge
	 * @return the other end of the edge, or null if this edge is a loop (i.e.
	 *         from n to n).
	 */
	N getOtherEnd(N n);

	/**
	 * @param n
	 * @return true if n is the start or end of this edge.
	 */
	boolean isAnEnd(N n);
}
