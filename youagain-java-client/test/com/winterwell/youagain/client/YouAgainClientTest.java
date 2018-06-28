package com.winterwell.youagain.client;

import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.utils.Dep;
import com.winterwell.utils.io.ConfigFactory;
import com.winterwell.utils.io.FileUtils;

public class YouAgainClientTest {

	@Test
	public void testInit() {
		YouAgainClient yac = new YouAgainClient("goodloop");
		assert yac.ENDPOINT != null;
	}

}
