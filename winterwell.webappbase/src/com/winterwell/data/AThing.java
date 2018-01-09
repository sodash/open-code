package com.winterwell.data;

import com.winterwell.depot.IInit;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.log.Log;

import lombok.Data;

/**
 * Base class for things.
 * 
 * Goal: loosely base on https://schema.org/Thing
 * @author daniel
 *
 */
//@Data Lombok is nice, but not using it makes builds more robust
public class AThing implements IInit {

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
		if (this.id!=null) {
			Log.w(LOGTAG(), "Change id from "+this.id+" to "+id+" "+ReflectionUtils.getSomeStack(10));
		}
		this.id = id;
	}

	protected String LOGTAG() {
		return getClass().getSimpleName();
	}

	public void setStatus(KStatus status) {
		this.status = status;
	}

	public String name;
	public String url;
	public String id;
	KStatus status;
	/**
	 * @deprecated
	 */
	Boolean modified;
	
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
		return getClass().getSimpleName()+"[name=" + name + ", id=" + id + ", status=" + status + ", modified=" + modified
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


