package com.winterwell.gson;

import java.io.IOException;
import java.lang.reflect.Type;

import com.winterwell.gson.stream.JsonReader;
import com.winterwell.gson.stream.JsonToken;
import com.winterwell.gson.stream.JsonWriter;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.IHasJson;

/**
 * TODO move some of our adapters in here for our convenience
 * @author daniel
 * @testedby {@link StandardAdaptersTest}
 */
public class StandardAdapters {


	public static final JsonSerializer IHASJSONADAPTER = new JsonSerializer<IHasJson>() {
		@Override
		public JsonElement serialize(IHasJson src, Type typeOfSrc, JsonSerializationContext context) {
			return context.serialize(src.toJson2());
		}
	};


	

/**
 * Time <-> iso-string
 * Warning: This loses the type info! 
 * It looks cleaner, but the inverse only works if the field is of type Time (and it is is slightly slower). 
 * Use-case: good for Elastic-Search
 * Experimental: This can handle flexible time inputs, like "tomorrow". 
 * But ISO format yyyy-mm-dd is strongly recommended!
 * @author daniel
 */
public static class TimeTypeAdapter implements JsonSerializer<Time>, JsonDeserializer<Time> {
	@Override
	public JsonElement serialize(Time src, Type srcType,
			JsonSerializationContext context) {
		return new JsonPrimitive(src.toISOString());
	}

	@Override
	public Time deserialize(JsonElement json, Type type,
			JsonDeserializationContext context) throws JsonParseException {
		if (json.isJsonObject()) {
			// a vanilla Gson will turn Time into {ut: }
			JsonObject jobj = (JsonObject) json;
			JsonPrimitive ut = jobj.getAsJsonPrimitive("ut");
			long utv = ut.getAsLong();
			return new Time(utv);
		}
		String s = json.getAsString();
		if (Utils.isBlank(s)) {
			return null;
		}
		Time t = TimeUtils.parseExperimental(s);
		return t;
	}
}

/**
 * Can coerce floating points into Longs -- this allows for robustness against floating point issues.
 * Can be registered for Long.class and/or long.class  -- they are separate.
 * @author daniel
 *
 */
public static class LenientLongAdapter extends TypeAdapter<Long>{
    
	@Override
    public Long read(JsonReader reader) throws IOException {
        if(reader.peek() == JsonToken.NULL){
            reader.nextNull();
            return null; // should this be 0?
        }
        String stringValue = reader.nextString();
        try{
            Long value = Long.valueOf(stringValue);
            return value;
        } catch(NumberFormatException e){
        	Double v = Double.valueOf(stringValue);
            return (long) Math.round(v);
        }
    }
    
    @Override
    public void write(JsonWriter writer, Long value) throws IOException {
        if (value == null) {
            writer.nullValue();
            return;
        }
        writer.value(value);
    }
}

	/**
	 * @deprecated Not sure why we have this!
	 * @author daniel
	 */
	public static class ClassTypeAdapter implements JsonSerializer<Class>,
			JsonDeserializer<Class> {
		@Override
		public JsonElement serialize(Class src, Type srcType,
				JsonSerializationContext context) {
			return new JsonPrimitive(src.getCanonicalName());
		}
	
		@Override
		public Class deserialize(JsonElement json, Type type,
				JsonDeserializationContext context) throws JsonParseException {
			try {
				return Class.forName(json.getAsString());
			} catch (ClassNotFoundException e) {
				throw new JsonParseException(e);
			}
		}
	}


}
