package com.winterwell.depot;

import com.winterwell.utils.web.XStreamUtils;

public class ESStoreWrapper {

	private transient Object artifact;

	public ESStoreWrapper(Object artifact) {
		this.artifact = artifact;
		// Base 64 encode too??
		this.raw = XStreamUtils.serialiseToXml(artifact);
	}

	public String raw;

}
