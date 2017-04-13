package com.winterwell.maths.graph;

/**
 * An edge in a directed graph
 * 
 * @testedby {@link DiEdgeTest}
 * @author daniel
 */
public class DiEdge<X> extends AEdge<DiNode<X>> implements
		IEdge.Directed<DiNode<X>>, IEdge.Weighted {

	private double weight = 1;

	protected DiEdge(DiNode<X> start, DiNode<X> end) {
		super(start, end);
		assert start != null;
		assert end != null;
		start.edgesFrom.add(this);
		end.edgesTo.add(this);
	}

	@Override
	public DiNode<X> getEnd() {
		return end;
	}

	@Override
	public DiNode<X> getStart() {
		return start;
	}

	public void setWeight(double wi) {
		this.weight = wi;
	}
	
	/**
	 * Defaults to 1
	 */
	public double getWeight() {
		return weight;
	}

}
