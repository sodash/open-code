package com.winterwell.youagain.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.mail.internet.InternetAddress;

import org.eclipse.jetty.util.ajax.JSON;
import org.jose4j.jwa.AlgorithmFactoryFactory;
import org.jose4j.jwt.JwtClaims;
import org.junit.runner.notification.RunListener.ThreadSafe;

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
import com.winterwell.web.ajax.AjaxMsg;
import com.winterwell.web.ajax.AjaxMsg.KNoteType;
import com.winterwell.web.ajax.JSend;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.data.XId;
import com.winterwell.web.fields.XIdField;

/**
 * Query YouAgain from Java. E.g. check that tokens are valid.
 * 
 * This is a thread-safe and lightweight object.
 * 
 * @author daniel
 */
public final class YouAgainClient {

	public static XId xidFromEmail(String email) {
		return new XId(WebUtils2.canonicalEmail(email), "email");
	}
	
	public static XId xidFromEmail(InternetAddress email) {
		return new XId(WebUtils2.canonicalEmail(email), "email");
	}

	
	static final String ENDPOINT = 
				"https://youagain.good-loop.com/youagain.json";
//				"http://localyouagain.winterwell.com/youagain.json";

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
	 * This is the method you want :)
	 * 
	 * This will also call state.setUser(). 
	 * Caches the return so repeated calls are fast.
	 * @param state
	 * @return null if not logged in at all, otherwise list of AuthTokens.
	 * WARNING: This can include anonymous temporary "nonce@temp" tokens!
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
		tokens = verify(jwt, state);
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
		Map user = userFromResponse(response);
		AuthToken at = new AuthToken(user);
		return at;
	}

	/**
	 * 
	 * @param jwt
	 * @param state Can be null. For sending messages back
	 * @return verified auth tokens and unverified nonce@temp tokens
	 */
	public List<AuthToken> verify(List<String> jwt, WebRequest state) {
		Log.d(LOGTAG, "verify: "+jwt);
		List<AuthToken> list = new ArrayList();
		if (jwt.isEmpty()) return list;
		for (String jt : jwt) {
			try {
				AuthToken token = new AuthToken(jt);
				// HACK include temp tokens!
				if (isTempId(jt)) {
					token.xid = new XId(jt, false);
					list.add(token);
					continue;
				}
				// TODO a better appraoch would be for the browser to make a proper JWT for @temp
				// decode the token
				JWTDecoder dec = getDecoder(); //"local".equals(state.get("login")));
				JwtClaims decd = dec.decryptJWT(jt);
				token.xid = new XId(decd.getSubject(), false);
				list.add(token);
			} catch (Throwable e) {
				Log.w(LOGTAG, e);
				// pass back to the user but keep on trucking
				if (state!=null) {
					state.addMessage(new AjaxMsg(KNoteType.warning, "JWT token error", e.toString()));
				}
			}
		}
		return list;		
	}

	private boolean isTempId(String jt) {
		return jt!=null && jt.endsWith("@temp");
	}


	static JWTDecoder dec = new JWTDecoder();
	
	private JWTDecoder getDecoder() throws Exception {
		if (dec.getPublicKey()==null) {
			String publickeyEndpoint = ENDPOINT.replace("youagain.json", "publickey");
			// load from the server, so we could change keys
			String pkey = new FakeBrowser().getPage(publickeyEndpoint);
//			String pkey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu3njghYwWN8Bf/f6FndCr3h3/uzPNctZZf2qLHqGicZaQQvjMqFfr2tz/JGsFkxeCSeEuLqzHjBoc8P9S2aKb7X04b/OfTJkSjH/5UArKuAGZL1/hVFwZwnSoQOhklElHtq/RwGUgemmu7QIjTcgKEINUNzC537vWOtiQkWAO/abqwpfQgKPvMNvViPMrJtk8A07bFetQKjU4A6do6E3BvTItzgMZJLmMVePn8Yo3uH/7rLtKybl2tn8BhOWPGLnEyZiPZ2f8V/56hR1zsHr9i9QMjLX8O18+w4pno04jST2Yp7yxTNN3mttqgyl8s8oFMptSG2/3g+WqwwwBTbGgQIDAQAB";			
			dec.setPublicKey(pkey);			
		}
		return dec;
	}

	/**
	 * Low-level access to JWT tokens. Use {@link #getAuthTokens(WebRequest)} instead.
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
		Pattern KEY = Pattern.compile("^([a-zA-Z0-9\\-_]+\\.)?jwt");
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

	public AuthToken login(String usernameUsuallyAnEmail, String password) throws LoginFailedException {
		Utils.check4null(usernameUsuallyAnEmail, password);
		FakeBrowser fb = new FakeBrowser();
		String response = fb.getPage(ENDPOINT, new ArrayMap(
				"app", app, 
				"action", "login",
				"person", usernameUsuallyAnEmail,
				"password", password));
		Map user = userFromResponse(response);
		AuthToken at = new AuthToken(user);
		return at;
	}

	public AuthToken register(String usernameUsuallyAnEmail, String password) {
		Utils.check4null(usernameUsuallyAnEmail, password);
		FakeBrowser fb = new FakeBrowser();
		String response = fb.getPage(ENDPOINT, new ArrayMap(
				"app", app, 
				"action", "signup",
				"person", usernameUsuallyAnEmail,
				"password", password));
		Map user = userFromResponse(response);
		AuthToken at = new AuthToken(user);
		return at;
	}

	private Map userFromResponse(String response) {
		JSend jsend = JSend.parse(response);
		Map cargo = jsend.getData().map();
		Map user = (Map) cargo.get("user");
		return user;
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
	
	/**
	 * also sets state.setUser()
	 * @param state
	 * @param auths
	 * @return
	 */
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
	
	
	public List<String> getSharedWith(String authToken) {
		FakeBrowser fb = new FakeBrowser();
		fb.setAuthenticationByJWT(authToken);
		String response = fb.getPage(ENDPOINT, new ArrayMap(
				"app", app, 
				"action", "shared-with"));
		
		Map jobj = (Map) JSON.parse(response);
		Object shares = SimpleJson.get(jobj, "cargo");
		if (shares instanceof Object[]) {
			return Arrays.stream((Object[]) shares).map(share -> (String) SimpleJson.get(share, "item")).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}
	
	public boolean share(String authToken, String targetUser, String item) {
		FakeBrowser fb = new FakeBrowser();
		fb.setAuthenticationByJWT(authToken);
		String response = fb.getPage(ENDPOINT, new ArrayMap(
				"app", app,
				"shareWith", targetUser,
				"entity", item,
				"action", "shared"));
		JSend jsend = JSend.parse(response);
		return jsend.isSuccess();
	}

	public void setDebug(boolean b) {
		this.debug = b;
	}

}
