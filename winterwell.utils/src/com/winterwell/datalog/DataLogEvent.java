package com.winterwell.datalog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.winterwell.utils.Null;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.log.KErrorPolicy;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.utils.web.SimpleJson;

/**
 * For logging an event -- which can have arbitrary detail.
 * @author daniel
 */
public final class DataLogEvent implements Serializable, IHasJson {
	private static final long serialVersionUID = 1L;

	public static final String EVENTTYPE = "evt";

	/**
	 * these get special treatment - as direct properties not key/value props.
	 * Which means nicer json + they can have a type in ES.
	 * 
	 * HACK: use StringBuilder.class as a marker for "text with tokenisation", and String.class for keywords.
	 * HACK: use Object.class to mark special-case
	 * HACK: use Null to mark no-index
	 */
	public static final Map<String, Class> COMMON_PROPS = 
			new HashMap(new ArrayMap(			
			// tracking
			"ip", String.class,
			"user", String.class,
			"url", String.class,
			"domain", String.class,
			"host", String.class,
			"country", String.class,
			// ad tracking
			"adid", String.class,
			"idfa", String.class,
			// common event-defining properties
			"tag", String.class, 
			"action", String.class, 
			"verb", String.class,
			"as", String.class,
			"turl", String.class,
			"href", String.class,
			"to", String.class,
			"src", String.class,
			"from", String.class,
			// content / advert properties
			"pub", String.class,
			"publisher", String.class,
			"ad", String.class,
			"vert", String.class,
			"vertiser", String.class,
			"bid", String.class, // Our Bid ID
			"xbid", String.class, // someone elses (possibly broken) bid id
			"variant", String.class,
			"campaign", String.class,
			"cid", String.class, // SoGive charity-ID. This is preferred to 'charity' as a property-name			
			"via", String.class, // exchange / DSP / agency used to make the connection. ??Can be a list.??is that supported
			// text properties (support tokenisation)
			"place", StringBuilder.class,
			"locn", StringBuilder.class,
			"location", StringBuilder.class,
			// a few XId properties
			"id", String.class,
			"xid", String.class,
			"oxid", String.class,
			"txid", String.class,
			"uxid", String.class,
			"su",String.class,
			// a few scoring/results properties
			"start", Time.class,
			"end", Time.class,
			"dt", Long.class,
			"note", StringBuilder.class,
			"notes", StringBuilder.class, 
			"score", Double.class,
			"amount", Double.class,
			"currency", String.class,
			"w", Integer.class, // width
			"h", Integer.class, // height
			"winw", Integer.class, // window-width
			"winh", Integer.class, // window-height
			"x", Double.class,
			"y", Double.class,
			"z", Double.class,
			"geo", Object.class,
			"lat", Double.class,
			"lng", Double.class,			
			// browser info
			"mbl", Boolean.class,
			"ua", StringBuilder.class, // user agent
			"os", String.class
			// no-index (object)
//			"xtra", Null.class FIXME this was causing bugs :(
			));

	public static final String simple = "simple";
	
	public final double count;
	/**
	 * Note: this is NOT the same as tag, because we want to limit to a sane number of these.
	 * e.g. type=simple, tag=any old string
	 */
	public final String eventType;
	/**
	 * never null
	 */
	final Map<String, Object> props;

	public Object getProp(String prop) {
		return props.get(prop);
	}
	
	public Map<String, Object> getProps() {
		return props;
	}
	
	/**
	 * Does NOT include time-period or dataspace. This is added by the DataLog based on how
	 * it buckets stuff.
	 */
	public final String id;
	
	/**
	 * Does NOT include time-period. This is added by the DataLog based on how
	 * it buckets stuff.
	 */
	public String getId() {
		return id;
	}

	public final String dataspace;

	/**
	 * When should this be set?? This may be the bucket rather than the exact time.
	 */
	public Time time = new Time();
	
	/**
	 * @deprecated Use the other constructor
	 * @param eventType
	 * @param properties
	 */
	public DataLogEvent(String eventType, Map<String,?> properties) {
		this(DataLog.getDataspace(), 1, eventType, properties);
	}
	
	public DataLogEvent(String tag, double count) {
		this(DataLog.getDataspace(), count, simple, new ArrayMap("tag", tag));
	}
	
	/**
	 * 
	 * @param dataspace e.g. "default" (which becomes datalog.default in ES)
	 * @param count e.g. 1
	 * @param eventType e.g. "evt.pick"
	 * @param properties e.g. {url, user} This is used directly and can be modified!
	 */
	public DataLogEvent(String dataspace, double count, String eventType, Map<String,?> properties) {
		this.dataspace = StrUtils.normalise(dataspace, KErrorPolicy.ACCEPT).toLowerCase().trim();
		assert ! dataspace.isEmpty() && ! dataspace.contains("/") : dataspace;
//		assert dataspace.equals(StrUtils.toCanonical(dataspace)) : dataspace +" v "+StrUtils.toCanonical(dataspace); 
		this.count = count;
		this.eventType = eventType;
		this.props = properties == null? Collections.EMPTY_MAP : (Map) properties;
		this.id = makeId();
		assert ! Utils.isBlank(eventType);
		// set time??
		Object t = this.props.get("time");
		if (t != null) {
			try {
				time = new Time(t.toString());
			} catch(Exception ex) {
				Log.w("DataLogEvent.time", t+" "+ex);
			}
		}
	}

	@Override
	public String toString() {
		return "DataLogEvent[count=" + count + ", eventType=" + eventType + ", props=" + props + ", id=" + id
				+ ", dataspace=" + dataspace + "]";
	}

	/**
	 * Unique based on dataspace, eventType and properties. Does NOT include time though!
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

	/**
	 * This is for ElasticSearch!
	 * For external use, use Gson or similar.
	 */
	@Override
	public String toJSONString() {
		return new SimpleJson().toJson(toJson2());
	}

	/**
	 * This is for ElasticSearch!
	 * For external use, use Gson or similar.
	 * 
	 * 
	 * Because this has to handle big data, we economise and store either n or v, not both.
	 * {k: string, n: ?number, v: ?string}
	 * 
	 * Does NOT include dataspace
	 */
	@Override
	public Map<String,?> toJson2() {
		Map map = new ArrayMap();		
//		map.put("dataspace", dataspace); This is given by the index
		map.put(EVENTTYPE, eventType);
		map.put("time", time.toISOString()); //getTime()); // This is for ES -- which works with epoch millisecs
		map.put("count", count);
		if (props.isEmpty()) return map;
		// others as a list (to avoid hitting the field limit in ES which could happen with dynamic fields)
		List propslist = new ArrayList();
		for(Entry<String, ?> pv : props.entrySet()) {
			Object v = pv.getValue();
			if ( ! Utils.truthy(v)) continue;
			Class proptype = COMMON_PROPS.get(pv.getKey());
			if (proptype!=null) {				
				// privileged props
				if (v instanceof Map && proptype!=Object.class) {
					// no objects here (otherwise ES will throw an error)
					// NB: this will catch xtra (no-index props) which have proptype Null.class
					String vs = new SimpleJson().toJson(v);
					v = vs;
				}
				map.put(pv.getKey(), v);
				continue;
			}
			ArrayMap<String,Object> prop;
			if (v instanceof Number) {
				prop = new ArrayMap(
						"k", pv.getKey(),
						"n", v
						);		
			} else {				
				prop = new ArrayMap(
						"k", pv.getKey(),
						"v", v.toString()
						);
			}
			propslist.add(prop);
		}
		map.put("props", propslist);
		return map;
	}

	public void setExtraResults(Map map) {
		props.put("xtra", map);
	}
	
}
