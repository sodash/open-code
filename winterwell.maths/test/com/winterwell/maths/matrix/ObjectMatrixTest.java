/**
 * 
 */
package com.winterwell.maths.matrix;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Containers.Changes;
import com.winterwell.utils.containers.Pair2;

import junit.framework.TestCase;

/**
 * @author miles
 * 
 */
public class ObjectMatrixTest extends TestCase {

	// TODO: copied-and-pasted from DBTestCase, because I don't want this class
	// to depend on it.
	// Find somewhere better to stick this routine.
	protected <X> void assertSetEquals(String message, Set<X> expected,
			Set<X> actual) {
		Changes<X> diffs = Containers.differences(actual, expected);
		assertTrue(
				message + ", unexpected elements found: " + diffs.getAdded(),
				diffs.getAdded().isEmpty());
		assertTrue(
				message + ", expected elements not found" + diffs.getDeleted(),
				diffs.getDeleted().isEmpty());
	}

	private ObjectMatrix<Integer, Integer> getTestMatrix() {
		return objectMatrixFromArray(getTestMatrixContents());
	}

	private double[][] getTestMatrixContents() {
		double[][] contents = { { 1.0, 0.0, 0.0, 0.0 }, { 0.0, 2.0, 1.0, 0.0 },
				{ 3.0, 0.0, 5.0, 0.0 }, { 0.0, 0.0, 6.0, 0.0 } };
		return contents;
	}

	private ObjectMatrix<Integer, Integer> objectMatrixFromArray(
			double[][] contents) {
		ObjectMatrix<Integer, Integer> matrix = new ObjectMatrix<Integer, Integer>();
		for (int i = 0; i < contents.length; i++) {
			for (int j = 0; j < contents[i].length; j++) {
				matrix.set(i, j, contents[i][j]);
			}
		}
		return matrix;
	}

	public void testEntries() {
		ObjectMatrix<Integer, Integer> matrix = getTestMatrix();
		double[][] contents = getTestMatrixContents();
		HashSet<Pair2<Integer, Integer>> seen = new HashSet<Pair2<Integer, Integer>>();
		for (Pair2<Integer, Integer> key : matrix.entries()) {
			seen.add(key);
			int row = key.first;
			int col = key.second;
			assertEquals(contents[row][col], matrix.get(row, col));
		}
		HashSet<Pair2<Integer, Integer>> expected = new HashSet<Pair2<Integer, Integer>>();
		for (int i = 0; i < contents.length; i++) {
			for (int j = 0; j < contents[i].length; j++) {
				if (contents[i][j] != 0.0) {
					expected.add(new Pair2<Integer, Integer>(i, j));
				}
			}
		}
		assertSetEquals("Keys of entries", expected, seen);
	}

	public void testGet() {
		ObjectMatrix<Integer, Integer> matrix = getTestMatrix();
		assertEquals(1.0, matrix.get(0, 0));
		assertEquals(0.0, matrix.get(0, 1));
		assertEquals(0.0, matrix.get(0, 2));
		assertEquals(0.0, matrix.get(0, 3));
		assertEquals(0.0, matrix.get(1, 0));
		assertEquals(2.0, matrix.get(1, 1));
		assertEquals(1.0, matrix.get(1, 2));
		assertEquals(0.0, matrix.get(1, 3));
		assertEquals(3.0, matrix.get(2, 0));
		assertEquals(0.0, matrix.get(2, 1));
		assertEquals(5.0, matrix.get(2, 2));
		assertEquals(0.0, matrix.get(2, 3));
		assertEquals(0.0, matrix.get(3, 0));
		assertEquals(0.0, matrix.get(3, 1));
		assertEquals(6.0, matrix.get(3, 2));
		assertEquals(0.0, matrix.get(3, 3));
	}

	public void testGetRow() {
		// third column of test data is null, so not included in the rows
		// returned by getRow().
		ObjectMatrix<Integer, Integer> matrix = getTestMatrix();
		Map<Integer, Double> row0 = matrix.getRow(0);
		Map<Integer, Double> row1 = matrix.getRow(1);
		Map<Integer, Double> row2 = matrix.getRow(2);
		Map<Integer, Double> row3 = matrix.getRow(3);
		assertEquals(1.0, row0.get(0));
		assertEquals(0.0, row0.get(1));
		assertEquals(0.0, row0.get(2));
		assertEquals(null, row0.get(3));
		assertEquals(0.0, row1.get(0));
		assertEquals(2.0, row1.get(1));
		assertEquals(1.0, row1.get(2));
		assertEquals(null, row1.get(3));
		assertEquals(3.0, row2.get(0));
		assertEquals(0.0, row2.get(1));
		assertEquals(5.0, row2.get(2));
		assertEquals(null, row2.get(3));
		assertEquals(0.0, row3.get(0));
		assertEquals(0.0, row3.get(1));
		assertEquals(6.0, row3.get(2));
		assertEquals(null, row3.get(3));
	}
}
