/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.winterwell.gson.internal.bind;

import static com.winterwell.gson.internal.bind.JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import com.winterwell.gson.FieldNamingStrategy;
import com.winterwell.gson.Gson;
import com.winterwell.gson.JsonSyntaxException;
import com.winterwell.gson.KLoopPolicy;
import com.winterwell.gson.TypeAdapter;
import com.winterwell.gson.TypeAdapterFactory;
import com.winterwell.gson.annotations.JsonAdapter;
import com.winterwell.gson.annotations.SerializedName;
import com.winterwell.gson.internal.$Gson$Types;
import com.winterwell.gson.internal.ConstructorConstructor;
import com.winterwell.gson.internal.Excluder;
import com.winterwell.gson.internal.ObjectConstructor;
import com.winterwell.gson.internal.Primitives;
import com.winterwell.gson.reflect.TypeToken;
import com.winterwell.gson.stream.JsonReader;
import com.winterwell.gson.stream.JsonToken;
import com.winterwell.gson.stream.JsonWriter;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.ReflectionUtils;

/**
 * Type adapter that reflects over the fields and methods of a class.
 */
public final class ReflectiveTypeAdapterFactory implements TypeAdapterFactory {
	private final ConstructorConstructor constructorConstructor;
	private final FieldNamingStrategy fieldNamingPolicy;
	private final Excluder excluder;
	private String classProperty;

	public ReflectiveTypeAdapterFactory(
			ConstructorConstructor constructorConstructor,
			FieldNamingStrategy fieldNamingPolicy, Excluder excluder,
			String classProperty) {
		this.constructorConstructor = constructorConstructor;
		this.fieldNamingPolicy = fieldNamingPolicy;
		this.excluder = excluder;
		this.classProperty = classProperty;
	}

	public boolean excludeField(Field f, boolean serialize) {
		return !excluder.excludeClass(f.getType(), serialize)
				&& !excluder.excludeField(f, serialize);
	}

	private String getFieldName(Field f) {
		SerializedName serializedName = f.getAnnotation(SerializedName.class);
		return serializedName == null ? fieldNamingPolicy.translateName(f)
				: serializedName.value();
	}

	public <T> TypeAdapter<T> create(Gson gson, final TypeToken<T> type) {
		Class<? super T> raw = type.getRawType();

		if (!Object.class.isAssignableFrom(raw)) {
			return null; // it's a primitive!
		}
		
		// TODO ?? guard against stuff we can't handle
//		Class<? super T> rawType = type.getRawType();
//		if (classProperty!=null && rawType.getCanonicalName()==null || rawType.isMemberClass()) {
//			return ObjectTypeAdapter.FACTORY.create(gson, type);
//		}

		ObjectConstructor<T> constructor = constructorConstructor.get(type);
		// TODO pass through more
		ReflectiveTypeAdapter<T> adapter = new ReflectiveTypeAdapter<T>(gson, this, type, constructor,
				getBoundFields(gson, type, raw), constructorConstructor, classProperty);
		return adapter;
	}

	private ReflectiveTypeAdapterFactory.BoundField createBoundField(
			final Gson context, final Field field, final String name,
			final TypeToken<?> fieldType, boolean serialize, boolean deserialize) 
	{	
		return new ReflectiveTypeAdapterBoundField(name, serialize, deserialize,
				context, field, fieldType, constructorConstructor);
	}


	private Map<String, BoundField> getBoundFields(Gson context,
			TypeToken<?> type, Class<?> raw) {
		Map<String, BoundField> result = new LinkedHashMap<String, BoundField>();
		if (raw.isInterface()) {
			return result;
		}

		Type declaredType = type.getType();
		while (raw != Object.class) {
			Field[] fields = raw.getDeclaredFields();
			for (Field field : fields) {
				boolean serialize = excludeField(field, true);
				boolean deserialize = excludeField(field, false);
				if (!serialize && !deserialize) {
					continue;
				}
				field.setAccessible(true);
				Type fieldType = $Gson$Types.resolve(type.getType(), raw,
						field.getGenericType());
				BoundField boundField = createBoundField(context, field,
						getFieldName(field), TypeToken.get(fieldType),
						serialize, deserialize);
				BoundField previous = result.put(boundField.name, boundField);
				if (previous != null) {
					throw new IllegalArgumentException(declaredType
							+ " declares multiple JSON fields named "
							+ previous.name);
				}
			}
			type = TypeToken.get($Gson$Types.resolve(type.getType(), raw,
					raw.getGenericSuperclass()));
			raw = type.getRawType();
		}
		return result;
	}

	static abstract class BoundField {
		final String name;
		final boolean serialized;
		final boolean deserialized;

		@Override
		public String toString() {
			return "BoundField[" + name + "]";
		}

		protected BoundField(String name, boolean serialized,
				boolean deserialized) {
			this.name = name;
			this.serialized = serialized;
			this.deserialized = deserialized;
		}

		abstract void write(JsonWriter writer, Object value)
				throws IOException, IllegalAccessException;

		abstract void read(JsonReader reader, Object value) throws IOException,
				IllegalAccessException;
	}
}