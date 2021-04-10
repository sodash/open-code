package com.winterwell.youagain.client;

import java.util.Arrays;
import java.util.List;

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
	
	@Test
	public void testVerify() {
		YouAgainClient yac = new YouAgainClient("good-loop");
		assert yac.yac.endpoint != null;
		List<String> jwts = Arrays.asList(
				"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzZXJ2ZXIiOiJiYWlsZXkiLCJzdWIiOiJkYW5pZWxAZ29vZC1sb29wLmNvbUBlbWFpbCIsImlzcyI6Im1vbmV5c2NyaXB0IiwiaWF0IjoxNjE4MDg2MjQ2LCJqdGkiOiJkc2Fsb3AxNzhiZDc0YjcwOCJ9.WipVoyFyhnKXIT4tFqfMcAhtUniM5GpplHV3K804fjGT8zMi0fMzPsdopckCRzynqgGUQNf1WzLKqki4vMmmI7v6FLaSBPOAjTQdvHpABjQVgohuaTAhxMBXAo6su1A6_a0MRT9-47pa8Muj_GUoFWsuPQJQGfbx3KcRS3G9srcGdW2fOE2GDkcTAubuPNHXLihQpysNMPsEgGYxeWhd1W-KTNl5mtpuposZeaOuYnpd8wqWq9SLw7ODxd6-oZhV8BAz9jtWiX77K0px1tIx0aT2ep951dTiMouwrwmZEbuUsepg3FuoZP5wDxwJL4YxD1rl_vziypAeRO6inC9dKA"
		);
		yac.verify(jwts, null);
		
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
