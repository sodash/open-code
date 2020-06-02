package com.winterwell.bob.wwjobs;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.winterwell.utils.Dep;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.web.app.KServerType;

public class BuildHacks {

	/**
	 * local / test / production
	 */
	public static KServerType getServerType() {
		// cache the answer
		if (_serverType==null) {
			_serverType = getServerType2();
			Log.d("AppUtils", "Using serverType "+_serverType);
		}
		return _serverType;		
	}
	
	
	private static KServerType _serverType;
	private static String _hostname;

	/**
	 * Determined in this order:
	 *
	 * 1. Is there a config rule "serverType=dev|production" in Statics.properties?
	 * (i.e. loaded from a server.properties file)
	 * 2. Is the hostname in the hardcoded PRODUCTION_ and DEV_MACHINES lists?
	 *
	 * @return
	 */
	private static KServerType getServerType2() {
		// ISiteConfig??
		
		// explicit config ??who uses this?? is it for unit tests??
		if (Dep.has(Properties.class)) {
			String st = Dep.get(Properties.class).getProperty("serverType");
			if (st!=null) {
				Log.d("init", "Using explicit serverType "+st);			
				return KServerType.valueOf(st);
			} else {
				Log.d("init", "No explicit serverType in config");
			}
		} else {
			Log.d("init", "No Properties for explicit serverType");
		}
		// explicitly listed
		String hostname = getFullHostname();
		Log.d("init", "serverType for host "+hostname+" ...?");
		if (LOCAL_MACHINES.contains(hostname)) {
			Log.i("init", "Treating "+hostname+" as serverType = "+KServerType.LOCAL);
			return KServerType.LOCAL;
		}
		if (TEST_MACHINES.contains(hostname)) {
			Log.i("init", "Treating "+hostname+" as serverType = "+KServerType.TEST);
			return KServerType.TEST;
		}

		Log.i("init", "Fallback: Treating "+hostname+" as serverType = "+KServerType.PRODUCTION);
		return KServerType.PRODUCTION;
	}

	private static final List<String> LOCAL_MACHINES = Arrays.asList(
			"stross", "aardvark", "burgess", "kornbluth"
			);
	private static final List<String> TEST_MACHINES = Arrays.asList(
			"hugh", "baker", "mail.soda.sh", "mail", "mail.sodash.net"
			);

	public static String getFullHostname() {
		if (_hostname==null) _hostname = WebUtils.fullHostname();
		return _hostname;
	}
}
