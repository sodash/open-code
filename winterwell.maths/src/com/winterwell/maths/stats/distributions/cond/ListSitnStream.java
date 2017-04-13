package com.winterwell.maths.stats.distributions.cond;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.containers.AbstractIterator;

public class ListSitnStream<X> implements ISitnStream<X> {

	private List<Sitn<X>> list;
	private String[] sig;

	public ListSitnStream(List<Sitn<X>> list) {
		this.list = list;
		// just take the first sig?!
		this.sig = list.get(0).context.sig;
	}
	
	@Override
	public Map<String, Object> getFixedFeatures() {
		return null;
	}

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
		// TODO Auto-generated method stub
		return null;
	}

}
