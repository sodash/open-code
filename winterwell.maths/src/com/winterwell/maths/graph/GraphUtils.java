package com.winterwell.maths.graph;

import com.winterwell.maths.datastorage.IIndex;
import com.winterwell.maths.matrix.SparseMatrix;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.LowerSymmDenseMatrix;
import no.uib.cipr.matrix.Matrix;

/**
 * 
 * @author Daniel
 * @testedby  GraphUtilsTest}
 */
public final class GraphUtils {

	/**
	 * 
	 * @param <N>
	 * @param graph
	 * @param index
	 * @return edge-weight with column=start, row=end. May use lower-symmetric
	 *         form for undirected graphs
	 */
	public static <N> Matrix getAdjacencyMatrix(IGraph<N> graph, IIndex<N> index) {
		assert graph != null;
		int n = index.size();
		Matrix adj;
		boolean symmetric;
		if (graph instanceof IGraph.Undirected) {
			adj = n < 100 ? new LowerSymmDenseMatrix(n)
					: new SparseMatrix(n, n);
			symmetric = true;
		} else {
			adj = n < 100 ? new DenseMatrix(n, n) : new SparseMatrix(n, n);
			symmetric = false;
		}
		// build
		for (N a : index) {
			assert a != null;
			int ai = index.indexOf(a);
			for (N b : index) {
				assert b != null;
				int bi = index.indexOf(b);
				if (symmetric && ai > bi) {
					continue; // TODO test
				}
				IEdge edge = graph.getEdge(a, b);
				if (edge == null) {
					continue;
				}
				// TODO weighted edges!
				double value = 1;
				adj.set(bi, ai, value);
			}
		}
		return adj;
	}

}
