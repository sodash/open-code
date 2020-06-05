package com.winterwell.bob;

import com.winterwell.utils.Utils;
import com.winterwell.utils.log.KErrorPolicy;
import com.winterwell.utils.log.Log;

public interface IErrorHandler {

	void handle(Throwable ex); // throws Exception;
	
	public static IErrorHandler IGNORE = err -> {}; 
	
	public static IErrorHandler forPolicy(KErrorPolicy policy) {
		switch(policy) {
		case ACCEPT:
		case IGNORE:
			return IGNORE;
		case DIE:
			return err -> {
				Log.e(err);
				System.exit(1);
			};
		case REPORT:
		case RETURN_NULL:
			return Log::e;
		case THROW_EXCEPTION:
			return err -> {
				throw Utils.runtime(err); 
			};
		}
		// :(
		throw new IllegalArgumentException("No default handling for "+policy);
	}
	
}

