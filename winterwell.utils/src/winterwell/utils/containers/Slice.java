package winterwell.utils.containers;

import org.w3c.dom.ls.LSLoadEvent;

/**
 * A slice or region of a String
 * 
 * WARNING: these do NOT serialise well using XStream! Suppose you have a large
 * doc with lots of slices... the output will repeat the base doc for every
 * slice, creating a giant file.
 * 
 * @see LSlice
 * @testedby SliceTest
 * @author daniel
 */
public class Slice implements CharSequence {

	private final CharSequence base;

	public final int end;

	public final int start;

	public Slice(CharSequence base) {
		this(base, 0, base.length());
	}

	/**
	 * 
	 * @param base
	 * @param start
	 *            inclusive
	 */
	public Slice(CharSequence base, int start) {
		this(base, start, base.length());
	}

	/**
	 * 
	 * @param base
	 * @param start
	 *            inclusive
	 * @param end
	 *            exclusive
	 */
	public Slice(CharSequence base, int start, int end) {
		// base instanceof Slice, then re-calc on the base String
		assert base != null;
		if (base instanceof Slice) {
			Slice bs = (Slice) base;
			base = bs.base;
			start += bs.start;
			end += bs.start;
		}
		assert !(base instanceof Slice) : base;
		assert start > -1 && end >= start : start + " " + end + " " + base;
		assert end <= base.length() : base.length() + " " + start + " " + end;
		this.base = base;
		this.start = start;
		this.end = end;
	}

	@Override
	public char charAt(int index) {
		return base.charAt(index + start);
	}

	public boolean contains(Slice other) {
		assert other.base.equals(base) : this + " vs " + other;
		return start <= other.start && end >= other.end;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Slice other = (Slice) obj;
		if (!base.equals(other.base))
			return false;
		if (end != other.end)
			return false;
		if (start != other.start)
			return false;
		return true;
	}

	public CharSequence getBase() {
		return base;
	}

	public final int getEnd() {
		return end;
	}

	public final int getStart() {
		return start;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = prime + base.hashCode();
		result = prime * result + end;
		result = prime * result + start;
		return result;
	}

	@Override
	public final int length() {
		return end - start;
	}

	/**
	 * @param slice2
	 * @return the overlap if there is any overlap, or null.
	 * 
	 *         Note: zero-length slices get special dispensation to overlap
	 *         without any actual overlap.
	 */
	public Slice overlap(Slice slice2) {
		assert slice2.base.equals(base) : this + " vs " + slice2;
		int s = Math.max(start, slice2.start);
		int e = Math.min(end, slice2.end);
		if (e > s)
			return new Slice(base, s, e);
		if (e == s) {
			if (length() == 0 || slice2.length() == 0)
				return new Slice(base, s, e);
		}
		return null;
	}

	public boolean startsWith(CharSequence w) {
		if (w.length() > length())
			return false;
		for (int i = 0, n = w.length(); i < n; i++) {
			if (charAt(i) != w.charAt(i))
				return false;
		}
		return true;
	}

	@Override
	public final CharSequence subSequence(int s, int e) {
		assert s >= 0 && e >= s && start + e <= end : s + " " + e + " \""
				+ this + '"';
		return base.subSequence(start + s, start + e);
	}

	/**
	 * The slice of string
	 */
	@Override
	public final String toString() {
		return base.subSequence(start, end).toString();
	}
}
