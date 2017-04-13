package com.winterwell.maths.graph.flow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.datastorage.Index;
import com.winterwell.maths.graph.GraphUtils;
import com.winterwell.maths.graph.IGraph;
import com.winterwell.maths.matrix.Eigenpair;
import com.winterwell.maths.matrix.IEigenVectorFinder;
import com.winterwell.maths.matrix.MOP;

import no.uib.cipr.matrix.Matrix;

/**
 * What are the influential nodes in a graph? Take the first eigenvector of the
 * adjaceny matrix as a measure of node influence.
 * 
 * @author Daniel
 */
public class EigenFlow {

	IEigenVectorFinder mop = new MOP();

	public EigenFlow() {
	}

	public <N> Map<N, Double> run(IGraph<N> graph) {
		Index<N> index = new Index<N>(graph.getNodes());
		assert index.size() != 0;
		Matrix adj = GraphUtils.getAdjacencyMatrix(graph, index);

		List<Eigenpair> eps = mop.getEigenpairs(adj);
		Eigenpair ep = eps.get(0);

		HashMap<N, Double> map = new HashMap<N, Double>(index.size());
		for (N node : index) {
			double val = ep.get(index.indexOf(node));
			map.put(node, val);
		}
		return map;
	}
}
