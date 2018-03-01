package com.winterwell.youagain.client;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Base64.Decoder;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

import com.winterwell.utils.log.Log;

/**
 * This class will wrap low-level JWT stuff, and present it for YouAgain.
 * @See JWTEncoder
 * @author daniel
 *
 */
public class JWTDecoder {

	private PublicKey pubKey;

	private static Decoder base64decoder = Base64.getDecoder();
	
	public JWTDecoder() {
	}
	

	public JwtClaims decryptJWT(String jwt) throws InvalidJwtException {
		assert ! jwt.endsWith("temp") : jwt;
	    // Use JwtConsumerBuilder to construct an appropriate JwtConsumer, which will
	    // be used to validate and process the JWT.
	    // The specific validation requirements for a JWT are context dependent, however,
	    // it typically advisable to require a (reasonable) expiration time, a trusted issuer, and
	    // and audience that identifies your system as the intended recipient.
	    // If the JWT is encrypted too, you need only provide a decryption key or
	    // decryption key resolver to the builder.
		JwtConsumerBuilder jcb = new JwtConsumerBuilder()
//	            .setRequireExpirationTime() // the JWT must have an expiration time
//	            .setMaxFutureValidityInMinutes(300) // but the  expiration time can't be too crazy
//	            .setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims to account for clock skew
	            .setRequireSubject() // the JWT must have a subject claim
//	            .setExpectedIssuer("Issuer") // whom the JWT needs to have been issued by
//	            .setExpectedAudience("Audience") // to whom the JWT is intended for
	            ;
		if (pubKey==null) {
			// doesnae work :(
			jcb.setSkipAllValidators();
		} else {
			jcb.setVerificationKey(pubKey);
		}
		JwtConsumer jwtConsumer = jcb.build(); // create the JwtConsumer instance

        //  Validate the JWT and process it to the Claims
        JwtClaims jwtClaims = jwtConsumer.processToClaims(jwt);
        return jwtClaims;
	}
	
	public JWTDecoder setPublicKey(String key) throws Exception {
		Log.d("ya.init", "decoder Public key: "+key);
	    byte[] data = base64decoder.decode(key);
	    X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
	    KeyFactory fact = KeyFactory.getInstance("RSA");
	    pubKey = fact.generatePublic(spec);
	    return this;
	}


	public PublicKey getPublicKey() {
		return pubKey;
	}

}
