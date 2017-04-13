package com.winterwell.maths.vector;

/**
 * Marks objects which can be compacted, potentially giving a significant
 * reduction in memory, or a speed-up in performance.
 * 
 * @author daniel
 * 
 */
public interface ICompact {

	/**
	 * Compress the object for less memory / better performance.
	 */
	void compact();
}
