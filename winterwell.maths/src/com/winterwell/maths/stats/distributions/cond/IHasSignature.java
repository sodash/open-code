package com.winterwell.maths.stats.distributions.cond;

import java.util.Map;

public interface IHasSignature {


	/**
	 * @return the signature of the Sitn {@link Cntxt}s which this stream
	 * generates.
	 * Can be empty, never null.
	 */
	String[] getContextSignature();
	
	/**
	 * Are some of the context features fixed for this model?
	 * @return Can be null
	 */
	Map<String,Object> getFixedFeatures();
	
}
