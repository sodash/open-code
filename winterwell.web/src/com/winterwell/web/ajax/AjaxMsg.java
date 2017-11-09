package com.winterwell.web.ajax;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.winterwell.utils.containers.AbstractMap2;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.web.WebEx;

/**
 * Standardise the format for sending messages to clients.
 * See Notification or Notifications.js in Creole for more details.
 * 
 * Implements map for json conversion
 * @author daniel
 */
public class AjaxMsg extends AbstractMap2<String, Object> implements Serializable {
	
	private static final long serialVersionUID = 1L;	
	protected final KNoteType type;
	protected final String id;
	protected final String text;
	protected final ArrayMap jobj;

	public static enum KNoteType {
		success, info, warning, error, klaxon
	}

	public AjaxMsg(String text) {
		this(KNoteType.info, null, text);
	}
	public AjaxMsg(Throwable ex) {
		// use class-name as ID :(
		this(KNoteType.error, ex.getClass().toString(), ex.getMessage());
		if (ex instanceof WebEx) {
			jobj.put("code", ((WebEx) ex).code);
		}
	}
	
	/**
	 * 
	 * @param type
	 * @param id
	 * @param text User friendly text. Can be null, though that's not a good idea.
	 */
	public AjaxMsg(KNoteType type, String id, String text) {
		this.type = type;
		assert type != null : text;
		this.id= id;		
		this.text = (text==null || text.isEmpty())? id : text;
		jobj = new ArrayMap(
				"type", type.toString(),
				"id", id,
				"text", text
				);
	}
	
	/**
	 * Create an AjaxMsg from a json-ready map.
	 * @param map MUST contain "type", "id", and "text"
	 */
	public AjaxMsg(Map map) {
		this(KNoteType.valueOf(map.get("type").toString()), 
			  map.get("id").toString(), 
			  map.get("text").toString());
		for(Object k : map.keySet()) {
			k = k.toString();
			if ("id".equals(k) || "type".equals(k) || "text".equals(k)) continue;
			jobj.put(k, map.get(k));	
		}
	}
	
	@Override
	public String toString() {
		return new SimpleJson().toJson(toJsonObj());
	}
	
	public String getText() {
		return text;
	}
	
	public final KNoteType getType() {
		return type;
	}
	
	public final String getId() {
		return id;
	}

	public final Map toJsonObj() {		
		return jobj;
	}
	

	@Override
	public Set<String> keySet() throws UnsupportedOperationException {
		return toJsonObj().keySet();
	}

	@Override
	public Object get(Object key) {
		return toJsonObj().get(key);
	}

	@Override
	public Object put(String key, Object value) {
		return jobj.put(key, value);
		//throw new UnsupportedOperationException();
	}

}