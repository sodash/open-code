//package com.goodloop.data;
//
//import java.lang.reflect.Type;
//
//import com.winterwell.gson.JsonDeserializationContext;
//import com.winterwell.gson.JsonDeserializer;
//import com.winterwell.gson.JsonElement;
//import com.winterwell.gson.JsonObject;
//import com.winterwell.gson.JsonParseException;
//import com.winterwell.gson.JsonPrimitive;
//import com.winterwell.gson.JsonSerializer;
//import com.winterwell.gson.TypeAdapter;
//import com.winterwell.utils.Utils;
//import com.winterwell.utils.time.Time;
//import com.winterwell.utils.time.TimeUtils;
//
//public class MoneyTypeAdapter implements 
////JsonSerializer<Time>, 
//JsonDeserializer<Money> {
//
//	@Override
//	public Money deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
//			throws JsonParseException 
//	{
//		context.
//		JsonObject jobj = (JsonObject) json;
//		JsonPrimitive ut = jobj.getAsJsonPrimitive("ut");
//		long utv = ut.getAsLong();
//		return new Time(utv);
//	}
//
//}
