package com.winterwell.maths.graph;

import java.util.Arrays;
import java.util.Collection;

import junit.framework.TestCase;

public class DiGraphTest extends TestCase {

	public void testGetEdge() {
		DiGraph<String> g = new DiGraph<String>();
		DiNode<String> a = g.addNode("A");
		DiNode<String> b = g.addNode("B");
		DiNode<String> c = g.addNode("C");
		DiNode<String> d = g.addNode("D");
		DiEdge<String> ab = g.addEdge(a, b, null);
		DiEdge<String> bc = g.addEdge(b, c, null);
		DiEdge<String> bd = g.addEdge(b, d, null);

		assert g.getEdge(a, b) == ab;
		assert g.getEdge(a, a) == null;
		assert g.getEdge(b, a) == null;
		assert g.getEdge(b, c) == bc;
	}

	public void testGetNeighbours() {
		DiGraph<String> g = new DiGraph<String>();
		DiNode<String> a = g.addNode("A");
		DiNode<String> b = g.addNode("B");
		DiNode<String> c = g.addNode("C");
		DiNode<String> d = g.addNode("D");
		DiEdge<String> ab = g.addEdge(a, b, null);
		DiEdge<String> bc = g.addEdge(b, c, null);
		DiEdge<String> bd = g.addEdge(b, d, null);

		assert g.getNeighbours(a).equals(Arrays.asList(b)) : g.getNeighbours(a);
		assert g.getNeighbours(b).equals(Arrays.asList(c, d)) : g
				.getNeighbours(b);
	}

	public void testRemoveEdge() {
		DiGraph<String> g = new DiGraph<String>();
		DiNode<String> a = g.addNode("A");
		DiNode<String> b = g.addNode("B");
		DiNode<String> c = g.addNode("C");
		DiNode<String> d = g.addNode("D");
		DiEdge<String> ab = g.addEdge(a, b, null);
		DiEdge<String> bc = g.addEdge(b, c, null);
		DiEdge<String> bd = g.addEdge(b, d, null);

		DiEdge<String> bc2 = g.getEdge(b, c);
		assert bc2 == bc;
		assert b.edgesFrom.contains(bc);
		assert c.edgesTo.contains(bc);

		g.removeEdge(bc);

		bc2 = g.getEdge(b, c);
		assert bc2 == null;
		assert !b.edgesFrom.contains(bc);
		assert !c.edgesTo.contains(bc);
	}

	public void testRemoveNode() {
		DiGraph<String> g = new DiGraph<String>();
		DiNode<String> a = g.addNode("A");
		DiNode<String> b = g.addNode("B");
		DiNode<String> c = g.addNode("C");
		DiNode<String> d = g.addNode("D");
		DiEdge<String> ab = g.addEdge(a, b, null);
		DiEdge<String> bc = g.addEdge(b, c, null);
		DiEdge<String> bd = g.addEdge(b, d, null);

		g.removeNode(b);

		assert g.getNodes().size() == 3;
		assert g.getNodes().equals(Arrays.asList(a, c, d)) : g.getNodes();

		Collection<DiEdge<String>> es = g.getEdges(a);
		assert es.size() == 0;
		es = g.getEdges(c);
		assert es.size() == 0;
		es = g.getEdges(d);
		assert es.size() == 0;
	}

}
