package com.winterwell.maths.graph;

import com.winterwell.utils.IFn;

public interface IHasValue<V> {

	V getValue();
	
	public static final IFn EXTRACT = new IFn<IHasValue,Object>() {
		@Override
		public Object apply(IHasValue x) {
			return x.getValue();
		}
	};
}
