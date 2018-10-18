package com.winterwell.utils.log;

import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;

public enum KErrorPolicy {
	/** Keep the error as is (throw an exception if this is not possible) */
	ACCEPT,
	/** Remove the file or whatever was to blame.
	 * 
	 * Warning: In Depot (to avoid trashing valued data) this only deletes in a couple of cases.
	 *  */
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
	ASK;

	
	public static <X> X process(KErrorPolicy ep, Throwable e) {
		// default to throw
		if (ep==null) ep = KErrorPolicy.THROW_EXCEPTION;		
		if (e==null) e = new RuntimeException();
		switch(ep) {
		case DIE:
			Log.e("dieonerror", e);
			System.exit(1);
		case THROW_EXCEPTION:
			throw Utils.runtime(e);
		case REPORT:
			Log.e("error", e);
		case ACCEPT: case IGNORE:
			return null;
		case ASK: case DELETE_CAUSE:
			Log.e("error", new TodoException("Handle "+ep+" for "+e));
			throw Utils.runtime(e);
		}
		return null;
	}
}