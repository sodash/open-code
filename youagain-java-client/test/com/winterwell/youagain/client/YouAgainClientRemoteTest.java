package com.winterwell.youagain.client;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import com.winterwell.utils.io.FileUtils;
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
		assert tokens == null;
	}

	
	@Test
	public void testRegisterFromJava() throws IOException {
		YouAgainClient yac = new YouAgainClient("test");
		Properties props = new Properties();
		props.load(FileUtils.getReader(new File("config/local.properties")));
		AuthToken token = yac.register("spoon.mcguffin@gmail.com", props.getProperty("spoon.password"));
		assert token != null;
	}

	@Test
	public void testLoginFromJava() throws IOException {
		YouAgainClient yac = new YouAgainClient("test");
		Properties props = new Properties();
		props.load(FileUtils.getReader(new File("config/local.properties")));
		AuthToken token = yac.login("spoon.mcguffin@gmail.com", props.getProperty("spoon.password"));
		assert token != null;
	}
	
	@Test
	public void testVerify() throws IOException {
		YouAgainClient yac = new YouAgainClient("datalog");
		yac.setDebug(true);
		List<String> jwt = Arrays.asList("eyJraWQiOiJ1anNpaWMxNWU3MDg5MDMyMSIsImFsZyI6IlJTMjU2In0.eyJpc3MiOiJnb29kbG9vcCIsImp0aSI6InVDMXc3Um1tN3hRYW9OS241MGpyV3ciLCJpYXQiOjE1MDY0MzQwMDgsImR2Y3NpZyI6ImlvcyBudWxsIiwic3ViIjoiZGFuaWVsQGdvb2QtbG9vcC5jb21AZW1haWwifQ.lMXog6BLOc5cmpdQakpn8yF9Wxg1vGzG9ut8cuQ-i2g9ajSljJf8cBKUJPOnQjqKYnPp1fwyM_76It2FXts8OtsbHH6V0hmNBOtRJKWdtuXtwTL_zp3GxJ9xvsvqWTU1v0OTb6D-18MjjbNiLZ35i5cvrZzOMYRcpH8hpGbZ-VWdLU7AcJXJPI9H-dLs4jCMbzkKh9xrZmr-dQHoVIDU8otsG1n3ZNfslfTW0TITbhrVweV_CKLIlxrNLvTV1GGXg5sjKMpMr0AAow59pS2H_tRlw-Fe4brmJ6OLfxvmCM8lNYt-6JzrXdK14LQA2UIl4-OzHEjRASXIFDownWT3DA");
		List<AuthToken> ok = yac.verify(jwt);
		System.out.println(ok);
	}

}
