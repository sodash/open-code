package com.winterwell.gson;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

import com.winterwell.gson.stream.JsonReader;
import com.winterwell.gson.stream.JsonToken;
import com.winterwell.gson.stream.JsonWriter;
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
 * Treat any CharSequence class like a String
 * Warning: This loses the type info! 
 * Use-case: good for special "type-safe" String-like classes, e.g. Dataspace
 * Experimental: This can handle flexible time inputs, like "tomorrow". 
 * But ISO format yyyy-mm-dd is strongly recommended!
 * @author daniel
 */
public static final class CharSequenceTypeAdapter implements JsonSerializer<CharSequence>, JsonDeserializer<CharSequence> {

	private Class<? extends CharSequence> klass;
	private Constructor<? extends CharSequence> scon;
	
	
	public CharSequenceTypeAdapter(Class<? extends CharSequence> klass) {
		this.klass = klass;
		try {
			scon = klass.getConstructor(String.class);			
		} catch (NoSuchMethodException e) {
			try {
				scon = klass.getConstructor(CharSequence.class);
			} catch (NoSuchMethodException e1) {
				throw Utils.runtime(e);
			}			
		}
		scon.setAccessible(true);
	}

	@Override
	public CharSequence deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException 
	{
		String s = null;
		try {
			s = json.getAsString();					
			CharSequence ni = scon.newInstance(s);
			return ni;
		} catch(Exception ex) {
			throw new JsonParseException(s, ex);
		}
	}

	@Override
	public JsonElement serialize(CharSequence src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(src.toString());		
	}
}

/**
 * Can coerce floating points into Longs -- this allows for robustness against floating point issues.
 * Can be registered for Long.class and/or long.class  -- they are separate.
 * @author daniel
 *
 */
public static class LenientLongAdapter extends TypeAdapter<Long>{
	
	private final Long nullValue;

	public LenientLongAdapter() {
		this(null);
	}
	
	/**
	 * @param nullValue 0 or null
	 */
	public LenientLongAdapter(Long nullValue) {
		this.nullValue = nullValue;
		assert nullValue == null || nullValue == 0 : nullValue;
	}
    
	@Override
    public Long read(JsonReader reader) throws IOException {
        if(reader.peek() == JsonToken.NULL){
            reader.nextNull();
            return nullValue;
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
        	if (nullValue==null) {
        		writer.nullValue();
        	} else {
        		writer.value(nullValue);
        	}
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
