package com.winterwell.gson.internal.bind;

import static com.winterwell.gson.internal.bind.JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter;

import java.io.IOException;
import java.lang.reflect.Field;

import com.winterwell.gson.Gson;
import com.winterwell.gson.TypeAdapter;
import com.winterwell.gson.annotations.JsonAdapter;
import com.winterwell.gson.internal.ConstructorConstructor;
import com.winterwell.gson.internal.Primitives;
import com.winterwell.gson.reflect.TypeToken;
import com.winterwell.gson.stream.JsonReader;
import com.winterwell.gson.stream.JsonWriter;

/**
 * 		// special casing primitives here saves ~5% on Android...
 * @author daniel
 *
 */
final class ReflectiveTypeAdapterBoundField extends ReflectiveTypeAdapterFactory.BoundField {
	
	private final boolean isPrimitive;
	private final TypeAdapter typeAdapter;
	private final Field field;
	private final TypeToken fieldType;
	private final Gson gson;
	private final ConstructorConstructor conCon; 
	
	ReflectiveTypeAdapterBoundField(String name, boolean serialized, boolean deserialized, 
			Gson gson, Field field, TypeToken<?> fieldType, ConstructorConstructor constructorConstructor) 
	{
		super(name, serialized, deserialized);
		this.gson = gson;
		this.field = field;
		this.fieldType = fieldType;
		this.conCon = constructorConstructor;
		isPrimitive = Primitives.isPrimitive(fieldType.getRawType());
		// special casing primitives here saves ~5% on Android...
		typeAdapter = getFieldAdapter(gson, field, fieldType);				
	}
	

	private TypeAdapter<?> getFieldAdapter(Gson gson, Field field,
			TypeToken<?> fieldType) {
		JsonAdapter annotation = field.getAnnotation(JsonAdapter.class);
		if (annotation != null) {
			TypeAdapter<?> adapter = getTypeAdapter(conCon, gson, fieldType, annotation);
			if (adapter != null)
				return adapter;
		}
		return gson.getAdapter(fieldType);
	}
	
	@Override
	void write(JsonWriter writer, Object value) throws IOException,
			IllegalAccessException {
		Object fieldValue = field.get(value);
		TypeAdapter t = new TypeAdapterRuntimeTypeWrapper(gson,
				this.typeAdapter, fieldType.getType());
		t.write(writer, fieldValue);
	}

	@Override
	void read(JsonReader reader, Object value) throws IOException, IllegalAccessException {
		Object fieldValue = typeAdapter.read(reader);
		if (fieldValue != null || !isPrimitive) {
			field.set(value, fieldValue);
		}
	}

}
