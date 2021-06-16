package com.winterwell.web.ajax;

/**
 * See https://github.com/omniti-labs/jsend
 * @author daniel
 *
 */
public enum KAjaxStatus {
	success,	
	/** There was a problem with the data submitted, or some pre-condition of the API call wasn't satisfied.
	 *  Can return data.
	 *  */
	fail, 
	/** server problem. Should return a message */
	error,
	/**
	 * http code 202 - wait
	 */
	accepted
}
