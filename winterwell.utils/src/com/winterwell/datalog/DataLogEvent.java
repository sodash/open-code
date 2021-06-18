package com.winterwell.datalog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Null;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.KErrorPolicy;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.utils.web.SimpleJson;

/**
 * For logging an event -- which can have arbitrary detail.
 * 
 * Best Practice for Logging a Table of Data
 * 
 * Do you need to do sums & averages?
 *  - Use count, or a {@link #COMMON_PROPS} numerical field.
 *  - Create multiple events, which can be selected via a shared COMMON_PROPS key=value,
 *  and use count for each event.
 *   
 * 
 * Use uncommon props for "audit trail" details -- stored but not easily
 * searchable.
 * 
 * 
 * 
 * @author daniel
 */
public final class DataLogEvent implements Serializable, IHasJson
//IProperties?? but keys would not include time, count, dataspace
{
	private static final long serialVersionUID = 1L;

	/**
	 * event-type -- can be an array to combine several events from a single session.
	 */
	public static final String EVT = "evt";

	/**
	 * These get special treatment - as direct properties not key/value props.
	 * Which means nicer json + they can have a type in ES.
	 * 
	 * It allows for one DataLogEvent to hold a few stats. This is handy for one DL event describes one advert.
	 * 
	 * The other DatLog approach is to turn multiple stats into multiple events, and let ES handle it. 
	 * 
	 * 
	 * HACK: use StringBuilder.class as a marker for "text with tokenisation", and String.class for keywords.
	 * HACK: use Object.class to mark special-case
	 * HACK: use Null to mark no-index
	 */
	public static final Map<String, Class> COMMON_PROPS = 
			new HashMap(new ArrayMap(			
			// tracking
			"ip", String.class,
			/** Before July 2021, can mix xids with temp and @trk ids. See `trk` */
			"user", String.class,
			/** temp and @trk ids */
			"trk", String.class,
			"url", String.class,
			"domain", String.class,
			"host", String.class,
			"country", String.class,
			// ad tracking
			"adid", String.class,
			"idfa", String.class,
			// common event-defining properties
			"cause", String.class,
			"tag", String.class, 
			"action", String.class, 
			"verb", String.class,
			"col", String.class, // Column name
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
			"campaign", String.class, // also a utm_ parameter
			"medium", String.class, // utm_ parameter
			"source", String.class, // utm_ parameter
			"lineitem", String.class, // LineItem ID, e.g. from AppNexus
			"cid", String.class, // SoGive charity-ID. This is preferred to 'charity' as a property-name			
			"via", String.class, // exchange / DSP / agency used to make the connection. ??Can be a list.??is that supported			
			"invalid", String.class, // keyword for "bot" | "test" (ie us or the publisher). Blank for fine!
			// text properties (support tokenisation)
			"place", StringBuilder.class,
			"locn", StringBuilder.class,
			"location", StringBuilder.class,
			"msg", StringBuilder.class,
			"message", StringBuilder.class,
			// Can we do an aggregation on message??
//			"m", String.class, // the keyword version of message (for doing exact-text repeat breakdowns, which are handy)

			// a few XId properties
			"id", String.class,
			"xid", String.class,
			"oxid", String.class,
			"txid", String.class,
			"email", String.class,
			"uxid", String.class,
			"su",String.class,
			"gby", String.class, // group-by ID -- for collating events together into one summary event

			// a few scoring/results properties
			"start", Time.class,
			"end", Time.class,
			"dt", Long.class, // milliseconds
			"note", StringBuilder.class,
			"notes", StringBuilder.class, 
			"score", Double.class,
			"amount", Double.class,
			"price", Double.class,
			/** Use a distinct param for donation amount, so theres no clashes when grouping events together */
			"dntn", Double.class,
			/**
			 * revenue model
			 */
			"rev", String.class, 
			/**
			 * rev-share for publishers
			 */
			"revpub", Double.class,
			/**
			 * rev-share for agencies
			 */
			"revagent", Double.class,
			"currency", String.class, // deprecated
			"curr", String.class,
			"w", Integer.class, // width
			"h", Integer.class, // height
			"size", String.class, // size could be e.g. "300x250", "billboard", or a number. So lets just store it as a keyword.
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
			"browser", String.class,
			"env", String.class, // environment -- possibly this should be StringBuilder for word handling?? but does bby then break??
			"os", String.class,
			// no-index (object)
			"xtra", Null.class, // FIXME this was causing bugs :(
			"nonce", Null.class, 
			"socialShareId", String.class // unique ID to identify where ad has been requested via link shared on social media
			// mailing list / CRM??
		));

	public static final String simple = "simple";
	
	public final double count;
	/**
	 * Note: this is NOT the same as tag, because we want to limit to a sane number of these.
	 * e.g. type=simple, tag=any old string
	 */
	private final String[] evt;
	/**
	 * never null
	 */
	final Map<String, Object> props;

	public Object getProp(String prop) {
		return props.get(prop);
	}
	
	/**
	 * 
	 * @return props (excluding count, dataspace, evt, id, time).
	 * This cannot be modified.
	 */
	public Map<String, Object> getProps() {
		return Collections.unmodifiableMap(props);
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

	public void setTime(Time time) {
		this.time = time;
	}
	
	/**
	 * Set in the constructor, as it affects the ID (which is how the grouping happens)
	 * transient as the ID is the eventual store of this.
	 */
	transient String groupById;
	
	public DataLogEvent(String tag, double count) {
		this(DataLog.getDataspace(), count, simple, new ArrayMap("tag", tag));
	}
	
	/**
	 * 
	 * @param dataspace e.g. "default" (which becomes datalog.default in ES)
	 * @param count e.g. 1
	 * @param eventType e.g. "evt.pick" This will be converted to lower-case (mainly 'cos it avoids confusing ES case related errors)
	 * @param properties e.g. {url, user} This is used directly and can be modified!
	 */
	public DataLogEvent(CharSequence dataspace, double count, String eventType, Map<String,?> properties) {
		this(dataspace, null, count, new String[] {eventType.toLowerCase()}, properties);
	}
	
	/**
	 * The base constructor. If in doubt use this.
	 * 
	 * @param dataspace e.g. "default" (which becomes datalog.default in ES)
	 * @param groupById This will be used to make the ID. can be null. Allows for grouping several events into one.
	 * @param count e.g. 1
	 * @param eventType e.g. "minview"
	 * @param properties e.g. {url, pub} This is used directly and can be modified! Can be null
	 */
	public DataLogEvent(CharSequence _dataspace, String groupById, double count, String[] eventType, Map<String,?> properties) {
		this.dataspace = StrUtils.normalise(_dataspace.toString(), KErrorPolicy.ACCEPT).toLowerCase().trim();
		assert ! dataspace.isEmpty() && ! dataspace.contains("/") : dataspace;
//		assert dataspace.equals(StrUtils.toCanonical(dataspace)) : dataspace +" v "+StrUtils.toCanonical(dataspace); 
		this.count = count;
		this.evt = eventType;
		this.props = properties == null? Collections.EMPTY_MAP : (Map) properties;
		this.groupById = Utils.isBlank(groupById)? null : groupById;
		this.id = makeId(groupById);
		assert ! Utils.isBlank(eventType[0]);
		// set time??
		Object t = this.props.get("time");
		if (t != null) {
			try {
				time = new Time(t.toString());
			} catch(Exception ex) {
				Log.w("DataLogEvent.time", t+" "+ex);
			}
		}
		// HACK adjust user
		initAdjustUserProp();
	}

	/**
	 * HACK store temp ids separately from proper user logins.
	 * So we can forget the temp ones, but keep the proper ones.
	 */
	private void initAdjustUserProp() {
		String user = (String) props.get("user");
		if (user ==null) return;
		if (user.endsWith("@trk") || user.endsWith("@temp") || StrUtils.isNumber(user)) {
			props.putIfAbsent("trk", user);
			props.remove("user");
		} else if (user.contains("@")) {
			props.putIfAbsent("uxid", user);
		}		
	}

	@Override
	public String toString() {
		return "DataLogEvent[count=" + count + ", eventType=" + Printer.str(getEventType()) + ", props=" + props + ", id=" + id
				+ ", dataspace=" + dataspace + "]";
	}

	/**
	 * Unique based on dataspace and (groupById OR eventType and properties). 
	 * 
	 * Does NOT include time though! So its NOT guaranteed unique!
	 * The storage layer should bucket identical IDs within the same time-bucket.
	 * If you need more fine-grained saving - then add a nonce / timestamp.
	 * 
	 * @param groupById Can be null 
	 * @param dataLogEvent
	 * @return ID -- if groupById is given, this is returned as-is (and time will not later be added).
	 * If groupById is null, then this is based on eventType + props (and time _will_ later be added).
	 */
	private String makeId(String groupById) {
		// If group-by-ID is given, use that
		if (groupById!=null && ! groupById.isEmpty()) {
			return groupById;	
		}
		// otherwise, make a blob of eventType + properties
		if (props==null || props.isEmpty()) {
			return getEventType()[0];
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
		return dataspace+"_"+getEventType()[0]+"_"+StrUtils.md5(txt);
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
	 * Because this has to handle big data, we economise and store either n or v, not both.
	 * {k: string, n: ?number, v: ?string}
	 * 
	 * Does NOT include dataspace
	 */
	@Override
	public Map<String,Object> toJson2() {
		Map map = new ArrayMap();		
//		map.put("dataspace", dataspace); This is given by the index
		map.put(EVT, getEventType());
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
				// Defend against numbers in the wrong format causing e.g. 
				// "mapper_parsing_exception","reason":"failed to parse [dt]"}], "number_format_exception","reason":"For input string: \"4.205515\"
				if (proptype == Long.class || proptype == Integer.class) {
					if (v instanceof Long || v instanceof Integer) {
						// OK
					} else {
						double nv = MathUtils.toNum(v);
						if (nv != Math.round(nv)) {
							Log.w("DataLogEvent", "Dropping non-int number (bad format, possibly wrong units): "+pv.getKey()+" = "+v+" in "+this);
							// ?? log a separate error event?
							continue;
						}
					}
				}
				// store the common prop
				map.put(pv.getKey(), v);
			} else {
				// not common - use key-value
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
		}
		map.put("props", propslist);
		return map;
	}

	public void setExtraResults(Map map) {
		props.put("xtra", map);
	}

	/**
	 * HACK This method inverts {@link #toJson2()}
	 * @param _dataspace
	 * @param esResult
	 * @return
	 */
	public static DataLogEvent fromESHit(CharSequence _dataspace, Map<String,?> hit) {
		String[] etypes = getEventTypeFromMap(hit);
		
		Time _time = new Time((String)hit.get("time"));
		double cnt = MathUtils.toNum(hit.get("count"));
		Map<String, Object> properties = new ArrayMap();
		// common props
		List<String> NOT_COMMON = Arrays.asList(EVT, "time","count", "dataspace", "id", "props");
		for(Entry<String, ?> pv : hit.entrySet()) {
			if (NOT_COMMON.contains(pv.getKey())) {
				continue;
			}
			Object v = pv.getValue();
			if ( ! Utils.truthy(v)) continue;
			properties.put(pv.getKey(), v);			
		}		
		// other props
		List<Map> hprops = Containers.asList(hit.get("props"));
		if (hprops!=null) {
			// NB: this copy from prop-list to map will keep the last value for a given prop
			//  -- which is what we want given how our ES update can create duplicates, and last = most recent.
			for (Map hp : hprops) {
				String k = (String) hp.get("k");
				Object n = hp.get("n");
				Object v = hp.get("v");
				properties.put(k, Utils.or(v, n));
			}
		}
		String gby = null;
		// OK - build it
		DataLogEvent dle = new DataLogEvent(_dataspace, gby, cnt, etypes, properties);
//		dle.id = id;
		dle.time = _time;
		return dle;
	}

	/**
	 * 	coerce eventType to String[]
	 * @param hit
	 * @return
	 */
	static String[] getEventTypeFromMap(Map<String, ?> hit) {
		Object etype = hit.get(EVT);
		if (etype==null) throw new IllegalArgumentException("Not a DataLogEVent "+hit);
		String[] etypes;
		if (etype instanceof String) {
			etypes = new String[] {(String)etype};
		} else if (etype instanceof Collection) {
			etypes = (String[]) ((Collection) etype).toArray(StrUtils.ARRAY);
		} else {
			assert etype.getClass().isArray() : etype;
			etypes = Containers.asList(etype).toArray(StrUtils.ARRAY);
		}
		return etypes;
	}

	public String[] getEventType() {
		return evt;
	}

	public String getEventType0() {
		assert evt != null && evt.length == 1 : "No evt "+this;
		return evt[0];
	}

	/**
	 * Best practice is to set props in the constructor and 
	 * NOT to modify the event after construction. But you can if you must.
	 * @param prop
	 * @param value
	 */
	public void putProp(String prop, Object value) {
		Utils.check4null(prop);
		props.put(prop, value);
	}

	public Time getTime() {
		return time;
	}

	/**
	 * @return A nice usually flat map. This is NOT what we store in ES.
	 */
	public Map<String,Object> toJsonPublic() {
		ArrayMap map = new ArrayMap(
			"count", count,
			"dataspace", dataspace,
			"evt", evt,
			"id", id,
			"time", time);
		// props
		if (props==null) return map;
		for(Entry<String, ?> pv : props.entrySet()) {
			Object v = pv.getValue();
			if (v==null) continue;
			// NB: conceivably v could be an object! But that should be rare
			map.put(pv.getKey(), v);
		}
		return map;
	}
	
}
