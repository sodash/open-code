package com.winterwell.youagain.client;

import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.utils.Dep;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.web.LoginDetails;
import com.winterwell.web.data.XId;

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
