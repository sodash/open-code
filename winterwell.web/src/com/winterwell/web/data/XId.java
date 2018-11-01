package com.winterwell.web.data;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.LoginDetails;

/**
 * An id for an external service.
 *
 * @see DBLogin (database backed)
 * @see LoginDetails (this is lighter and a bit different)
 * @author daniel
 * @testedby XIdTest
 */
public final class XId implements Serializable, IHasJson, CharSequence, Comparable<XId> {
	private static final long serialVersionUID = 1L;

	/**
	 * Group 1 is the name, group 2 is the service
	 */
	public static final Pattern XID_PATTERN = Pattern.compile(
			"(\\S+)@([A-Za-z\\.]+)?");

	/**
	 * Company-type. Added to the start of the XId name for SOME data-sources,
	 * to avoid any overlap with other types.  
	 */
	public static final String WART_C = "c_";
	/**
	 * Person-type. Added to the start of the XId name for SOME data-sources,
	 * to avoid any overlap with other types.  
	 */
	public static final String WART_P = "p_";
	/**
	 * Video-type. Added to the start of the XId name for SOME data-sources,
	 * to avoid any overlap with other types.
	 */
	public static final String WART_V = "v_";
	/**
	 * Group-type. Added to the start of the XId name for SOME data-sources,
	 * to avoid any overlap with other types.
	 */
	public static final String WART_G = "g_";

	/**
	 * XId for unknown person + unspecified service
	 */
	public static final XId ANON = new XId(WART_P+"anon@unspecified", false);
	
	public final String name;
	public final String service;

	/**
	 * @param name Canonicalises via {@link IPlugin#canonical(XId, KKind)}
	 * @param plugin
	 */
	public XId(String name, String service, IDoCanonical plugin) {
		this(name, null, service, plugin);
	}
	
	/**
	 * 
	 * @param name
	 * @param kind Can be null
	 * @param service
	 * @param plugin
	 */
	public XId(String name, Object kind, String service, IDoCanonical plugin) {
		if (plugin != null) name = plugin.canonical(name, kind);
		this.name = name;
		this.service = service;
		// null@twitter is a real user :( c.f. bug #14109 
		assert notNullNameCheck() : name;		
		assert name != null;
		assert ! service.contains("@") : service;
	}

	static Map<String,IDoCanonical> service2canonical = IDoCanonical.DUMMY_CANONICALISER;
	
	/**
	 * Use with {@link IDoCanonical#DUMMY_CANONICALISER} to allow XIds to be used _without_ initialising Creole.
	 * @param service2canonical
	 */
	public static void setService2canonical(
			Map<String, IDoCanonical> service2canonical) 
	{
		XId.service2canonical = service2canonical;
	}

	/**
	 * @param name
	 * @param service
	 */
	public XId(String name, String service) {
		this(name, service, service2canonical.get(service));
	}

	/**
	 * Usage: to bypass canonicalisation and syntax checks on name.
	 * This is handy where the plugin canonicalises for people, but XIds
	 * are used for both people and messages (e.g. Email).
	 *
	 * @param name
	 * @param service
	 * @param checkName Must be false to switch off the syntax checks performed by
	 * {@link #XId(String, String)}.
	 */
	public XId(String name, String service, boolean checkName) {
		this.service = service;
		this.name = name;
		assert notNullNameCheck() : name+"@"+service;
		assert ! checkName;
		assert ! service.contains("@") : service;
		return;
	}

	/**
	 * Convert a name@service String (as produced by this class) into
	 * a XId object.
	 * @throws IllegalArgumentException if id cannot be parsed
	 */
	public XId(String id) {
		this(id, (Object)null);
	}
	
	/**
	 * 
	 * @param id e.g. "alice@twitter"
	 * @param kind e.g. KKind.Person
	 */
	public XId(String id, Object kind) {
		int i = id.lastIndexOf('@');
		if (i <= 0) {
			throw new IllegalArgumentException("Invalid XId " + id);
		}
		this.service = id.substring(i+1);
		// Text for XStream badness
		assert ! id.startsWith("<xid>") : id;
		// HACK: canonicalise here for main service (helps with boot-strapping)
		if (isMainService()) {
			this.name = id.substring(0, i).toLowerCase();
			assert notNullNameCheck() : id;
			return;
		}
		// a database object?
		if (service.startsWith("DB")) {
//			try { // commented out to cut creole dependency
//				assert Fields.CLASS.fromString(service.substring(2)) != null : service;
//			} catch (ClassNotFoundException e) {
//				throw Utils.runtime(e);
//			}
			this.name = id.substring(0, i);
			assert notNullNameCheck() : id;
			return;
		}
		
		IDoCanonical plugin = service2canonical.get(service);
		String _name = id.substring(0, i);
		this.name = plugin==null? _name : plugin.canonical(_name, kind);
		assert notNullNameCheck() : id;
	}
	
	private boolean notNullNameCheck() {
		if (name==null || name.length()==0) return false;
		if (name.equals("null") && ! "twitter".equals(service)) return false;
		return true;
	}

	/**
	 * Convert a name@service String (as produced by this class) into
	 * a XId object.
	 * @param canonicaliseName Must be false, to switch off using plugins to canonicalise
	 * the name.
	 */
	public XId(String id, boolean canonicaliseName) {
		assert ! canonicaliseName;
		int i = id.lastIndexOf('@');
		// handle unescaped web inputs -- with some log noise 'cos we don't want this
		if (i==-1 && id.contains("%40")) {
			Log.e("XId", "(handling smoothly) Unescaped url id: "+id);
			id = WebUtils2.urlDecode(id);
			i = id.lastIndexOf('@');
		}
		assert i>0 : id;
		this.service = id.substring(i+1);
		this.name = id.substring(0, i);
		assert notNullNameCheck() : id;
	}

	public XId(String name, IDoCanonical plugin) {
		this(name, null, plugin.getService(), plugin);
	}
	
	public XId(String name, Object kind, IDoCanonical plugin) {
		this(name, kind, plugin.getService(), plugin);
	}

	/**
	 * name@service
	 * This is the inverse of the String constructor, i.e.
	 * xid equals new Xid(xid.toString()). So you can use it for storage.
	 */
	@Override
	public String toString() {
		return name+"@"+service;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = name.hashCode();
		result = prime * result + service.hashCode();
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass()) {
			// Probably a bug!
//			Log.d("xid", "XId.equals() "+this+" to "+obj.getClass()+" "+ReflectionUtils.getSomeStack(8));
			return false;
		}
		XId other = (XId) obj;
		if (!name.equals(other.name))
			return false;
		if (!service.equals(other.service))
			return false;
		return true;
	}

	/**
	 * Never null
	 */
	public String getName() {
		return name;
	}

	public String getService() {
		return service;
	}
	
	/**
	 * TODO poke this value on JVM start-up
	 */
	static String MAIN_SERVICE = initMAIN_SERVICE();
	
	public boolean isMainService() {
		return MAIN_SERVICE.equals(service);
	}

	private static String initMAIN_SERVICE() {
		// NB: This property gets set by AWebsiteConfig
		String s = System.getProperty("XId.MAIN_SERVICE");
		if (s!=null) return s;
		// HACK -- known WW instances
		File dir = FileUtils.getWorkingDirectory();
		if (dir.getName().equals("creole")) {
			return "platypusinnovation.com";
		}			
		return "soda.sh";
	}

	/**
	 * @return true for rubbish XIds of the form "row-id@soda.sh" or "foo@temp"
	 */
	public boolean isTemporary() {
		return isService("temp") || (isMainService() && StrUtils.isNumber(name));
	}

	/**
	 * Convenience method
	 * @param other
	 * @return true if the services match
	 */
	public boolean isService(String _service) {
		return this.service.equals(_service);
	}




	/**
	 * @return The name, minus any Hungarian warts SoDash has added
	 * to ensure uniqueness between types.
	 */
	public String dewart() {
		// person?
		if (name.startsWith(WART_P)) return name.substring(WART_P.length());
		if (name.startsWith(WART_G)) return name.substring(WART_G.length());
		if (name.startsWith(WART_V)) return name.substring(WART_V.length());
		if (name.startsWith(WART_C)) return name.substring(WART_C.length());
		// TODO do we use any others?
		return name;
	}

	public boolean hasWart(String wart) {
		return name.startsWith(wart);
	}

	/**
	 * Convenience for ensuring a List contains XId objects.
	 * @param xids May be Strings or XIds or IHasXIds (or a mix). Must not be null.
	 * Note: Strings are NOT run through canonicalisation -- they are assumed to be OK!
	 * @return a copy of xids, can be modified 
	 */
	public static ArrayList<XId> xids(Collection xids) {
		return xids(xids, false);
	}
	
	/**
	 * Convenience for ensuring a List contains XId objects. Uses {@link #xid(Object, boolean)}
	 * @param xids May be Strings or XIds (or a mix).
	 * @return a copy of xids, can be modified 
	 */
	public static ArrayList<XId> xids(Collection xids, boolean canonicalise) {
		final ArrayList _xids = new ArrayList(xids.size());
		for (Object x : xids) {
			if (x==null) continue;
			XId xid = xid(x, canonicalise);
			_xids.add(xid);
		}
		return _xids;
	}
	/**
	 * Flexible type coercion / constructor convenience.
	 * @param xid Can be String (actually any CharSequence) or XId or IHasXId or null (returns null). Does NOT canonicalise
	 * */
	public static XId xid(Object xid) {
		return xid(xid, false);
	}
	
	public static XId xid(Object xid, boolean canon) {
		if (xid==null) return null;
		if (xid instanceof XId) return (XId) xid;		
		if (xid instanceof CharSequence) {
			return new XId(xid.toString(), canon);
		}
		IHasXId hasxid = (IHasXId) xid;
		return hasxid.getXId();
	}
	
	@Override
	public String toJSONString() {
		return new SimpleJson().toJson(toString());
	}

	@Override
	public Object toJson2() throws UnsupportedOperationException {
		return toString();
	}

	@Override
	public int length() {
		return toString().length();
	}

	@Override
	public char charAt(int index) {
		return toString().charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return toString().subSequence(start, end);
	}

	@Override
	public int compareTo(XId o) {
		return toString().compareTo(o.toString());
	}


	
}
