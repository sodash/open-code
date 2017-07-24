package com.winterwell.data;

/**
 * NB: Copy-pasta from Creole's KStatus
 * @author daniel
 *
 */
public enum KStatus {
	/**
	 * Status = 0, 
	 */
	DRAFT,

	/**
	 * The author has requested publishing, but doesn't have the appropriate
	 * rights.
	 */
	REQUEST_PUBLISH,
	
	 /**
	 * The object is cleared for future publication.
	 */
	PENDING,

	/**
	 * The object is published (status=2)
	 */
	PUBLISHED,

	/**
	 * Still in the back catalogue.
	 *  */
	ARCHIVED,

	/** Marked for deletion (but not yet deleted). */
	TRASH,

	/** special value for use only in url parameters */
	ALL_BAR_TRASH
}
