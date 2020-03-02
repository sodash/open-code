package com.winterwell.youagain.client;

public class YATestUtils {

	public static AuthToken getAuthTokenForSpoonMcGuffin(YouAgainClient yac) {
		try {
			AuthToken userToken = yac.login("spoonmcguffin@gmail.com", "my1stpassword");
			return userToken;
		} catch(Exception ex) {
			AuthToken userToken = yac.register("spoonmcguffin@gmail.com", "my1stpassword");
			return userToken;
		}
	}

}
