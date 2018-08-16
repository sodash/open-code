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

import java.io.IOException;
import java.util.Map;

import com.winterwell.gson.Gson;
import com.winterwell.gson.JsonElement;
import com.winterwell.gson.JsonObject;
import com.winterwell.gson.JsonParseException;
import com.winterwell.gson.JsonPrimitive;
import com.winterwell.gson.TypeAdapter;
import com.winterwell.gson.TypeAdapterFactory;
import com.winterwell.gson.internal.Primitives;
import com.winterwell.gson.internal.Streams;
import com.winterwell.gson.reflect.TypeToken;
import com.winterwell.gson.stream.JsonReader;
import com.winterwell.gson.stream.JsonWriter;

/**
 * Adapted from gson/extras by Daniel/Winterwell
 * Status: experimental
 * 
 *  */
public final class RuntimeTypeAdapterFactory<T> implements TypeAdapterFactory {
//	private final Gson gson;
	private final String classProperty;

  public RuntimeTypeAdapterFactory(String classProperty) {
    if (classProperty == null) {
      throw new NullPointerException();
    }
    this.classProperty = classProperty;
  }

  public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> type) {
	  if (Primitives.isPrimitive(type.getType()) || Primitives.isWrapperType(type.getType())) {
		  return null;
	  }
	TypeAdapter<R> delegate = gson.getDelegateAdapter(this, type);	  
    return new RTA<R>(classProperty, delegate, type);
  }
}

final class RTA<R> extends TypeAdapter<R> {
    private final String classProperty;
	private final TypeAdapter<R> delegate;
	private TypeToken<R> type;

	public RTA(String classProperty, TypeAdapter<R> delegate, TypeToken<R> type) {
    	this.classProperty = classProperty;
    	this.delegate = delegate;
    	this.type = type;
	}

	@Override public R read(JsonReader in) throws IOException {
        JsonElement jsonElement = Streams.parse(in);
        JsonElement labelJsonElement = jsonElement.getAsJsonObject().remove(classProperty);
        if (labelJsonElement == null) {
          throw new JsonParseException("cannot deserialize " + jsonElement
              + " because it does not define a field named " + classProperty);
        }
        String label = labelJsonElement.getAsString();
        Object r = delegate.fromJsonTree(jsonElement);
        return (R) r;
      }

      @Override public void write(JsonWriter out, R value) throws IOException {
        Class<?> srcType = value.getClass();
		// anonymous classes return null here
		String cName = value.getClass().getCanonicalName();
		// We can't do anything sensible at de-serialisation with
		// member-classes.
		// So leave it for the object->map adapter
		boolean mc = value.getClass().isMemberClass();
		if (mc || cName==null || value.getClass()==Object.class) {
			delegate.write(out, value);
			return;
		}
		JsonElement jtree = delegate.toJsonTree(value);
		JsonObject jsonObject = jtree.getAsJsonObject();
        if (jsonObject.has(classProperty)) {
          throw new JsonParseException("cannot serialize " + srcType.getName()
              + " because it already defines a field named " + classProperty);
        }
        JsonObject clone = new JsonObject();
        clone.add(classProperty, new JsonPrimitive(cName));
        for (Map.Entry<String, JsonElement> e : jsonObject.entrySet()) {
          clone.add(e.getKey(), e.getValue());
        }
        Streams.write(clone, out);
      }
}
