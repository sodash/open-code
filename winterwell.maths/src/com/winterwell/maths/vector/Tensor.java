package com.winterwell.maths.vector;

import com.winterwell.utils.Printer;
import com.winterwell.utils.TodoException;

/**
 * Tensors are a generalisation of vectors and matrices.
 * 
 * This has no real purpose except for testing interview candidates. This is a
 * model answer. There are other solutions (recursive, hash-map).
 * 
 * The *tensor-order* (aka *rank* or *dimension*) is the number of indices for
 * the Tensor. E.g. a number has tensor-order 0, a vector has tensor-order 1,
 * and a matrix has tensor-order 2
 */
public class Tensor {

	private double[] data; // Tensor values
	private int[] dimSizes; // Dimension lengths

	/**
	 * Special case constructor for making a Tensor that is really a matrix
	 * 
	 * @param matrix
	 */
	public Tensor(double[][] matrix) {
		// TODO
		throw new TodoException();
	}

	public Tensor(int[] dimSizes) {
		this.dimSizes = dimSizes;
		int dimMult = 1;
		for (int i = 0; i < dimSizes.length; i++) {
			dimMult *= dimSizes[i];
		}
		data = new double[dimMult];
	}

	/**
	 * The Tensor product. This accumulates indices. The new tensor has
	 * tensor-order = the tensor-order of the two combined tensors added
	 * together. E.g. A_ij combined with B_k gives C_ijk where C_ijk = A_ij .
	 * B_k
	 * 
	 * @param b
	 * @return a new Tensor with tensor-order greater than this.
	 */
	Tensor combine(Tensor b) {
		// TODO
		throw new TodoException();
	}

	/**
	 * Create a new Tensor by *contracting* this one. That is, sum over two
	 * indices. The result is a new Tensor with tensor-order 2 less than this.
	 * 
	 * E.g. Suppose this is a matrix A, then this.contract(0, 1) would give the
	 * trace \sum_i A_ii, i.e. the sum of the diagonal, which is a number.
	 * 
	 * @return a new contracted Tensor
	 */
	Tensor contract(int indexA, int indexB) {
		// TODO
		throw new TodoException();
	}

	/**
	 * Get a value in the Tensor TODO change the method signature
	 */
	public double get(int[] coordinate) {
		if (coordinate.length != dimSizes.length)
			throw new IllegalArgumentException();
		int value = 0;
		int dimMult = 1;
		for (int i = 0; i < dimSizes.length; i++) {
			if (dimSizes[i] <= coordinate[i])
				throw new ArrayIndexOutOfBoundsException(
						Printer.toString(coordinate));
			value += coordinate[i] * dimMult;
			dimMult *= dimSizes[i];
		}
		return data[value];
	}

	/**
	 * For the special case where both this and the other Tensor are matrices.
	 * Perform normal matrix multiplication: C_ik = \sum j A_ij . B_jk Clue:
	 * There is a one line answer to this.
	 * 
	 * @param b
	 * @return the matrix this.b
	 */
	Tensor matrixMultiply(Tensor b) {
		// TODO
		throw new TodoException();
	}

	/**
	 * Set a value in the Tensor TODO change the method signature
	 */
	public void set() {
		// TODO
		throw new TodoException();
	}
}
