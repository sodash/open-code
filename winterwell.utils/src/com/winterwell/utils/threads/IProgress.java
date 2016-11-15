package com.winterwell.utils.threads;

/**
 * Interface for slow things that can report on their progress.
 * 
 * @author daniel
 */
public interface IProgress {

	/**
	 * How much of the (current) task has been done?
	 * 
	 * @return [done, total]. This may be [percentage, 1] for returning
	 *         percentages. This can be a total estimate, and it can be coarse
	 *         grained. If really unknown, return null. If done is known but not
	 *         total, return -1 for total.
	 */
	double[] getProgress();

}
