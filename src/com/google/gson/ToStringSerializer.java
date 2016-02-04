package com.google.gson;

import java.lang.reflect.Type;

import winterwell.utils.web.IHasJson;

/**
 * COnvert to JSON -- by just outputting the toString() string.
 * Use-case: for when you want to avoid creating objects.
 * @author daniel
 */
public class ToStringSerializer implements JsonSerializer{

	@Override
	public JsonElement serialize(Object src, Type typeOfSrc,
			JsonSerializationContext context) {
		return new JsonPrimitive(src.toString());
	}
	
}
