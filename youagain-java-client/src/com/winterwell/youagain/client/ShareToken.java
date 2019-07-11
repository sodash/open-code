package com.winterwell.youagain.client;

import java.util.Map;
import java.util.regex.Pattern;

import com.auth0.jwt.impl.PublicClaims;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.web.data.XId;

/**
 * Similar to an AuthToken. A ShareToken says "I (by=signer) share (app:item) with (_to=person/people) to read/write"
 * It can also say "I signer own item"
 * 
 * TODO expiry, purpose
 * 
 * @author daniel
 *
 */
public class ShareToken implements IHasJson {

	/**
	 * A token which can be verified with the YouAgain server.
	 * 
	 * Can be null for tracking-tokens
	 */
	String token;
	
	/**
	 * @return a JWT token
	 */
	public String getToken() {
		if (token!=null) return token;			
		try {
			JWTEncoder enc = new JWTEncoder(app);
			XId itemId = new XId(item,"share",false);
			Map<String, Object> props = new ArrayMap(
				PublicClaims.AUDIENCE, _to,
				"by", by, // NOT issuer, as that = app
				"r", read,
				"w", write,
//				"status", status ??
				PublicClaims.CONTENT_TYPE, type
			);
			token = enc.encryptJWT(itemId, props);
			return token;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}
	
	@Override
	public String toString() {
		return "ShareToken[app=" + app + ", item=" + item + ", by=" + by + ", _to=" + _to + "]";
	}

	
	public XId getTo() {
		return new XId(_to, false);
	}

	/**
	 * This does NOT test for whether it is valid! 
	 * @param app
	 * @param userId
	 * @param entity
	 * @return
	 */
	public static ShareToken newOwnershipClaim(String app, XId userId, String entity) {
		return new ShareToken(app, userId, entity);
	}
	
	
	@Deprecated // for deserialisation
	public ShareToken() {	
	}
	
	public ShareToken(String app, XId userId, String entity, XId shareWith) {
		Utils.check4null(app, userId, entity, shareWith);
		this.app = app;
		this.by = userId.toString();
		setItem(entity);
		this._to= shareWith.toString();
		this.read = true; // you almost always want read
	}


	private void setItem(String entity) {
		this.item = entity;
		// type? e.g. "role:editor" has type "role"
		Pattern ptype = Pattern.compile("^(\\w+):");
		String[] found = StrUtils.find(ptype, item);
		if (found!=null) {
			this.type = found[1];
		}
	}

	private ShareToken(String app, XId userId, String entity) {
		Utils.check4null(app, userId, entity);		
		this.app = app;
		this.by = OWNER;
		setItem(entity);
		this._to= userId.toString();
		this.read = true;
		this.write = true;
	}

	public static final String OWNER = "!owner";
	
	String app;
	
	/**
	 * A path or XId, E.g. /myfolder/myfile, or winterstein@twitter
	 */
	String item;
	
	/**
	 * TODO If item starts foo:bar, then store "foo" here for fast search.
	 */
	transient String type;
	
	public String getItem() {
		return item;
	}
	
	/**
	 * XId of the person sharing.  Or {@link #OWNER} to assert ownership
	 */
	String by;
	
	/**
	 * XId of the person shared with.
	 * NB: "to" is an SQL keyword, so best avoided.
	 */
	String _to;
	
	Integer status;
	
	boolean read = true;
	
	boolean write = true;

	@Override
	public Map toJson2() throws UnsupportedOperationException {
		return new ArrayMap(
				"_to", _to,
				"app", app,
				"by", by,
				"item", item,
				"read", read,
				"status", status,
				"type", type,
				"write", write
		);
	}
}
