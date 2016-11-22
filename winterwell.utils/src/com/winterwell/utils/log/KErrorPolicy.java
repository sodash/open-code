package com.winterwell.utils.log;

public enum KErrorPolicy {
	/** Keep the error as is (throw an exception if this is not possible) */
	ACCEPT,
	/** Remove the file or whatever was to blame */
	DELETE_CAUSE,
	/**
	 * There is no point in going on.
	 *
	 * @deprecated You rarely want this.
	 */
	DIE,
	/** There is no error here. This never happened. */
	IGNORE,
	/** Report to log or user. */
	REPORT,
	/** Return null _or a default value_ in place of whatever. */
	RETURN_NULL,
	/** Raise hell. */
	THROW_EXCEPTION,
	/** Ask the user?! */
	ASK
}