package com.winterwell.maths.stats;
/**
 * Different goodness of model scores
 */
public enum KScore {
	/** How much of the target variance have we explained? */
	R2,
	/** Discount R2 by the number of explanatory variables used. */
	ADJUSTED_R2,
	
	/** compare categorical outputs */
	CHI2,
	
	RMSE,
	
	/** 
	 * This is normalised by y_mean.
	 * For the vector case, it is normalised by ||y_mean|| 
	 * This will go awry if the mean is 0 or near zero!*/
	NRMSE,
	
	/**
	 * See https://en.wikipedia.org/wiki/Mean_absolute_percentage_error
	 */
	MAPE,	
	
	/**
	 * K-fold cross-validation -- i.e. leave-some-out-for-testing, then swap.
	 */
	KFOLD,
	
	/**
	 * Output the residuals as a column
	 */
	RESIDUALS
}
