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
	 * Published but there are unpublished edits
	 */
	MODIFIED,

	/**
	 * The author has requested publishing, but doesn't have the appropriate
	 * rights.
	 */
	REQUEST_PUBLISH,
	
	 /**
	 * The object is cleared for future publication. E.g. this is a scheduled Facebook post.
	 */
	PENDING,

	/**
	 * The object is published
	 */
	PUBLISHED,

	/**
	 * Still in the back catalogue. @deprecated Not currently used
	 *  */
	ARCHIVED,

	/** Marked for deletion (but not yet deleted). */
	TRASH,

	/** special value for use only in url parameters */
	ALL_BAR_TRASH;

	/**
	 * We only have three main ES indices, published and draft (which holds all the non-published)
	 * and trash index, but this is not normally searched.
	 */
	public static KStatus[] main() {
		return new KStatus[] {PUBLISHED, DRAFT, TRASH};
	}
}
