package com.winterwell.maths.stats.distributions.cond;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.containers.AbstractIterator;

/**
 * 
 * @author daniel
 * @testedby {@link ListSitnStreamTest}
 * @param <X>
 */
public class ListSitnStream<X> implements ISitnStream<X> {

	private final List<Sitn<X>> list;
	private final String[] sig;

	public ListSitnStream(List<Sitn<X>> list) {
		this(list,
			// just take the first sig, if one exists?!
			list.size() > 0 ? list.get(0).context.sig : new String[]{}
		);
	}

	public ListSitnStream(List<Sitn<X>> list, String[] sig) {
		this.list = list;
		// just take the first sig?!
		this.sig = sig;
	}
	
	@Override
	public Map<String, Object> getFixedFeatures() {
		return null;
	}

	@Deprecated // not supported for this class
	@Override
	public ISitnStream<X> factory(Object sourceSpecifier) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String[] getContextSignature() {
		return sig;
	}

	@Override
	public Collection<Class> getFactoryTypes() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public AbstractIterator<Sitn<X>> iterator() {
		final Iterator<Sitn<X>> sitnsit = list.iterator();
		AbstractIterator<Sitn<X>> it = new AbstractIterator<Sitn<X>>() {
			@Override
			protected Sitn<X> next2() throws Exception {
				return sitnsit.hasNext() ? sitnsit.next() : null;
			}			
		};	
		return it;
	}

}
