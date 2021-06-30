package com.winterwell.utils.web;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.winterwell.utils.FailureException;
import com.winterwell.utils.Mutable;
import com.winterwell.utils.Printer.IPrinter;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.AbstractMap2;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.IntRange;

/**
 * A simple but friendly JSON import/export class.
 * 
 * Why? To avoid external dependencies and crockford's org.json jar. Advice: Use
 * Jetty's JSON class instead. Or Gson.
 * 
 * @testedby  SimpleJsonTest}
 * @author daniel
 */
public class SimpleJson {

	private static final Object END_OF_ARRAY = "]";
	private static final Object END_OF_MAP = "}";
	static final Map<Class, IPrinter> useMe = new HashMap<Class, IPrinter>();

	public static final <X> void addPlugin(Class<X> klass, IPrinter<X> convertor) {
		useMe.put(klass, convertor);
	}

	/**
	 * Convenience for drilling down through the map-of-maps data structures
	 * that JSON tends to throw up. Does not do any conversion.
	 * 
	 * @param <X>
	 * @param jsonObj
	 * @param fields
	 *            or integers for array indices
	 * @return e.g. jsonObj.field1.field2 Can be null if any step is null
	 */
	public static <X> X get(Object jsonObj, Object... fields) {
		if (jsonObj==null) return null;
		assert jsonObj instanceof Map || jsonObj instanceof List : jsonObj;
		for (int i = 0; i < fields.length; i++) {
			// object property
			Object fi = fields[i];
			if (fi instanceof String) {
				String f = (String) fi;
				if (jsonObj instanceof Map) {					
					Object jsonObj2 = ((Map) jsonObj).get(f);
					if (jsonObj2 == null) {
	//					throw new NullPointerException(jsonObj + ".." + f);
						return null;
					}
					jsonObj = jsonObj2;
					continue;
				}
				// array index?
				if (StrUtils.isInteger(f)) {
					fi = Integer.valueOf(f);
					// cary on below...
				} else {
					// try POJO access?
					Object v = ReflectionUtils.getPrivateField(jsonObj, f);
					jsonObj = v;
					continue;
//					throw new IllegalArgumentException("Cannot parse "+Printer.str(fields));
				}
			} // ./ isa String
			int f = ((Number) fi).intValue();			
			if (jsonObj.getClass().isArray()) {
				jsonObj = Array.getLength(jsonObj) > f? Array.get(jsonObj, f) : null;
			} else {
				jsonObj = Containers.get((List)jsonObj, f);
			}
			if (jsonObj == null) {
//				throw new NullPointerException(jsonObj + ".." + f);				
				return null;
			}
		}
		return (X) jsonObj;
	}

	public void appendJson(StringBuilder sb, Object x) {
		if (x == null) {
			sb.append("null");
			return;
		}
		// use a plugin?
		IPrinter prntr = useMe.get(x.getClass());
		if (prntr != null) {
			prntr.append(x, sb);
			return;
		}
		if (x instanceof IHasJson) {
			((IHasJson) x).appendJson(sb);
			return;
		}
		if (x instanceof CharSequence) {
			// Escape "s
			sb.append('"');
			escape(sb, (CharSequence) x);
			sb.append('"');
			return;
		}
		if (x instanceof Number) {
			sb.append(x.toString());
			return;
		}
		if (x instanceof Boolean) {
			sb.append(x.toString());
			return;
		}
		if (x instanceof Map) {
			appendJson2_map((Map) x, sb);
			return;
		}
		if (x.getClass().isArray()) {
			x = Containers.asList(x);
		}
		if (x instanceof Collection) {
			appendJson2_list((Collection) x, sb);
			return;
		}
		// fallback to toString()
		// Escape "s
		sb.append('"');
		escape(sb, x.toString());
		sb.append('"');
		// Log.report("SimpleJson: ...Unknown type: "+x.getClass());
	}

	private void appendJson2_list(Collection list, StringBuilder sb) {
		sb.append("[");
		for (Object x : list) {
			appendJson(sb, x);
			sb.append(", ");
		}
		if (!list.isEmpty()) {
			StrUtils.pop(sb, 2);
		}
		sb.append("]");
	}

	private void appendJson2_map(Map map, StringBuilder sb) {
		sb.append("{");
		for (Object key : map.keySet()) {
			// Ensure ""s on key
			String jk = toJson(key);
			if ( ! jk.startsWith("\"")) {
				sb.append('"');
				sb.append(jk);
				sb.append('"');
			} else {
				sb.append(jk);
			}
			sb.append(":");
			Object v = map.get(key);
			appendJson(sb, v);
			sb.append(", ");
		}
		if (!map.isEmpty()) {
			StrUtils.pop(sb, 2);
		}
		sb.append("}");
	}

	/**
	 * Escape "s, tabs, \r, \n
	 * 
	 * @param sb
	 * @param x
	 */
	private void escape(StringBuilder sb, CharSequence x) {
		char b;
		char c = 0;
		String hhhh;
		int len = x.length();

		for (int i = 0, n = x.length(); i < n; i++) {
			b = c;
			c = x.charAt(i);

			switch (c) {
			case '\\':
			case '"':
				sb.append('\\');
				sb.append(c);
				break;
			case '/':
				if (b == '<') {
					sb.append('\\');
				}
				sb.append(c);
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\r':
				sb.append("\\r");
				break;
			default:
				if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
						|| (c >= '\u2000' && c < '\u2100')) {
					hhhh = "000" + Integer.toHexString(c);
					sb.append("\\u" + hhhh.substring(hhhh.length() - 4));
				} else {
					sb.append(c);
				}
			}
		}
	}

	/**
	 * Use org.eclipse.jetty JSON instead
	 * @param json
	 * @return
	 */
	@Deprecated
	// doesn't work reliably yet
	public Object fromJson(String json) {
		return fromJson2(json, new Mutable.Int(0));
	}

	Object fromJson2(String json, Mutable.Int index) {
		// String?
		// Ignore whitespace outside of quotes
		char ci;
		while(true) {
			ci = json.charAt(index.value);
			if (ci!=' ' && ci!='\n' && ci!='\r' && ci!='\t') break;
			index.value++;
		}
		if (ci == '"' || ci == '\'') {
			// find closing quote
			StringBuilder word = new StringBuilder();
			index.value++;
			boolean escaped = false;
			for (; index.value < json.length(); index.value++) {
				char c = json.charAt(index.value);
				if (escaped) {
					word.append(c);
					escaped = false;
					continue;
				}
				if (c == ci)
					// index.value++;
					return word.toString();
				if (c == '\\') {
					escaped = true;
				} else {
					word.append(c);
				}
			}
			throw new IllegalArgumentException(json);
		}
		if (StrUtils.isNumber(json))
			return Double.valueOf(json);
		if (json.startsWith("null", index.value)) {
			index.value += 4;
			return null;
		}
		if (json.startsWith("true", index.value)) {
			index.value += 4;
			return Boolean.TRUE;
		}
		if (json.startsWith("false", index.value)) {
			index.value += 5;
			return Boolean.FALSE;
		}
		// array
		if (ci == '[') {
			index.value++;
			List array = new ArrayList();
			for (; index.value < json.length(); index.value++) {
				Object e = fromJson2(json, index);
				if (e == END_OF_ARRAY) {
					break;
				}
				array.add(e);
			}
			return array;
		}
		if (ci == ']')
			return END_OF_ARRAY;
		// map
		if (ci == '{') {
			index.value++;
			Map array = new HashMap();
			for (; index.value < json.length(); index.value++) {
				Object k = fromJson2(json, index);
				if (k.equals(",")) continue; // a bit lax!
				if (END_OF_MAP.equals(k)) {
					break;
				}
				String marker = (String) fromJson2(json, index);
				marker = marker.trim();
				Object v = fromJson2(json, index);
				if ( ! marker.equals(":"))
					throw new FailureException(marker+" "+StrUtils.substring(json, index.value, 0)+" json:"+json);
				array.put(k, v);
			}
			return array;
		}
		if (ci == '}')
			return END_OF_MAP;
		if (ci == ':') return ":";
		if (ci == ',') return ",";
		throw new RuntimeException("TODO! " + json.substring(0, index.value)
				+ "^" + json.substring(index.value));
	}

	public String toJson(Object obj) {
		StringBuilder sb = new StringBuilder();
		appendJson(sb, obj);
		String json = sb.toString();

		return json;
	}

	/**
	 * Convenience for get-map-(put-if-absent)
	 * @param jobj
	 * @param key
	 * @return jobj.key
	 */
	public static Map<String, Object> getCreate(Map jobj, String key) {
		Object m = jobj.get(key);
		if (m==null) {
			m = new ArrayMap(); // hm... good for small maps, bad for larger ones. But this is a convenience method.
			jobj.put(key, m);
		}
		if (m instanceof Map) {
			return (Map<String, Object>) m;
		}
		// probably an array!
		List<Object> list = Containers.asList(m);
		// make up a new map??
		Map map = new ListAsMap(list);
		return map;
	}

	/**
	 * Convenience for drilling down through (and making) the map-of-maps data structures
	 * that JSON tends to involve. 
	 * Uses getCreate to access/build intermediate objects.
	 * @param jobj
	 * @param value
	 * @param key 
	 */
	public static void set(Map<String, ?> jobj, Object value, String... key) {
		Map obj = jobj;
		for(int i=0,n=key.length-1; i<n; i++) {
			String k = key[i];
			obj = getCreate(obj, k);
			assert obj != null : jobj+" "+k;
		}
		// set it
		String k = key[key.length-1];
		try {		
			if (value==null) {
				obj.remove(k);
			} else {
				obj.put(k, value);
			}
		} catch (UnsupportedOperationException ex) {
			// add to an array failed? replace the array
			if (obj instanceof ListAsMap && key.length > 1) {
				ArrayList obj2 = new ArrayList(((ListAsMap) obj).list);
				new ListAsMap(obj2).put(k, value);
				String[] pathToArray = Arrays.copyOf(key, key.length-1);
				set(jobj, obj2, pathToArray);
				return;
			}
			// can't fix
			throw ex;
		}
	}

	/**
	 * Convenience to get a List value
	 * @param jsonObj
	 * @param fields
	 * @return List or null
	 */
	public static List getList(Object jsonObj, Object... fields) {
		Object v = get(jsonObj, fields);
		if (v==null) return null;
		return Containers.asList(v);
	}


}

/**
 * Helpful for some json methods, to let arrays be maps
 * @author daniel
 *
 * @param <V>
 */
class ListAsMap<V> extends AbstractMap2<String, V> {

	@Override
	public Set<String> keySet() throws UnsupportedOperationException {
		IntRange keys = new IntRange(0, list.size()-1);
		return new ArraySet(Containers.apply(keys, k -> Integer.toString(k)));
	}
	
	List<V> list;
	
	public ListAsMap(List<V> list) {
		this.list = list;
	}

	@Override
	public V get(Object i) {
		Integer ni = i instanceof Integer? (Integer) i : Integer.valueOf((String)i);
		return list.get(ni);
	}

	@Override
	public V put(String i, V v) {
		Integer ni = Integer.valueOf((String)i);
		if (ni >= list.size()) {
			// pad the list
			while (ni > list.size()) {
				list.add(null); 
			}
			// add
			list.add(v);
			return null;
		}
		return list.set(ni, v);		
	}
	
}
