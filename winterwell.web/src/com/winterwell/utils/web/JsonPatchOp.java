
package com.winterwell.utils.web;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.containers.ArrayMap;

/**
 * diff using json-patch https://tools.ietf.org/html/rfc6902
 * 
 * //		[
//	     { "op": "test", "path": "/a/b/c", "value": "foo" },
//	     { "op": "remove", "path": "/a/b/c" },
//	     { "op": "add", "path": "/a/b/c", "value": [ "foo", "bar" ] },
//	     { "op": "replace", "path": "/a/b/c", "value": 42 },
//	     { "op": "move", "from": "/a/b/c", "path": "/a/b/d" },
//	     { "op": "copy", "from": "/a/b/d", "path": "/a/b/e" }
//	   ]
 * @author daniel
 *
 */
public class JsonPatchOp implements Serializable, IHasJson {
	
	public static enum KOp {
		test, remove, add, replace, move, copy
	}
	
	public KOp op;
	
	public String path;
	
	public String from;
	
	public Object value;

	public static JsonPatchOp remove(String path) {
		JsonPatchOp jpo = new JsonPatchOp();
		jpo.op = KOp.remove;
		jpo.path = path;
		return jpo;
	}
	public static JsonPatchOp add(String path, Object value) {
		JsonPatchOp jpo = new JsonPatchOp();
		jpo.op = KOp.add;
		jpo.path = path;
		jpo.value=value;
		return jpo;
	}
	public static JsonPatchOp replace(String path, Object newValue) {
		JsonPatchOp jpo = new JsonPatchOp();
		jpo.op = KOp.replace;
		jpo.path = path;
		jpo.value=newValue;
		return jpo;
	}


	@Override
	public String toString() {
		return "JsonPatchOp["+toJson2()+"]";
	}


	@Override
	public int hashCode() {
		return Objects.hash(from, op, path, value);
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JsonPatchOp other = (JsonPatchOp) obj;
		return Objects.equals(from, other.from) && op == other.op && Objects.equals(path, other.path)
				&& Objects.equals(value, other.value);
	}


	/**
	 * Assumes value is already a json-style object!
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public Map<String,Object> toJson2() throws UnsupportedOperationException {		
		assert value==null || ReflectionUtils.isBasicType(value) || value instanceof Map || value instanceof List || value.getClass().isArray() : value.getClass();
		return new ArrayMap(
			"op", op.toString(),
			"path", path,
			"from", from,
			"value", value
		);
	}

	
}
