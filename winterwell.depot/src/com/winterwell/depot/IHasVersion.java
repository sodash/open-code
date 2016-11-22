package com.winterwell.depot;

/**
 * Marker interface for artifacts which have a version number. 
 * If the artifact is stored in a database, then the version number
 * MUST be stored in the field/column vrsn (to allow sql level checks).
 * <p>
 * It is an error to try and save an older version over the top of a newer one.
 * <p>
 * Use-cases:<br>
 * 1. Defend against bugs where edits are lost due to Hibernate's implicit tracking of 
 * dirty objects leading to unwanted saves. <br>
 * 2. Provide extra info on Depot lifecycles.
 * 
 * @author daniel
 */
public interface IHasVersion {
	
	Object getVrsn();
	
	public static interface IHasBefore {
		/**
		 * Set a starting state, from which we can measure edits to make a diff.
		 * @param before
		 */
		void setBefore(Object before);
		
		/**
		 * Get the starting state, from which we can measure edits to make a diff.
		 * @param before Can be null.
		 */
		Object getBefore();
	}
	
	
}
