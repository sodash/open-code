package com.winterwell.youagain.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.WebEx;
import com.winterwell.web.ajax.JSend;
import com.winterwell.web.ajax.JThing;
import com.winterwell.web.data.XId;

public final class ShareClient {

	public static final String ACTION_SHARE = "share";
	public static final String ACTION_DELETE_SHARE = "delete-share";
	public static final String ACTION_CLAIM = "claim";

	ShareClient(YouAgainClient youAgainClient) {
		this.yac = youAgainClient;
	}

	YouAgainClient yac;

	/**
	 * 
	 * @param authToken TODO manage this better
	 * @param prefix Optional
	 * @return
	 */
	public List<String> getSharedWith(AuthToken at, String prefix) {
		return getSharedWith(Collections.singletonList(at), prefix);
	}
	
	public List<String> getSharedWith(List<AuthToken> auths, String prefix) {		
		try {
			FakeBrowser fb = new FakeBrowser();
			List<String> jwts = Containers.apply(auths, AuthToken::getToken);
			fb.setAuthenticationByJWTs(jwts);
			fb.setDebug(true);
			String response = fb.getPage(yac.yac.endpoint, new ArrayMap(
					"app", yac.iss,
					"action", "shared-with",
					"prefix", prefix));
			
			Map jobj = (Map) JSON.parse(response);
			Object shares = SimpleJson.get(jobj, "cargo");
			if (shares instanceof Object[]) {
				return Arrays.stream((Object[]) shares).map(share -> (String) SimpleJson.get(share, "item")).collect(Collectors.toList());
			}
		} catch (WebEx.E401 e401) {
			Log.d("ShareClient.getSharedWith", e401);
			return Collections.emptyList();	
		}
		return Collections.emptyList();
	}
	
	/** List the users a particular entity is shared to 
	 * @param auths */
	public List<ShareToken> getShareList(CharSequence share, List<AuthToken> auths) {
		if (auths.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		FakeBrowser fb = yac.fb(auths);
		String response = fb.getPage(yac.yac.endpoint, new ArrayMap(
			"app", yac.iss,
			"action", "share-list",
			"entity", share.toString()
			));

		JSend.parse(response).getData();
		Map jobj = (Map) JSON.parse(response);
		Object shares = SimpleJson.get(jobj, "cargo");
		if (shares==null) return null;
		List<Map> lshares = Containers.asList(shares);
		List<ShareToken> sts = Containers.apply(lshares, sm -> new ShareToken((Map)sm));
		return sts;
	}
	
	/**
	 * 
	 * @param authToken Who authorises this share?
	 * @param item ID of the thing being shared.
	 * @param targetUser Who is it shared with?
	 */
	public ShareToken share(AuthToken authToken, String item, XId targetUser) {
		FakeBrowser fb = new FakeBrowser()
				.setDebug(true);
		fb.setAuthenticationByJWT(authToken.getToken());
		Map<String, String> shareAction = new ArrayMap(
			"action", ACTION_SHARE,
			"app", yac.iss,
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
	
	public boolean delete(AuthToken authToken, String item, XId targetUser) {
		FakeBrowser fb = new FakeBrowser()
				.setDebug(true);
		fb.setAuthenticationByJWT(authToken.getToken());
		Map<String, String> shareAction = new ArrayMap(
			"action", ACTION_DELETE_SHARE,
			"app", yac.iss,
			"shareWith", targetUser,
			"entity", item
		);
		// call the server
		System.out.println("");
		fb.getPage(yac.yac.endpoint, shareAction);
		
		// No exception? It's done.
		if (fb.getStatus() >= 200 && fb.getStatus() < 400) return true;
		return false;
	}

	public boolean canWrite(AuthToken authToken, String item, List<ShareToken> shares) {
		Utils.check4null(authToken, item, shares);
		for (ShareToken shareToken : shares) {
			if ( ! item.equals(shareToken.getItem())) continue;
			if ( ! shareToken.write) continue;
			if (shareToken.getTo().contains(authToken.getXId())) {
				return true;
			}
		}
		return false;
	}

	public List<ShareToken> getShareStatus(ShareTarget share, List<AuthToken> auths) {
		if (auths.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		List<ShareToken> list = getShareList(share, auths);
		List<XId> myXIds = Containers.apply(auths, AuthToken::getXId);
		List<ShareToken> myShares = Containers.filter(list, st -> ! Collections.disjoint(st.getTo(), myXIds));
		return myShares;
	}
	
}
