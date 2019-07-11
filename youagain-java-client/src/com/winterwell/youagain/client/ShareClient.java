package com.winterwell.youagain.client;

import java.util.Map;

import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.ajax.JSend;
import com.winterwell.web.ajax.JThing;
import com.winterwell.web.data.XId;

public final class ShareClient {

	public static final String ACTION_SHARE = "share";

	ShareClient(YouAgainClient youAgainClient) {
		this.yac = youAgainClient;
	}

	YouAgainClient yac;

	public ShareToken share(AuthToken authToken, String item, XId targetUser) {
		FakeBrowser fb = new FakeBrowser()
				.setDebug(true);
		fb.setAuthenticationByJWT(authToken.getToken());
		Map<String, String> shareAction = new ArrayMap(
			"action", ACTION_SHARE,
			"app", yac.app,
			"shareWith", targetUser,
			"entity", item
		);
		// call the server
		System.out.println("");
		String response = fb.getPage(yac.yac.endpoint, shareAction);
		
		JSend jsend = JSend.parse(response);		
		JThing d = jsend.getData();
		d.setType(ShareToken.class);
		Object st = d.java();
		return (ShareToken) st;
	}
	
}
