package com.winterwell.youagain.client;

import org.junit.Test;

import com.winterwell.web.LoginDetails;
import com.winterwell.web.data.XId;
/**
 * See also tests in the YA server project (which are able to spin up a server and access server classes too).
 * @author daniel
 *
 */
public class YouAgainClientTest {

	@Test
	public void testInit() {
		YouAgainClient yac = new YouAgainClient("goodloop");
		assert yac.yac.endpoint != null;
	}
	

//	@Test // needs a proper password to work
	public void testGetOAuthTokens() {
		YouAgainClient yac = new YouAgainClient("sogive");
//		yac.setENDPOINT("http://localyouagain.good-loop.com/youagain.json");
		XId txid = new XId("winterstein@twitter");
		LoginDetails appOwnerAuth = new LoginDetails("localyouagain.good-loop.com", "daniel@sodash.com", "password");
		String[] oauth = yac.getOAuthTokens(txid, appOwnerAuth);
		assert oauth.length==2;
//		Twitter jtwit = new Twitter();
	}
}
