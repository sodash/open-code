package com.winterwell.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * Context object for {@link Dep}. This allows having several contexts active within one JVM.
 * Contexts are hierarchical (there's a parent context) and thread-local by default -- but you can
 * pass them between threads. 
 * 
 * @author daniel
 *
 */
public final class DepContext implements Closeable {

	
	/**
	 * This allows chaining with Dep.with(). For use in try-with blocks, e.g.
	 * <code>
	 * try (DepContext context = Dep.with(MyThing.class, myThing)
	 * 								.with(OtherThing.class, otherThing)) 
	 * {
	 * 
	 * }
	 * </code>
	 * @param klass
	 * @param value
	 * @return
	 */
	public <X> DepContext with(Class<X> klass, X value) {
		Dep.set(klass, value);		
		return this;
	}
	
	volatile boolean closed;
	final DepContext parent;
	final private Object ckey;

	public DepContext(DepContext parent, Object contextKey) {
		this.ckey = contextKey;
		this.parent = parent;
	}

	@Override
	public String toString() {
		return "DepContext [closed=" + closed + ", parent=" + parent + ", ckey=" + ckey + "]";
	}

	@Override
	public void close() {
		if (parent==null) throw new IllegalStateException("Cannot close top-level context");
		this.closed = true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ckey == null) ? 0 : ckey.hashCode());
		result = prime * result + (closed ? 1231 : 1237);
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
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
		DepContext other = (DepContext) obj;
		if (ckey == null) {
			if (other.ckey != null)
				return false;
		} else if (!ckey.equals(other.ckey))
			return false;
		if (closed != other.closed)
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		return true;
	}
	
	
}
