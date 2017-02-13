package com.winterwell.datalog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.utils.web.SimpleJson;

/**
 * For logging an event -- which can have arbitrary detail.
 * @author daniel
 */
public final class DataLogEvent implements Serializable, IHasJson {
	private static final long serialVersionUID = 1L;
	
	public final double count;
	public final String eventType;
	final Map<String, ?> props;

	/**
	 * Does NOT include time-period or dataspace. This is added by the DataLog based on how
	 * it buckets stuff.
	 */
	public final String id;
	
	/**
	 * Does NOT include time-period or dataspace. This is added by the DataLog based on how
	 * it buckets stuff.
	 */
	public String getId() {
		return id;
	}

	public final String dataspace;
	
	public DataLogEvent(String eventType, Map<String,?> properties) {
		this(DataLog.getDataspace(), 1, eventType, properties);
	}
	
	public DataLogEvent(String dataspace, double count, String eventType, Map<String,?> properties) {
		this.dataspace = dataspace;
		assert ! dataspace.isEmpty() && dataspace.equals(StrUtils.toCanonical(dataspace)) && ! dataspace.contains("/") : dataspace;
		this.count = count;
		this.eventType = eventType;
		this.props = properties;
		this.id = makeId();
		assert ! Utils.isBlank(eventType);
	}

	/**
	 * Unique based on eventType and properties
	 * @param dataLogEvent
	 */
	private String makeId() {
		if (props==null || props.isEmpty()) {
			return eventType;
		}
		List<String> keys = new ArrayList(props.keySet());
		Collections.sort(keys);
		StringBuilder sb = new StringBuilder();
		for (String key : keys) {
			Object v = props.get(key);
			if (v==null) continue;
			sb.append(key);
			sb.append('=');
			sb.append(v);
			sb.append('&');
		}
		String txt = sb.toString();
		return dataspace+"/"+eventType+"_"+StrUtils.md5(txt);
	}

	@Override
	public String toJSONString() {
		return new SimpleJson().toJson(toJson2());
	}

	@Override
	public Map<String,?> toJson2() {
		Map map = new ArrayMap();
		map.putAll(props);
		map.put("dataspace", dataspace);
		map.put("eventType", eventType);
		map.put("count", count);
		return map;
	}	
	
}
