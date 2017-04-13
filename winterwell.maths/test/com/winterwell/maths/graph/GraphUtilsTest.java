package com.winterwell.maths.graph;

import org.junit.Test;

import com.winterwell.maths.datastorage.Index;

import no.uib.cipr.matrix.Matrix;

public class GraphUtilsTest {

	@Test
	public void testGetAdjacencyMatrix() {
		UnDiGraph<String> graph = new UnDiGraph<String>();
		UnDiNode<String> a = graph.addNode("A");
		UnDiNode<String> b = graph.addNode("B");
		graph.addEdge(a, b, null);
		assert a.edges.size() == 1;
		assert b.edges.size() == 1;
		graph.addEdge(b, b, null);
		assert a.edges.size() == 1;
		assert b.edges.size() == 2;
		Index<UnDiNode<String>> index = new Index<UnDiNode<String>>();
		int ai = index.add(a);
		int bi = index.add(b);
		assert ai == 0 && bi == 1;
		assert index.size() == 2;
		Matrix adj = GraphUtils.getAdjacencyMatrix(graph, index);

		assert adj.numRows() == 2 && adj.numColumns() == 2;
		assert adj.get(0, 0) == 0 : adj;
		assert adj.get(0, 1) == 1;
		assert adj.get(1, 0) == 1;
		assert adj.get(1, 1) == 1;
	}

}
