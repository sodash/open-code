package com.winterwell.nlp.similarity;

/**
 * Compare two pieces of text.
 * 
 * @author daniel
 * 
 */
public interface ICompareText {

	/**
	 * How similar are a and b?
	 * 
	 * @param a
	 * @param b
	 * @return a value in [0,1] 1=equivalent, 0=no-connection TODO should this
	 *         be a metric instead?
	 */
	double similarity(String a, String b);

}
