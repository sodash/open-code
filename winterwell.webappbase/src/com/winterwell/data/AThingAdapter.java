package com.winterwell.data;

import java.lang.reflect.Type;
import java.util.Map;

import com.winterwell.gson.FlexiGson;
import com.winterwell.gson.JsonDeserializationContext;
import com.winterwell.gson.JsonDeserializer;
import com.winterwell.gson.JsonElement;
import com.winterwell.gson.JsonObject;
import com.winterwell.gson.JsonParseException;
import com.winterwell.gson.JsonSerializationContext;
import com.winterwell.gson.JsonSerializer;
import com.winterwell.web.data.XId;

/**
 * serialize: adds "@type"
 * 
 * deserialize: calls init()
 * 
 * @author daniel
 *
 */
public class AThingAdapter implements JsonSerializer<AThing>, JsonDeserializer<AThing> {

	@Override
	public String toString() {
		return "AThingAdapter";
	}

	@Override
	public AThing deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		AThing thing = context.deserialize(json, typeOfT);
		thing.init();
		return thing;
	}

	@Override
	public JsonElement serialize(AThing src, Type typeOfSrc, JsonSerializationContext context) {
		// serialize as a pojo
		JsonObject je = (JsonObject) context.serialize(src, Object.class);
		String type = src.getClass().getSimpleName();
		je.addProperty("@type", type);
		return je;
	}

}
