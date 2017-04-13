package com.winterwell.maths.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.winterwell.utils.IFilter;
import com.winterwell.utils.containers.Containers;

/**
 * A view of a larger graph.
 * 
 * TODO filter out edges as well
 * 
 * @author daniel
 * 
 * @param <N>
 *            node type
 */
public class SubGraph<N> implements IGraph<N> {

	private final IFilter<N> filter;
	private final IGraph<N> graph;
	private List<N> nodes;

	public SubGraph(IGraph<N> graph, Collection<N> myNodes) {
		assert graph != null && myNodes != null;
		this.nodes = new ArrayList(myNodes);
		this.graph = graph;
		this.filter = new IFilter<N>() {
			@Override
			public boolean accept(N x) {
				return nodes.contains(x);
			}
		};
	}

	public SubGraph(IGraph<N> graph, IFilter<N> filter) {
		this.graph = graph;
		this.filter = filter;
		assert graph != null && filter != null;
	}

	@Override
	public IEdge addEdge(N start, N end, Object params) {
		return graph.addEdge(start, end, params);
	}

	@Override
	public N addNode(Object params) {
		return graph.addNode(params);
	}

	@Override
	public IEdge getEdge(N start, N end) {
		return graph.getEdge(start, end);
	}

	@Override
	public Collection<IEdge> getEdges(N node) {
		if (!filter.accept(node))
			return Collections.emptyList();
		Collection<? extends IEdge> edges = graph.getEdges(node);
		Collection<IEdge> fes = new ArrayList<IEdge>();
		for (IEdge<N> e : edges) {
			N n2 = e.getOtherEnd(node);
			if (!filter.accept(n2)) {
				continue;
			}
			fes.add(e);
		}
		return fes;
	}

	@Override
	public Collection<? extends N> getNeighbours(N node) {
		Collection<? extends N> neighbours = graph.getNeighbours(node);
		return Containers.filter(neighbours, filter);
	}

	@Override
	public Collection<N> getNodes() {
		if (nodes != null)
			return nodes;
		Collection<N> allNodes = graph.getNodes();
		nodes = Containers.filter(allNodes, filter);
		return nodes;
	}

	@Override
	public void removeEdge(IEdge edge) {
		graph.removeEdge(edge);
	}

	@Override
	public void removeNode(N node) {
		graph.removeNode(node);
	}

}
