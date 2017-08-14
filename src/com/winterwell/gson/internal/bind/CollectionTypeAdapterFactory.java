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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.winterwell.gson.Gson;
import com.winterwell.gson.TypeAdapter;
import com.winterwell.gson.TypeAdapterFactory;
import com.winterwell.gson.internal.$Gson$Types;
import com.winterwell.gson.internal.ConstructorConstructor;
import com.winterwell.gson.internal.ObjectConstructor;
import com.winterwell.gson.reflect.TypeToken;
import com.winterwell.gson.stream.JsonReader;
import com.winterwell.gson.stream.JsonToken;
import com.winterwell.gson.stream.JsonWriter;

/**
 * Adapt a homogeneous collection of objects.
 */
public final class CollectionTypeAdapterFactory implements TypeAdapterFactory {
	private final ConstructorConstructor constructorConstructor;

	public CollectionTypeAdapterFactory(
			ConstructorConstructor constructorConstructor) {
		this.constructorConstructor = constructorConstructor;
	}

	public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
		Type type = typeToken.getType();

		Class<? super T> rawType = typeToken.getRawType();
		if (!Collection.class.isAssignableFrom(rawType)) {
			return null;
		}

		Type elementType = $Gson$Types.getCollectionElementType(type, rawType);
		TypeAdapter<?> elementTypeAdapter = gson.getAdapter(TypeToken
				.get(elementType));
		ObjectConstructor<T> constructor = constructorConstructor
				.get(typeToken);

		@SuppressWarnings({ "unchecked", "rawtypes" })
		// create() doesn't define a type parameter
		TypeAdapter<T> result = new Adapter(gson, elementType,
				elementTypeAdapter, constructor);
		return result;
	}

	private static final class Adapter<E> extends TypeAdapter<Collection<E>> {
		private final TypeAdapter<E> elementTypeAdapter;
		private final ObjectConstructor<? extends Collection<E>> constructor;

		public Adapter(Gson context, Type elementType,
				TypeAdapter<E> elementTypeAdapter,
				ObjectConstructor<? extends Collection<E>> constructor) {
			this.elementTypeAdapter = new TypeAdapterRuntimeTypeWrapper<E>(
					context, elementTypeAdapter, elementType);
			this.constructor = constructor;
		}

		public Collection<E> read(JsonReader in) throws IOException {
			if (in.peek() == JsonToken.NULL) {
				in.nextNull();
				return null;
			}

			Collection<E> collection = constructor.construct();
			// Winterwell modification: Let's also handle integer-keyed objects, since js treats these as basically equivalent to arrays. ^DW
			if (in.peek() == JsonToken.BEGIN_OBJECT) {
				// object-as-array
				in.beginObject();
				List list = new ArrayList();
				while (in.hasNext()) {
					// Does not assume the keys are in order.
					// This is slightly inefficient for the normal case, where the keys are in order. 
					String pname = in.nextName();
					int index = Integer.parseInt(pname);
					E instance = elementTypeAdapter.read(in);
					if (index <list.size()) {
						list.set(index, instance);
					} else {
						while(list.size() < index) list.add(null);
						list.add(instance);
					}
				}
				in.endObject();
				collection.addAll(list);
			} else {
				// End of Winterwell modification
				// normal array read
				in.beginArray();
				while (in.hasNext()) {
					E instance = elementTypeAdapter.read(in);
					collection.add(instance);
				}
				in.endArray();
			}
			return collection;
		}

		public void write(JsonWriter out, Collection<E> collection)
				throws IOException {
			if (collection == null) {
				out.nullValue();
				return;
			}

			out.beginArray();
			for (E element : collection) {
				elementTypeAdapter.write(out, element);
			}
			out.endArray();
		}
	}
}
