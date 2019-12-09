package com.winterwell.youagain.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class App2AppAuthClientTest {

	@Test
	public void testGetIdentityTokenFromYA() {
		YouAgainClient yac = new YouAgainClient("test");
		App2AppAuthClient a2a = new App2AppAuthClient(yac);
		String appAuthName = "testapp2";
		String appAuthPassword = "testpwd";
		AuthToken token = a2a.getIdentityTokenFromYA(appAuthName, appAuthPassword);
		System.out.println(token);
		assert token != null;
	}

	@Test
	public void testRegisterIdentityTokenWithYA() {
		YouAgainClient yac = new YouAgainClient("test");
		App2AppAuthClient a2a = new App2AppAuthClient(yac);
		String appAuthName = "testapp2";
		String appAuthPassword = "testpwd";
		AuthToken token = a2a.registerIdentityTokenWithYA(appAuthName, appAuthPassword);
		System.out.println(token);
		assert token != null;
	}

	@Test
	public void testGetPermissionsToken() {
		fail("Not yet implemented");
	}

}
