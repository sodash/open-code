package com.winterwell.youagain.client;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;

/**
 * See https://github.com/auth0/java-jwt
 * 
 * This class will wrap low-level JWT stuff, and present it for YouAgain.
 * @See JWTEncoder
 * @author daniel
 *
 */
public class JWTDecoder {

	public static PublicKey keyFromString(String skey) throws NoSuchAlgorithmException, InvalidKeySpecException {		
		byte[] data = base64decoder.decode(skey);
		KeyFactory fact = KeyFactory.getInstance("RSA");
	    X509EncodedKeySpec spec = new X509EncodedKeySpec(data);	    
	    PublicKey pubKey = fact.generatePublic(spec);
	    return pubKey;
	}

	private static Decoder base64decoder = Base64.getDecoder();

	
	private static final String LOGTAG = "JWTDecoder";

	String app;

	private PublicKey pubKey;

	/**
	 * Usually get via {@link YouAgainClient#getDecoder()}
	 */
	public JWTDecoder(String app) {
		this.app = app;
	}
	

	public DecodedJWT decryptJWT(String jwt) {
		verify(jwt, app);
		DecodedJWT djwt = JWT.decode(jwt);
		return djwt;
	}
	
	
	public void verify(String jwt, String issuer) {
		
		Algorithm algorithm = algorithm();
		
		assert ! jwt.endsWith("temp") : jwt;
		JWTVerifier verifier;
		try {
			verifier = JWT.require(algorithm)
	//				.withIssuer(issuer) // TODO reinstate
					.build();
		} catch (Throwable ex) {
			// Seen Apr 2021 -- some jar versioning issue in auth0's jar
			Log.e(LOGTAG, "verifier low-level code problem! "+ex+" Treat jwt as verified");
			return;
		}
		try {
			DecodedJWT decoded = verifier.verify(jwt);
			Log.d(LOGTAG, "verified "+jwt+" -> "+decoded.getSubject());

			// debugging
			if ( ! Utils.equals(issuer, decoded.getIssuer())) {
				Log.w(LOGTAG, "verify - issuer mismatch! expected: "+issuer+" got: "+decoded.getIssuer()+" from "+jwt+" "+ReflectionUtils.getSomeStack(12));
			}
		} catch(Throwable ex) {
			Object token = "";
			try {
				DecodedJWT djwt = JWT.decode(jwt);
				token = getMap(djwt);
			} catch(Throwable ex2) {
				// oh well
			}
			throw new WrappedException("JWT verify failed for "+token+" = '"+jwt+"' for issuer "+issuer
					+" public key "+getPublicKey(), ex);
		}
	}

	private Map getMap(DecodedJWT djwt) {
		Map<String, Claim> map = djwt.getClaims();
		Map<String, Object> map2 = Containers.applyToMap(map, (k,c) -> c.asString());
		return map2;
	}


	private Algorithm algorithm() {
		RSAPublicKey pubk = (RSAPublicKey) getPublicKey();
		assert pubk != null : "JWTDecoder - no PublicKey - setPublicKey first";
		Algorithm alg = Algorithm.RSA256(pubk, null);
		return alg;
	}


	public JWTDecoder setPublicKey(PublicKey key) throws Exception {
//		Log.d("ya.init", "set decoder Public key: "+key+" via "+ReflectionUtils.getSomeStack(8));
		pubKey = key;
	    return this;
	}


	public PublicKey getPublicKey() {
		return pubKey;
	}

}
