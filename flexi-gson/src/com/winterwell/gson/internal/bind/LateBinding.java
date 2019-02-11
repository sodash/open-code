package com.winterwell.gson.internal.bind;

/**
 * For JSOG references, where the object hasn't finished being made yet.
 * @author daniel
 *
 */
public final class LateBinding {

	public final String ref;

	public LateBinding(String ref) {
		this.ref = ref;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ref == null) ? 0 : ref.hashCode());
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
		LateBinding other = (LateBinding) obj;
		if (ref == null) {
			if (other.ref != null)
				return false;
		} else if (!ref.equals(other.ref))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "LateBinding [ref=" + ref + "]";
	}

	
}
