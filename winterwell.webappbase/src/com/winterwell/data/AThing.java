package com.winterwell.data;

import com.winterwell.depot.IInit;

import lombok.Data;

/**
 * Base class for things.
 * 
 * Goal: loosely base on https://schema.org/Thing
 * @author daniel
 *
 */
@Data
public class AThing implements IInit {

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
	
	
}


