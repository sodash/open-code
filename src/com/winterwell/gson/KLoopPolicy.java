package com.winterwell.gson;

/**
 * @author daniel
 *
 */
public enum KLoopPolicy {
	/** The default behaviour (NB: this is equivalent to setting null as the loop-policy). Loops will cause stack-overflow. */
	NO_CHECKS,
	/**
	 * Check and throw an exception if a loop is detected.
	 */
	EXCEPTION,
	/** If a loop occurs, put in a null. So you can safely serialise objects with circular references, but deserialising may lose info*/
	QUIET_NULL,
//	/**
//	 * TODO JSOG style "@id" and "@ref" fields. This differs from JSOG in that it only uses "@id" when a loop is detected
//	 */
//	ID_REF,
	/**
	 * TODO Use "@id" and "@ref" fields for the JSOG format.
	 */
	JSOG
	;
}
