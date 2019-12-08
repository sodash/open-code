package com.winterwell.youagain.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class App2AppAuthClientTest {

	@Test
	public void testGetIdentityTokenFromYA() {
		App2AppAuthClient a2a = new App2AppAuthClient();
		String appAuthName = "testapp";
		String appAuthPassword = "testpwd";
		a2a.getIdentityTokenFromYA(appAuthName, appAuthPassword);
	}

	@Test
	public void testRegisterIdentityTokenWithYA() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetPermissionsToken() {
		fail("Not yet implemented");
	}

}
