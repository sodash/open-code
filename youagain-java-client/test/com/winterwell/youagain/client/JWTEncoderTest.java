package com.winterwell.youagain.client;

import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.utils.Utils;

public class JWTEncoderTest {

	@Test
	public void testMakeKey() throws Exception {
		
		String keyName = "test_"+Utils.getRandomString(4);
		RSAKeyPair fooKey = JWTEncoder.getKey(keyName);
		
		// caching
		RSAKeyPair fooKey2 = JWTEncoder.getKey(keyName);
		assert fooKey==fooKey2;
	}

}
