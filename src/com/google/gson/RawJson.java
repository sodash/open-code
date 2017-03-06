package com.google.gson;

public final class RawJson extends JsonElement {

	public RawJson(String json) {
		this.json = json;
	}
	
	public final String json;

	@Override
	public String toString() {	
		return json;
	}

	@Override
	JsonElement deepCopy() {
		return this;
	}
	
}
