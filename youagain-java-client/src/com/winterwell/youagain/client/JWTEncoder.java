package com.winterwell.youagain.client;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Date;
import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.winterwell.utils.Dep;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.utils.web.XStreamUtils;
import com.winterwell.web.data.XId;

/**
 * This class will wrap low-level JWT stuff, and present it for YouAgain.
 * 
 * Not safe as a global 'cos deviceSignature is user specific.
 * 
 * @author daniel
 * @testedby JWTTest
 */
public class JWTEncoder {
	public static final String CLAIM_DEVICE_SIG = "dvcsig";

	String app;

	private String deviceSignature;	
	
	public JWTEncoder(String app) {
		this.app = app;
	}
	
	public void setDeviceSignature(String deviceSignature) {
		this.deviceSignature = deviceSignature;
	}
	
	/**
	 * Make an encrypted JWT token
	 * @param state
	 * @param xid
	 * @return
	 * @throws JoseException
	 */
	public String encryptJWT(XId xid) throws Exception {
		return encryptJWT(xid, null);
	}
	
	/**
	 * 
	 * @param xid Identity aka subject
	 * @param extraProperties
	 * @return
	 * @throws Exception
	 */
	public String encryptJWT(XId xid, Map<String,Object> extraProperties) throws Exception {
	    // Create the Claims, which will be the content of the JWT
		Algorithm alg = algorithm();
		Builder jwtb = JWT.create();
	    if (app!=null) {
	    	jwtb.withIssuer(app);  // who creates the token and signs it
	    }
	    // for debugging -- where did this come from?
	    jwtb.withClaim("server", WebUtils2.fullHostname());
//	    claims.setAudience("Audience"); // to whom the token is intended to be sent
//	    claims.setExpirationTimeMinutesInTheFuture(10); // time when the token will expire (10 minutes from now)
	    jwtb.withJWTId(Utils.getUID()); // a unique identifier for the token
	    jwtb.withIssuedAt(new Date());
	    // TODO reject tokens if the device doesn't match
	    jwtb.withClaim(CLAIM_DEVICE_SIG, deviceSignature);
//	    claims.setNotBeforeMinutesInThePast(2); // time before which the token is not yet valid (2 minutes ago)
	    jwtb.withSubject(xid.toString()); // the subject/principal is whom the token is about
//	    claims.setClaim("email","mail@example.com"); // additional claims/attributes about the subject can be added	    
//	    List<String> groups = Arrays.asList("group-one", "other-group", "group-three");
//	    claims.setStringListClaim("groups", groups); // multi-valued claims work too and will end up as a JSON array

	    // TODO Add oauth tokens, so this token (+ server secrets) is all you need for Twitter??
	    
	    if (extraProperties!=null) {
	    	for(String p : extraProperties.keySet()) {
	    		Object v = extraProperties.get(p);
	    		if (v==null) continue;
	    		if (v instanceof Boolean) {
	    			jwtb.withClaim(p, (Boolean)v);
	    		} else {
	    			jwtb.withClaim(p, v.toString());
	    		}
	    	}
	    }
	    
//	    TODO jwtb.withKeyId(getPublicKeyId());
	    
	    // The payload of the JWS is JSON content of the JWT Claims	    
	    String jwt = jwtb.sign(alg);

	    Log.d("ya", "signed jwt for "+xid+" = "+jwt+" "+ReflectionUtils.getSomeStack(10));
	    
	    // sanity check	    
	    JWTDecoder dec = new JWTDecoder(app);
	    dec.setPublicKey(getPublicKey());
	    DecodedJWT decd = dec.decryptJWT(jwt);
	    Log.d("ya", "decodes "+jwt+" to JWT for "+decd.getSubject()+" w public key "+dec.getPublicKey());
	    
	    return jwt;
	}
	

	private Algorithm algorithm() {
		RSAKeyPair key = getKey(YouAgainClient.MASTERAPP);
		Algorithm alg = Algorithm.RSA256(key.publicKey, key.privateKey);
		return alg;
	}

	static RSAKeyPair getKey(String signingApp) {
		if (Dep.has(RSAKeyPair.class)) {
			RSAKeyPair rsaKey = Dep.get(RSAKeyPair.class);
			return rsaKey;
		}
		// Do we have a file?
		File propsFile = new File("config", FileUtils.safeFilename(signingApp+".RSAKeyPair.xml", false));
		try {
			if (propsFile.isFile()) {
				RSAKeyPair rsaJsonWebKey = XStreamUtils.serialiseFromXml(FileUtils.read(propsFile));
				Log.i("ya.init", "key for "+signingApp+" loaded from "+propsFile);
				Dep.set(RSAKeyPair.class, rsaJsonWebKey);				
				return rsaJsonWebKey;
			}
		} catch(Exception ex) {
			Log.e("Login", ex);
		}
		// make a key!		
		return newKey(propsFile);
	}
	

	static RSAKeyPair newKey(File saveTo) {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			KeyPair _kp = kpg.generateKeyPair();
			RSAKeyPair kp = new RSAKeyPair(_kp);
	//	     TODO Give the JWK a Key ID (kid), which is just the polite thing to do
//		    rsaJsonWebKey.setKeyId(Utils.getUID());
		    if (saveTo != null) {
		    	String xml = XStreamUtils.serialiseToXml(kp);
		    	saveTo.getParentFile().mkdirs();
		    	FileUtils.write(saveTo, xml);
		    }
		    Log.i("ya.init", "new key made! public-key: "+kp.getPublic());
		    Dep.set(RSAKeyPair.class, kp);
		    return kp;
		} catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	public PublicKey getPublicKey() {
		PublicKey pkey = getKey(YouAgainClient.MASTERAPP).publicKey;
		return pkey;
	}
	
	public String getPublicKeyId() {
		RSAKeyPair webKey = getKey(YouAgainClient.MASTERAPP);
		String kid = webKey.id;
		return kid;
	}

	@Override
	public String toString() {
		return "JWTEncoder[app=" + app + "]";
	}


}
