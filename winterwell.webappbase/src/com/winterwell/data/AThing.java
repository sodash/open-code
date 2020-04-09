package com.winterwell.data;

import java.util.List;

import com.winterwell.depot.IInit;
import com.winterwell.es.ESKeyword;
import com.winterwell.utils.Dep;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.web.data.IHasXId;
import com.winterwell.web.data.XId;
import com.winterwell.youagain.client.ShareToken;
import com.winterwell.youagain.client.YouAgainClient;

/**
 * Base class for things.
 * 
 * Goal: loosely base on https://schema.org/Thing
 * @author daniel
 *
 */
//@Data Lombok is nice, but not using it makes builds more robust
public class AThing implements IInit {

	/**
	 * Note: sadly this is not present on data before April 2020
	 */
	Time created = new Time();
	
	/**
	 * 
	 * @return default pattern: if id has an @, use it - otherwise add "@type.good-loop.com"
	 */
	public XId getXId() {
		if (XId.XID_PATTERN.matcher(getId()).matches()) {
			return new XId(getId());
		}
		String type = getClass().getSimpleName().toLowerCase();
		String service = type+".good-loop.com";
		return new XId(getId(), service, false);
	}

	
	public void setShares(List<ShareToken> shares) {
		this.shares = shares;
	}
	
	/**
	 * @deprecated YA is the definitive source. This is a cache.
	 * @return
	 */
	public List<ShareToken> getShares() {
		return shares;
	}
	
	/**
	 * @deprecated TODO fill in from ES
	 */
	transient int _version;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		if (this.id!=null && ! this.id.equals("new")) {
			Log.w(LOGTAG(), "Change id from "+this.id+" to "+id+" "+ReflectionUtils.getSomeStack(10));
		}
		this.id = id;
	}

	protected String LOGTAG() {
		return getClass().getSimpleName();
	}

	/**
	 * Set the local flag. Does NOT trigger a save or any database level action.
	 * @param status
	 */
	public void setStatus(KStatus status) {
		this.status = status;
	}

	public String name;
	
	@ESKeyword
	public String url;
	/**
	 * Normally an XId (so that it has service, and is in a canonical form for that service)
	 */
	public String id;
	KStatus status;
	
	/**
	 * Cache of shares - YouAgain is the definitive source, but we can store in
	 * the DB for speedy filtering.
	 * 
	 * TODO have YA make calls to set these (using an endpoint registered against an app).
	 * TODO but the app should have logic to create shares, e.g. "an anon-xid donation is shared with the donor's email"
	 */
	private List<ShareToken> shares;
	
	/**
	 * Check (and patch) the data in this Thing.
	 * @return this
	 */
	public void init() {		
	}
	
	public KStatus getStatus() {
		return status;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"[name=" + name + ", id=" + id + ", status=" + status
				+ "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AThing other = (AThing) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}
	
	
}


