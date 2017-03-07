/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.winterwell.gson;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.winterwell.gson.internal.$Gson$Preconditions;
import com.winterwell.gson.internal.ConstructorConstructor;
import com.winterwell.gson.internal.Excluder;
import com.winterwell.gson.internal.Primitives;
import com.winterwell.gson.internal.Streams;
import com.winterwell.gson.internal.bind.ArrayTypeAdapter;
import com.winterwell.gson.internal.bind.CollectionTypeAdapterFactory;
import com.winterwell.gson.internal.bind.DateTypeAdapter;
import com.winterwell.gson.internal.bind.EnumMapTypeAdapter;
import com.winterwell.gson.internal.bind.JsonAdapterAnnotationTypeAdapterFactory;
import com.winterwell.gson.internal.bind.JsonTreeReader;
import com.winterwell.gson.internal.bind.JsonTreeWriter;
import com.winterwell.gson.internal.bind.LateBinding;
import com.winterwell.gson.internal.bind.MapTypeAdapterFactory;
import com.winterwell.gson.internal.bind.ObjectTypeAdapter;
import com.winterwell.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.winterwell.gson.internal.bind.RuntimeTypeAdapterFactory;
import com.winterwell.gson.internal.bind.SqlDateTypeAdapter;
import com.winterwell.gson.internal.bind.TimeTypeAdapter;
import com.winterwell.gson.internal.bind.TypeAdapters;
import com.winterwell.gson.reflect.TypeToken;
import com.winterwell.gson.stream.JsonReader;
import com.winterwell.gson.stream.JsonToken;
import com.winterwell.gson.stream.JsonWriter;
import com.winterwell.gson.stream.MalformedJsonException;

/**
 * Gson, with a classname that won't conflict with other jars 
 * @author Daniel Winterstein
 */
public final class FlexiGson extends Gson {
}

