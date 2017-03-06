package com.google.gson;

import java.util.function.Supplier;

/**
 * Wrapper for getting a fairly vanilla Gson, with NO @class class properties
 * @author daniel
 *
 */
public class PlainGson implements Supplier<Gson> {

	private final Gson gson;

	public PlainGson() {
		this(
				new GsonBuilder().setClassProperty(null).create()
			);
	}
	
	public PlainGson(Gson gson) {
		this.gson = gson;
	}
	
	public Gson get() {
		return gson;
	}
	
}
