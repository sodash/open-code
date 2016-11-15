package winterwell.utils.web;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JSONCleaner {
	
	
	/**
	 * Takes a Map, expected to represent a JSON object, and generates a new Map which replaces
	 * any Doubles therein (or in any nested Maps) containing NaN or Infinity with string representations.
	 * Has basic circular reference safety.
	 * @param jsonMap A Map. Should be JSON-safe, but this probably won't break if it isn't.
	 * @return A Map of the same type as jsonMap with circular references replaced with a placeholder String and NaN/Infinity replaced with null
	 */
	public static Map cleanDoubles(Map jsonMap) {
		Set<Object> visited = new HashSet<Object>();
		
		Map newMap = null;
		try {
			newMap = jsonMap.getClass().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			newMap = new HashMap();
		}
		
		for(Object key: jsonMap.keySet()) {
			Object value = jsonMap.get(key);
			
			if(visited.contains(value)) {
				newMap.put(key, "circular reference removed");
				continue;
			}
			
			if (value instanceof Map) {
				newMap.put(key, cleanDoubles((Map) value));
			} else if (value instanceof Double) {
				if(((Double)value).isInfinite() || ((Double)value).isNaN()) {
					newMap.put(key, ((Double)value).toString());
				}
			} else {
				newMap.put(key, value);
			}
			
			visited.add(value);
		}
		return newMap;
	}
	
	/**
	 * Takes a Map, expected to represent a JSON object, and replaces any Doubles in the Map
	 * (or any nested Maps) containing NaN or Infinity with string representations.
	 * Has basic circular reference safety.
	 * @param jsonMap A Map. Should be JSON-safe, but this probably won't break if it isn't.
	 */
	public static void cleanDoublesInPlace(Map jsonMap) {
		Set<Object> visited = new HashSet<Object>();
		
		for(Object key: jsonMap.keySet()) {
			Object value = jsonMap.get(key);
			
			if(visited.contains(value)) {
				continue;
			}
			
			if (value instanceof Map) {
				cleanDoublesInPlace((Map) value);
			} else if (value instanceof Double) {
				if(((Double)value).isInfinite() || ((Double)value).isNaN()) {
					jsonMap.put(key, ((Double)value).toString());
				}
			}
			
			visited.add(value);
		}
	}
}
