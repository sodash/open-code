package com.winterwell.utils.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.web.ajax.JThing;
/**
 * diff using json-patch https://tools.ietf.org/html/rfc6902
 * @author daniel
 * @testedby {@link JsonPatchTest}
 */
public class JsonPatch implements IHasJson {

	List<JsonPatchOp> diffs;

	public List<JsonPatchOp> getDiffs() {
		return diffs;
	}
	
	public JsonPatch(List<JsonPatchOp> diffs) {
		Utils.check4null(diffs);
		this.diffs = diffs;		
	}
	
	public JsonPatch(Map before, Map after) {
		this.diffs = new ArrayList();
		diffMap("", before, after);		
	}
	
	public JsonPatch(List before, List after) {
		this.diffs = new ArrayList();
		diffList("", before, after);		
	}
	
	private void diffMap(String path, Map before, Map after) {
		if (after==null) {
			diffs.add(JsonPatchOp.remove(path));
			return;
		}
		if (before==null) {
			diffs.add(JsonPatchOp.add(path, after));
			return;
		}
		Set<String> akeys = after.keySet();
		for(String k : akeys) {
			// changes?
			Object av = after.get(k);
			if (av==null) continue;
			Object bv = before.get(k);
			String pathk = path+"/"+k;
			if (bv==null) {
				diffs.add(JsonPatchOp.add(pathk, av));
				continue;
			}			
			if (av.equals(bv)) continue;
			// recurse?
			if (bv instanceof Map && av instanceof Map) {
				diffMap(pathk, (Map)bv, (Map)av);
				continue;
			}
			if (bv.getClass().isArray()) bv = Containers.asList(bv);
			if (av.getClass().isArray()) av = Containers.asList(av);
			if (bv instanceof List && av instanceof List) {
				diffList(pathk, (List)bv, (List)av);
				continue;
			}
			diffs.add(JsonPatchOp.replace(pathk, av));			
		}
		// what got removed?
		Set<String> bkeys = before.keySet();
		for(String k : bkeys) {
			Object av = after.get(k);
			if (av==null) {
				diffs.add(JsonPatchOp.remove(path+"/"+k));
			}
		}
	}

	private void diffList(String path, List before, List after) {
		if (after==null) {
			diffs.add(JsonPatchOp.remove(path));
			return;
		}
		if (before==null) {
			diffs.add(JsonPatchOp.add(path, after));
			return;
		}		
		for(int k=0; k<after.size(); k++) {
			// changes?
			Object av = after.get(k);
			if (av==null) continue;
			Object bv = before.size() <= k? null : before.get(k);
			String pathk = path+"/"+k;
			if (bv==null) {
				diffs.add(JsonPatchOp.add(pathk, av));
				continue;
			}			
			if (av.equals(bv)) continue;
			// recurse?
			if (bv instanceof Map && av instanceof Map) {
				diffMap(pathk, (Map)bv, (Map)av);
				continue;
			}
			if (bv.getClass().isArray()) bv = Containers.asList(bv);
			if (av.getClass().isArray()) av = Containers.asList(av);
			if (bv instanceof List && av instanceof List) {
				diffList(pathk, (List)bv, (List)av);
				continue;
			}
			diffs.add(JsonPatchOp.replace(pathk, av));			
		}
		// what got removed?
		for(int k=after.size(); k<before.size(); k++) {
			diffs.add(JsonPatchOp.remove(path+"/"+k));			
		}			
	}

	public void apply(Map jobj) {			
		if (diffs==null || diffs.isEmpty()) {
			return;
		}		
		for (JsonPatchOp diff : diffs) {
			// NB: drop the leading / on path
			String[] bits = diff.path.substring(1).split("/");			
			Object value = diff.value;
			switch(diff.op) {
			case add: case replace:
				SimpleJson.set(jobj, value, bits);
				break;
			case remove:
				SimpleJson.set(jobj, null, bits);
				break;			
			case move:
			case copy:
			case test:
				throw new TodoException(diff);
			}
		}
	}
	
	public void apply(List jarr) {
		if (diffs==null || diffs.isEmpty()) {
			return;
		}		
		// Hack wrap so we can use Map methods
		ArrayMap jobj = new ArrayMap("foo", jarr);
		for (JsonPatchOp diff : diffs) {
			String[] bits = ("foo"+diff.path).split("/");			
			Object value = diff.value;
			switch(diff.op) {
			case add: case replace:
				SimpleJson.set(jobj, value, bits);
				break;
			case remove:
				SimpleJson.set(jobj, null, bits);
				break;			
			case move:
			case copy:
			case test:
				throw new TodoException(diff);
			}
		}
	}

	@Override
	public List toJson2() throws UnsupportedOperationException {
		return Containers.apply(diffs, JsonPatchOp::toJson2);
	}
	
	@Override
	public String toString() {
		return "JsonPatch"+toJSONString();
	}

	

}
