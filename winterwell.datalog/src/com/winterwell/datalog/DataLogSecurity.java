/**
 * 
 */
package com.winterwell.datalog;

import java.util.List;

import com.winterwell.utils.log.Log;
import com.winterwell.web.ajax.AjaxMsg;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.data.XId;

/**
 * @author daniel
 *
 */
public class DataLogSecurity {

	public static void check(WebRequest state, String dataspace, List<String> breakdown) {
		// TODO insist on login
		XId user = state.getUserId();
		if (user==null) {
			state.addMessage(new AjaxMsg(new SecurityException("not logged in")));
			Log.e("security", "not logged in "+state);
		}
		
		// TODO check shares with YouAgain -- all the authd XIds		
		
		// assume all OK!
	}

}
