package com.winterwell.youagain.client;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.jetty.util.ajax.JSON;
import org.jose4j.jwt.JwtClaims;

import com.winterwell.utils.Key;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.containers.Properties;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.WebEx;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.data.XId;
import com.winterwell.web.fields.ListField;
import com.winterwell.web.fields.SField;
import com.winterwell.web.fields.XIdField;

import lgpl.haustein.Base64Encoder;

public class YouAgainClient {

	static final String ENDPOINT = "https://youagain.winterwell.com/youagain.json";

	private static final Key<List<AuthToken>> AUTHS = new Key("auths");

	private static final String LOGTAG = "youagain";
	
	final String app;

	private boolean debug;
	
	public YouAgainClient(String app) {
		assert ! Utils.isBlank(app);
		this.app = app;
		
		setDebug(true); // FIXME
	}	
	
	/**
	 * TODO
	 * This will also call state.setUser()
	 * @param state
	 * @return null if not logged in at all, otherwise list of AuthTokens
	 */
	public List<AuthToken> getAuthTokens(WebRequest state) {
		List<AuthToken> tokens = state.get(AUTHS);
		if (tokens!=null) return tokens;
		List<String> jwt = getAllJWTTokens(state);
		// basic auth?
		AuthToken basicToken = null;
		Pair<String> np = WebUtils2.getBasicAuthentication(state.getRequest());
		if (np !=null) {
			// verify it
			basicToken = verifyNamePassword(np.first, np.second);
		}
		if (jwt.isEmpty() && basicToken==null) return null;
		// verify the tokens
		tokens = verify(jwt);
		// add the name/password user first, if set
		if (basicToken!=null) tokens.add(0, basicToken);
		// stash them
		state.put(AUTHS, tokens);
		// set user?
		XId uxid = getUserId2(state, tokens);		
		return tokens;
	}
	
	private AuthToken verifyNamePassword(String email, String password) {
		Utils.check4null(email, password);
		FakeBrowser fb = new FakeBrowser();
		String response = fb.getPage(ENDPOINT, new ArrayMap(
				"app", app, 
				"action", "login", 
				"person", email,
				"password", password));
		// FIXME
		Map jobj = (Map) JSON.parse(response);
		System.out.println(response);			
		AuthToken token = new AuthToken(null);
		token.xid = new XId(email, "email");
		return token;
	}

	List<AuthToken> verify(List<String> jwt) {
		Log.d(LOGTAG, "verify: "+jwt);
		List<AuthToken> list = new ArrayList();
		if (jwt.isEmpty()) return list;
		try {
			FakeBrowser fb = new FakeBrowser();
			Object response = fb.getPage(ENDPOINT, new ArrayMap(
					"app", app, 
					"action", "verify", 
					"jwt", StrUtils.join(jwt, ","),
					"debug", debug
					));
			Log.w(LOGTAG, "TODO process YouAgain verify response: " + response);
		} catch(Throwable ex) {
			Log.w(LOGTAG, ex); // FIXME
		}
		// HACK Security hole!
		for (String jt : jwt) {
			try {
				AuthToken token = new AuthToken(jt);
				// FIXME decode the token properly!
				JWTDecoder dec = getDecoder();
				JwtClaims decd = dec.decryptJWT(jt);
				token.xid = new XId(decd.getSubject(), false);
				list.add(token);
			} catch (Exception e) {
				Log.e(LOGTAG, e);
			}
		}
		return list;		
	}

	private JWTDecoder getDecoder() throws Exception {
		JWTDecoder dec = new JWTDecoder();
		String pkey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu3njghYwWN8Bf/f6FndCr3h3/uzPNctZZf2qLHqGicZaQQvjMqFfr2tz/JGsFkxeCSeEuLqzHjBoc8P9S2aKb7X04b/OfTJkSjH/5UArKuAGZL1/hVFwZwnSoQOhklElHtq/RwGUgemmu7QIjTcgKEINUNzC537vWOtiQkWAO/abqwpfQgKPvMNvViPMrJtk8A07bFetQKjU4A6do6E3BvTItzgMZJLmMVePn8Yo3uH/7rLtKybl2tn8BhOWPGLnEyZiPZ2f8V/56hR1zsHr9i9QMjLX8O18+w4pno04jST2Yp7yxTNN3mttqgyl8s8oFMptSG2/3g+WqwwwBTbGgQIDAQAB"; 
		// FIXME load from the server, so we could change keys
		dec.setPublicKey(pkey);
		return dec;
	}

	/**
	 * Low-level access to JWT tokens
	 * https://en.wikipedia.org/wiki/JSON_Web_Token
	 * @param state
	 * @return
	 */
	public List<String> getAllJWTTokens(WebRequest state) {		
		Collection<String> all = new ArrayList();		
		// Auth header
		String authHeader = state.getRequest().getHeader("Authorization");
		if (authHeader!=null) {
			authHeader = authHeader.trim();
			if (authHeader.startsWith("Bearer")) {
				String jwt = authHeader.substring("Bearer".length(), authHeader.length()).trim();
				if (state.debug) Log.d(LOGTAG, "JWT from auth-header Bearer: "+jwt);
				all.add(jwt);
			}
		}		
		// NB: This isnt standard, this is our naming rule 
		Pattern KEY = Pattern.compile("^(\\w+\\.)?jwt");
		// cookies
		Map<String, String> cookies = WebUtils2.getCookies(state.getRequest());
		for(String c : cookies.keySet()) {
			if ( ! KEY.matcher(c).find()) continue; 
			String jwt = cookies.get(c);
			if (state.debug) Log.d(LOGTAG, "JWT from cookie "+c+": "+jwt);
			all.add(jwt);
		}				
		// and parameters
		Map<String, Object> pmap = state.getParameterMap();
		for(String c : pmap.keySet()) {
			if ( ! KEY.matcher(c).matches()) continue;		
			Object jwt = pmap.get(c);
			// is it a list??
			if (jwt instanceof String[]) {
				List<String> jwts = Containers.asList((String[])jwt);
				if (state.debug) Log.d(LOGTAG, "JWTs from parameter "+c+": "+jwts);				
				all.addAll(jwts);	
			} else {
				if (state.debug) Log.d(LOGTAG, "JWT from parameter "+c+": "+jwt);	
				all.add((String)jwt);
			}
		}
		// unpack any lists
		// since , [ are not valid base64, this is safe and easy
		ArraySet all2 = new ArraySet();
		for (String jwt : all) {
			if (jwt.startsWith("[")) {
				try {
					List jwts = (List) JSON.parse(jwt);
					all2.addAll(jwts);
				} catch (Exception ex) {
					Log.w(LOGTAG, "JWT parse error: "+ex+" from "+jwt);
				}
			} else if (jwt.contains(",")) {
				List jwts = StrUtils.splitOnComma(jwt);
				all2.addAll(jwts);
			} else {
				all2.add(jwt);
			}
		}
		// done
		return all2.asList();
	}

	public AuthToken login(String usernameUsuallyAnEmail, String password) {
		Utils.check4null(usernameUsuallyAnEmail, password);
		FakeBrowser fb = new FakeBrowser();
		Object response = fb.getPage(ENDPOINT, new ArrayMap(
				"app", app, 
				"action", "login",
				"person", usernameUsuallyAnEmail,
				"password", password));
		System.out.println(response);
		return null;
	}

	public AuthToken register(String usernameUsuallyAnEmail, String password) {
		Utils.check4null(usernameUsuallyAnEmail, password);
		FakeBrowser fb = new FakeBrowser();
		String response = fb.getPage(ENDPOINT, new ArrayMap(
				"app", app, 
				"action", "signup",
				"person", usernameUsuallyAnEmail,
				"password", password));
		Map jobj = (Map) JSON.parse(response);
		Map user = SimpleJson.get(jobj, "cargo", "user");
		return new AuthToken("TODO");
	}

	/**
	 * If uxid is specified use that (testing for a matching auth token!),
	 * otherwise return the first auth-token,
	 * or null.
	 * @return the user this request should be treated as being from.
	 */
	public XId getUserId(WebRequest state) {
		List<AuthToken> auths = getAuthTokens(state);
		return getUserId2(state, auths);
	}
	
	XId getUserId2(WebRequest state, List<AuthToken> auths) {
		XId uxid = state.get(new XIdField("uxid"));
		if (uxid==null) {
			// no user?
			if (auths==null || auths.isEmpty()) {
				return null;
			}			
			uxid = auths.get(0).xid;			
		} else {
			if (auths==null) throw new WebEx.E401(state.getRequestUrl(), "No auth-tokens. Can't act as "+uxid);
		}
		assert uxid != null;
		final XId fuxid = uxid;
		// FIXME security check
//		AuthToken auth = Containers.first(auths, a -> a.xid.equals(fuxid));
//		if (auth==null) {
//			throw new WebEx.E401(state.getRequestUrl(), "No auth-token for "+uxid);
//		}
		// set the user
		Properties user = new Properties(new ArrayMap("xid", uxid));
		state.setUser(uxid, user);
		// done
		return uxid;
	}

	public void setDebug(boolean b) {
		this.debug = b;
	}

}
