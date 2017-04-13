/**
 * 
 */
package com.winterwell.nlp.similarity;

/**
 * Compare two words. Identical to {@link ICompareText}, but signifies that the
 * inputs should be single words.
 * 
 * @author Daniel
 * 
 */
public interface ICompareWords {

	/**
	 * How similar are a and b?
	 * 
	 * @param a
	 * @param b
	 * @return a value in [0,1]
	 */
	double similarity(String a, String b);

}
