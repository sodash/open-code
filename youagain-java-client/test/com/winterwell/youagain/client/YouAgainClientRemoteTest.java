package com.winterwell.youagain.client;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.web.WebEx;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.test.TestHttpServletRequest;
import com.winterwell.web.test.TestHttpServletResponse;

public class YouAgainClientRemoteTest {

	@Test
	public void testGetAuthTokensNoAuth() {
		YouAgainClient yac = new YouAgainClient("test");
		HttpServletRequest req = new TestHttpServletRequest();
		HttpServletResponse resp = new TestHttpServletResponse();
		WebRequest state = new WebRequest(req,resp);
		List<AuthToken> tokens = yac.getAuthTokens(state);
		assert tokens.isEmpty() : tokens;
	}

	
	@Test
	public void testRegisterFromJava() throws IOException {
		YouAgainClient yac = new YouAgainClient("test");
		Properties props = new Properties();
		props.load(FileUtils.getReader(new File("config/local.properties")));
		String p = props.getProperty("spoon.password");
		AuthToken token = yac.register("spoon.mcguffin@gmail.com", p);
		assert token != null;
	}
	
	@Test
	public void testBadRegisterFromJava() throws IOException {
		// make sure spoon is registered
		testRegisterFromJava();
		
		try {
			YouAgainClient yac = new YouAgainClient("test");
			String p = "not-right";
			AuthToken token = yac.register("spoon.mcguffin@gmail.com", p);
			assert false;
		} catch(WebEx ex) {
			// OK :)
		}
	}

	@Test
	public void testLoginFromJava() throws IOException {
		YouAgainClient yac = new YouAgainClient("test");
		Properties props = new Properties();
		props.load(FileUtils.getReader(new File("config/local.properties")));
		AuthToken token = yac.login("spoon.mcguffin@gmail.com", props.getProperty("spoon.password"));
		assert token != null;
	}
	
}
