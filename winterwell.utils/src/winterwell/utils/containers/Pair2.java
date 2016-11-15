package winterwell.utils.containers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import winterwell.utils.Utils;

/**
 * @deprecated Use com. version
 * Inspite of the name, this is the base class for Pair. It's more general wrt
 * types.
 * 
 * @author daniel
 * 
 * @param <A>
 * @param <B>
 */
public class Pair2<A, B> extends com.winterwell.utils.containers.Pair2<A, B> {
	private static final long serialVersionUID = 1L;

	public Pair2(A a, B b) {
		super(a,b);
	}

	/**
	 * Create a pair from a two-element list or set.
	 * 
	 * @param ab
	 */
	@SuppressWarnings("unchecked")
	public Pair2(Iterable<?> ab) {
		super(ab);
	}

}
