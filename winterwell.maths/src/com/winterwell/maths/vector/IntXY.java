package com.winterwell.maths.vector;

/**
 * X-Y integer coordinates. For convenience and some efficiency in the popular
 * 2-dimensional integer case.
 * 
 * @author Daniel
 * 
 */
public final class IntXY {

	// Exposed for convenience
	public final int x;
	public final int y;

	public IntXY(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntXY other = (IntXY) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

}
