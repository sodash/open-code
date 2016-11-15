package winterwell.utils.containers;

import java.util.Iterator;

/**
 * An integer range. Can be iterated over, e.g.
 * <code>for(int i : new IntRange(10,20)) ...</code>
 * 
 * @see Range
 * @author Daniel
 * 
 */
public class IntRange
// extends AbstractList<Integer>
		implements Iterable<Integer> {

	@Override
	public Iterator<Integer> iterator() {
		return new Iterator<Integer>() {
			int i = low;

			@Override
			public boolean hasNext() {
				return i <= high;
			}

			@Override
			public Integer next() {
				return i++;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * @param slice2
	 * @return the overlap if there is any overlap, or null.
	 * 
	 *         Note: zero-length slices get special dispensation to overlap
	 *         without any actual overlap.
	 */
	public IntRange overlap(IntRange slice2) {
		int s = Math.max(low, slice2.low);
		int e = Math.min(high, slice2.high);
		if (e > s)
			return new IntRange(s, e);
		if (e == s) {
			if (size() == 0 || slice2.size() == 0)
				return new IntRange(s, e);
		}
		return null;
	}

	public final int high;

	public final int low;

	/**
	 * Create an IntRange [low, high] - inclusive of end points
	 * 
	 * @param a
	 * @param b
	 *            The order of a,b is not important
	 */
	public IntRange(int a, int b) {
		if (a < b) {
			low = a;
			high = b;
		} else {
			low = b;
			high = a;
		}
	}

	/**
	 * Is x inside this range? Includes the end values.
	 */
	public final boolean contains(int x) {
		return x >= low && x <= high;
	}
	
	/**
	 * Is x inside this range? Includes the end values.
	 */
	public final boolean contains(double x) {
		return x >= low && x <= high;
	}

	public boolean contains(IntRange other) {
		return low <= other.low && high >= other.high;
	}

	public final Integer get(int index) {
		assert contains(index + low);
		return index + low;
	}

	public final int size() {
		// includes the end points
		return high - low + 1;
	}

	@Override
	public String toString() {
		return "[" + low + ", " + high + "]";
	}

	/**
	 * Convenience method
	 * 
	 * @param x
	 * @return x, capped to fall inside range (so x if inside, high if too-high,
	 *         low if too-low)
	 */
	public final int cap(double x) {
		return Math.min(high, Math.max((int) Math.round(x), low));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + high;
		result = prime * result + low;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntRange other = (IntRange) obj;
		if (high != other.high)
			return false;
		if (low != other.low)
			return false;
		return true;
	}

	/**
	 * 
	 * @param start
	 * @param end
	 * @return e.g. (0,3) -> [0,1,2]
	 */
	public static int[] array(int start, int end) {
		int[] ints = new int[end-start];
		int cnt = 0;
		for(int i=start; i<end; i++) {
			ints[cnt] = i;
			cnt++;
		}
		return ints;
	}
	
	
}
