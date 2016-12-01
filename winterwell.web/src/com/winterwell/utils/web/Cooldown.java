package com.winterwell.utils.web;

import java.util.Set;

import com.winterwell.utils.ReflectionUtils;

import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.web.data.XId;

/**
 * Check here before running probes / using logins.
 * 
 * TODO could this be handled via BigMachine's ResourceManager??
 * @author daniel
 *
 */
public class Cooldown {

	static ExpiringMap<XId,String> expiringMap = new ExpiringMap();
	
	public static Set<XId> getAll() {
		return expiringMap.keySet();
	}
	
	public static void cooldown(XId login, Dt dt) {
		if (login==null) return;
		assert dt != null : login;
		Log.d("cooldown", login+" for "+dt+" "+ReflectionUtils.getSomeStack(6));
		String old = expiringMap.put(login, "cool", dt);		
	}

	public static void cooldownOver(XId xid) {
		expiringMap.remove(xid);
	}	
	
	public static boolean isCoolingDown(XId xid) {
		return expiringMap.get(xid) != null;
	}	
	
}
