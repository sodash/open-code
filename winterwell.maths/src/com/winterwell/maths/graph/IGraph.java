/**
 * 
 */
package com.winterwell.maths.graph;

import java.util.Collection;

/**
 * Interface for graphs.
 * 
 * <h3>Directed vs Undirected Graphs</h3> This interface applies to both
 * directed and undirected graphs. The documentation tends to talk in terms of directed
 * graphs. For <i>directed</i> graphs, "from node-a to node-b" should be read as
 * excluding any edge from node-b to node-a. For un-directed graphs,
 * "from node-a to node-b" should be read as "linking node-a and node-b".
 * 
 * @author Daniel
 */
public interface IGraph<N> {

	public static interface Directed<N> extends IGraph<N> {

		/**
		 * Add a directed edge from start to end. Behaviour if an edge already
		 * exists depends on the graph type.
		 * @param params Can be null.
		 */
		@Override
		IEdge.Directed addEdge(N start, N end, Object params);

		@Override
		IEdge.Directed getEdge(N start, N end);

		@Override
		Collection<? extends IEdge.Directed> getEdges(N node);

		Collection<? extends IEdge.Directed> getEdgesTo(N node);
	}

	public static interface Undirected<N> extends IGraph<N> {

	}

	IEdge addEdge(N start, N end, Object params);

	/**
	 * Add a node to the graph.
	 * 
	 * @param params
	 *            Arbitrary info to be passed to the node constructor. Can be
	 *            null.
	 * @return node
	 */
	N addNode(Object params);

	/**
	 * @param start
	 * @param end
	 * @return Edge from start to end, or null if there is none.
	 */
	IEdge getEdge(N start, N end);

	/**
	 * @param node
	 * @return edges <i>from</i> the node in a directed graph (all edges
	 *         involving the node in an undirected graph).
	 */
	Collection<? extends IEdge> getEdges(N node);

	/**
	 * Nodes reachable from this node in one step.
	 * 
	 * @param node
	 */
	Collection<? extends N> getNeighbours(N node);

	/**
	 * @return All the nodes in this graph. Do not edit.
	 */
	Collection<N> getNodes();

	void removeEdge(IEdge edge);

	void removeNode(N node);
}
