package com.winterwell.web.app;

import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.web.LoginDetails;

public class LoginsTest {

	@Test
	public void testGet() {
		LoginDetails ld = Logins.get("words.bighugelabs.com");
		assert ld != null;
	}

}
