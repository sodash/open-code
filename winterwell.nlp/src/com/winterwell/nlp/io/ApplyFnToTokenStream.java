package com.winterwell.nlp.io;

import com.winterwell.maths.timeseries.FilteredDataStream;
import com.winterwell.utils.IFn;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.AbstractIterator;

/**
 * 
 * @see FilteredDataStream
 * @author daniel
 */
public final class ApplyFnToTokenStream extends ATokenStream {

	/**
	 * Use this to strip out numbers
	 */
	public static final IFn<Tkn, Tkn> NO_NUMBERS = new IFn<Tkn, Tkn>() {
		@Override
		public Tkn apply(Tkn value) {
			if (value==null) return null;
			if (StrUtils.isNumber(value.getText())) {
				return null;
			}
			return value;
		}
	};
	
	private IFn<Tkn, Tkn> fn;

	/**
	 * @param base
	 * @param fn Can return null for "skip this one"
	 */
	public ApplyFnToTokenStream(ITokenStream base, IFn<Tkn, Tkn> fn) {
		super(base);
		this.fn = fn;
	}
	
	@Override
	protected Tkn processFromBase(Tkn original, AbstractIterator<Tkn> bit) {
		try {
			return fn.apply(original);
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}
	
	@Override
	public ITokenStream factory(String input) {		
		return new ApplyFnToTokenStream(base.factory(input), fn);	
	}

}
