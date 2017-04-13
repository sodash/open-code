package com.winterwell.maths.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;

/**
 * Default directed graph implementation.
 * <p>
 * Uses a list of nodes, with the node objects carrying all the connection info.
 * 
 * @author Daniel
 * 
 * @param <N>
 *            values stored at nodes
 * @testedby {@link DiGraphTest}
 */
public class DiGraph<N> implements IGraph.Directed<DiNode<N>> {

	@Override
	public String toString() {
		if (nodes.size() > 12) {
			return getClass().getSimpleName()+"["+nodes.size()+" nodes]";	
		}
		DotPrinter dp = new DotPrinter(this);
		return getClass().getSimpleName()+"["+dp.out()+"]";
	}
	
	private List<DiNode<N>> nodes = new ArrayList<DiNode<N>>();

	/**
	 * Add a directed edge from start to end. 
	 * If an edge already exists -- creates another one anyway!
		@param edgeValue Value set on the new edge. Can be null.
	 */
	@Override
	public DiEdge<N> addEdge(DiNode<N> start, DiNode<N> end, Object edgeValue) {
		assert nodes.contains(start);
		assert nodes.contains(end);
		DiEdge<N> edge = new DiEdge<N>(start, end);
		edge.setValue(edgeValue);
		return edge;
	}

	/**
	 * Add a node to the graph. This will always add a new node -- it can create duplicates!
	 */
	@Override
	public DiNode<N> addNode(Object x) {
		DiNode<N> node = new DiNode<N>((N) x);
		nodes.add(node);
		return node;
	}
	
	/**
	 * @param nodeValue
	 * @return the first node with nodeValue, or null
	 */
	public DiNode<N> getNode(N nodeValue) {
		assert nodeValue != null;
		for(DiNode<N> node : nodes) {
			if (Utils.equals(nodeValue, node.getValue())) return node;
		}
		return null;
	}

	@Override
	public DiEdge getEdge(DiNode<N> start, DiNode<N> end) {
		assert nodes.contains(start) : start+" -> "+end;
		assert nodes.contains(end) : start+" -> "+end;
		for (DiEdge<N> edge : start.edgesFrom) {
			if (edge.getEnd() == end)
				return edge;
		}
		return null;
	}

	@Override
	public Collection<DiEdge<N>> getEdges(DiNode<N> node) {
		assert nodes.contains(node);
		return node.edgesFrom;
	}

	@Override
	public Collection<DiEdge<N>> getEdgesTo(DiNode<N> node) {
		assert nodes.contains(node);
		return node.edgesTo;
	}

	@Override
	public Collection<DiNode<N>> getNeighbours(DiNode<N> n) {
		DiNode<N> node = n;
		assert nodes.contains(node);
		Collection<DiNode<N>> ns = new ArrayList<DiNode<N>>(
				node.edgesFrom.size());
		for (DiEdge e : node.edgesFrom) {
			ns.add(e.getEnd());
		}
		return ns;
	}

	@Override
	public Collection<DiNode<N>> getNodes() {
		return nodes;
	}

	@Override
	public void removeEdge(IEdge e) {
		DiEdge<N> edge = (DiEdge) e;
		edge.start.edgesFrom.remove(e);
		edge.end.edgesTo.remove(e);
	}

	@Override
	public void removeNode(final DiNode<N> node) {
		assert nodes.contains(node);
		nodes.remove(node);
		// clean up links
		for (DiEdge e : node.edgesTo) {
			if (e.getStart() == node) {
				continue;
			}
			e.getStart().edgesFrom.remove(e);
		}
		for (DiEdge e : node.edgesFrom) {
			if (e.getEnd() == node) {
				continue;
			}
			e.getEnd().edgesTo.remove(e);
		}
	}

	/**
	 * @return a view on this graph where the nodes are of type N
	 * (rather than of type DiNode<N>).
	 * Add & remove methods write-through to the underlying graph.
	 * <p>
	 * This is only valid if all the nodes have values!
	 */
	public IGraph.Directed<N> getValueAdaptor() {
		final DiGraph<N> g = this;
		return new IGraph.Directed<N>() {

			@Override
			public String toString() {
				if (nodes.size() > 12) {
					return "DiGraph(values)["+nodes.size()+" nodes]";	
				}
				DotPrinter dp = new DotPrinter(this);
				return "DiGraph(values)["+dp.out()+"]";
			}
			
			@Override
			public N addNode(Object x) {
				DiNode<N> dn = g.addNode(x);
				return dn.getValue();
			}

			@Override
			public Collection<? extends N> getNeighbours(N node) {
				DiNode<N> dn = g.getNode(node);
				Collection<DiNode<N>> ns = g.getNeighbours(dn);
				return extract(ns);
			}

			private Collection<N> extract(Collection<? extends IHasValue<N>> ns) {
				return Containers.apply(ns, IHasValue.EXTRACT);
			}

			@Override
			public Collection<N> getNodes() {
				return extract(g.getNodes());
			}

			@Override
			public void removeEdge(IEdge edge) {
				g.removeEdge(edge);
			}

			@Override
			public void removeNode(N node) {
				DiNode<N> dn = g.getNode(node);
				if (dn==null) return;
				g.removeNode(dn);
			}

			@Override
			public com.winterwell.maths.graph.IEdge.Directed addEdge(N start,
					N end, Object edgeValue) {
				DiNode<N> s = g.getNode(start);
				DiNode<N> e = g.getNode(end);
				DiEdge<N> de = g.addEdge(s, e, edgeValue);
				return valueEdge(de);
			}

			@Override
			public com.winterwell.maths.graph.IEdge.Directed getEdge(N start, N end) {
				DiNode<N> s = g.getNode(start);
				DiNode<N> e = g.getNode(end);
				DiEdge edge = g.getEdge(s, e);
				return valueEdge(edge);
			}

			private IEdge.Directed valueEdge(DiEdge edge) {
				return new ValueEdge<N>(edge);
			}

			@Override
			public Collection<IEdge.Directed> getEdges(
					N node) {
				DiNode<N> dn = g.getNode(node);
				Collection<DiEdge<N>> des = g.getEdges(dn);
				return valueEdges(des);
			}

			@Override
			public Collection<IEdge.Directed> getEdgesTo(
					N node) {
				DiNode<N> dn = g.getNode(node);
				Collection<DiEdge<N>> des = g.getEdgesTo(dn);
				return valueEdges(des);
			}

			private Collection<IEdge.Directed> valueEdges(Collection<DiEdge<N>> des) {
				ArrayList list = new ArrayList(des.size());
				for(DiEdge<N> de : des) {
					list.add(valueEdge(de));
				}
				return list;
			}
			
		};
	}

	/**
	 * Add all nodes from graph2 to this graph (creating wrapper DiNode objects).
	 * <p>
	 * Duplicate edges won't be created. 
	 * Edge value-objects & weights will be set *if* they don't conflict with
	 * existing values & weights. 
	 * @param graph2
	 */
	public <M extends N> void addAll(IGraph.Directed<M> graph2) {
		// add all nodes
		for(M node : graph2.getNodes()) {
			// Block nested graphs (potential for bugs > usefulness)
			assert ! (node instanceof DiNode) : node;
			DiNode<N> dn = getNode(node);
			if (dn==null) {
				dn = addNode(node);
			}
		}
		
		// add all edges
		for(M node : graph2.getNodes()) {
			DiNode<N> start = getNode(node);
			assert start != null : node;
			// edges from node
			Collection<? extends IEdge.Directed> edges = graph2.getEdges(node);
			for (IEdge.Directed edge : edges) {
				Object end2 = edge.getEnd();
				DiNode<N> end = getNode((N)end2);
				assert end != null : edge;
				DiEdge de = getEdge(start, end);
				if (de==null) {
					// not there? add a new edge
					de = addEdge(start, end, null);
				}
				// add extra info -- if it doesn't conflict!
				// ...value
				if (edge instanceof IHasValue && de.getValue()==null) {
					Object val = ((IHasValue) edge).getValue();
					de.setValue(val);
				}
				// ...weight
				if (edge instanceof IEdge.Weighted && de.getWeight()==0) {
					double w = ((IEdge.Weighted) edge).getWeight();
					de.setWeight(w);
				}
			}
		}
	}
	
}

class ValueEdge<N> extends AEdge<N>
implements IEdge.Directed<N>, IEdge.Weighted, IHasValue {
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getStart().hashCode();
		result = prime * result + getEnd().hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ValueEdge other = (ValueEdge) obj;
		if ( ! getStart().equals(other.getStart())) return false;
		if ( ! getEnd().equals(other.getEnd())) return false;
		return true;
	}

	private DiEdge<N> edge;

	public ValueEdge(DiEdge edge) {
		super((N)edge.getStart().getValue(), (N)edge.getEnd().getValue());
		this.edge = edge;
		assert edge != null;
	}

	@Override
	public N getEnd() {
		return end;
	}

	@Override
	public N getStart() {
		return start;
	}

	@Override
	public Object getValue() {
		return edge.getValue();
	}

	@Override
	public double getWeight() {
		return edge.getWeight();
	}
	
}
