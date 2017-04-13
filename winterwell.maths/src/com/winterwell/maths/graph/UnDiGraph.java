package com.winterwell.maths.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.winterwell.utils.containers.ArraySet;

public class UnDiGraph<X> implements IGraph.Undirected<UnDiNode<X>> {

	private List<UnDiNode<X>> nodes = new ArrayList<UnDiNode<X>>();

	@Override
	public UnDiEdge<X> addEdge(UnDiNode<X> start, UnDiNode<X> end,
			Object ignored) {
		assert nodes.contains(start);
		assert nodes.contains(end);
		UnDiEdge<X> edge = new UnDiEdge<X>(start, end);
		return edge;
	}

	@Override
	public UnDiNode<X> addNode(Object x) {
		UnDiNode<X> node = new UnDiNode<X>((X) x);
		nodes.add(node);
		return node;
	}

	@Override
	public UnDiEdge getEdge(UnDiNode<X> start, UnDiNode<X> end) {
		assert nodes.contains(start);
		assert nodes.contains(end);
		for (UnDiEdge<X> edge : start.edges) {
			if (!edge.isAnEnd(end)) {
				continue;
			}
			// guard against A - A loops
			if (start == end && (edge.start != start || edge.end != start)) {
				continue;
			}
			return edge;
		}
		return null;
	}

	@Override
	public Collection<UnDiEdge<X>> getEdges(UnDiNode<X> node) {
		assert nodes.contains(node);
		return node.edges;
	}

	@Override
	public Collection<UnDiNode<X>> getNeighbours(UnDiNode<X> n) {
		UnDiNode<X> node = n;
		assert nodes.contains(node);
		Collection<UnDiNode<X>> ns = new ArraySet<UnDiNode<X>>(
				node.edges.size());
		for (UnDiEdge<X> e : node.edges) {
			ns.add(e.getOtherEnd(n));
		}
		return ns;
	}

	@Override
	public Collection<UnDiNode<X>> getNodes() {
		return Collections.unmodifiableList(nodes);
	}

	@Override
	public void removeEdge(IEdge e) {
		UnDiEdge<X> edge = (UnDiEdge<X>) e;
		for (UnDiNode n : edge.getEndPoints()) {
			n.edges.remove(e);
		}
	}

	@Override
	public void removeNode(UnDiNode<X> n) {
		UnDiNode<X> node = n;
		assert nodes.contains(node);
		nodes.remove(node);
		// clean up links
		for (UnDiEdge<X> e : node.edges) {
			UnDiNode n2 = e.getOtherEnd(n);
			if (n2 == null) {
				continue;
			}
			n2.edges.remove(e);
		}
	}

}
