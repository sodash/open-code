package com.winterwell.depot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.IProperties;
import com.winterwell.utils.Key;
import com.winterwell.utils.Printer;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Properties;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.LineReader;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.XStreamUtils;

/**
 * The keys in Depot. Describes how to make a data artifact.
 * <p>
 * fields/properties must be primitives or other ArtifactDescriptions! Complex
 * objects would risk memory leaks and serialisation problems.
 * <p>
 * How to get a Desc for an artifact:
 *
 * - If the artifact implements {@link IHasDesc}, then use that.
 * - Use {@link Desc#getDescCache()}
 * - Make a new one.
 *
 * @param <X>
 *            type of artifact
 * @author Daniel
 * @testedby DescTest
 *
 *           <p>
 *           <b>Copyright & license</b>: (c) Winterwell Associates Ltd, all
 *           rights reserved. This class is NOT formally a part of the
 *           com.winterwell.utils library. In particular, licenses for the
 *           com.winterwell.utils library do not apply to this file.
 */
public final class Desc<X> implements IProperties, Serializable, Comparable<Desc> {

	
	
	/**
	 * For debugging -- the recent history of this object
	 */
	public transient Object history;

	/**
	 * @deprecated When we simplify Desc, this will likely go.
	 * 
	 * Default: join Files with concatenation. Override to handle other types!
	 *
	 * @param bits
	 * @return bits glued together
	 */
	protected X join(List<X> bits) {
		assert type == File.class : this;
		assert range != null : this;
		try {
			File concat = File.createTempFile("depot", ".join");
			boolean append = false;
			for (Object bit : bits) {
				// NB: need to create fresh output-streams as they get closed by
				// copy()
				FileOutputStream outStream = new FileOutputStream(concat,
						append);
				FileUtils.copy(new FileInputStream((File) bit), outStream);
				append = true;
			}
			return (X) concat;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * @deprecated When we simplify Desc, this will likely go.
	 * 
	 * Default: filter Files with 1st word=Java timestamp Override to handle
	 * other types!
	 *
	 * @param slice
	 *            Never null
	 * @return The portion of slice that fits range, or null
	 */
	protected X filter(X slice) {
		assert type == File.class : this; // over-ride to handle other types
		assert range != null : this;
		try {
			File fltrd = File.createTempFile("depot", ".fltr");
			LineReader lr = new LineReader((File) slice);
			BufferedWriter w = FileUtils.getWriter(fltrd);
			Pattern digits = Pattern.compile("^\\d+");
			for (String line : lr) {
				Matcher m = digits.matcher(line);
				if (!m.find())
					continue;
				Time v = new Time(Long.valueOf(m.group()));
				if (range.contains(v)) {
					w.write(line);
					w.write('\n');
				}
			}
			w.close();
			return (X) fltrd;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	private static final long serialVersionUID = 1L;

	private static final int MAX_KEY_LENGTH = 128;
	private static final int MAX_VALUE_LENGTH = 254;

	/**
	 * Could be stored on any machine. Should be filed under /any/
	 */
	public static final String ANY_SERVER = "any";

	/**
	 * This machine! This is what {@link #server} defaults to.
	 *
	 * TODO should we sym-link local & my-server?? Or make local=myserver?
	 */
	public static final String LOCAL_SERVER = "local";

	/**
	 * Magic server value that maps to the default remote depot e.g. datastore.soda.sh, as set by DepotConfig.
	 * See DepotConfig#defaultRemoteHost
	 */
	public static final String CENTRAL_SERVER = "central";

	/**
	 * @return store here -- under the server name
	 */
	public static final String MY_SERVER() {
		return WebUtils.fullHostname();
	}

	@Deprecated // When we simplify Desc, this will likely go.
	public static enum KSerialiser {
		JAVA, 
		/** The default, ie null = XStream */ XSTREAM
	}

	/**
	 * Use a weak reference here because these are being bunged into values
	 * above. See docs for WeakHashMap
	 */
	transient volatile WeakReference<X> boundValue;

	private String name;

	private String tag;

	private final Map<String, Object> properties = new HashMap();

	/**
	 * Where should this be stored? This is part of the id -- so 
	 * e.g. myserver1/free_memory != myserver2/free_memory.
	 * local by default.
	 */
	String server = LOCAL_SERVER;

	/**
	 * @deprecated When we simplify Desc, this will likely go.
	 * @see #setServerHint(String). Normally null
	 */
	transient String serverHint;

	private Class type;

	/**
	 * @deprecated When we simplify Desc, this will likely go, or become a String.
	 * 
	 * null by default. If not-null, this should be part of the id.
	 */
	private Number version;

	private transient String id;

	/**
	 * Copy constructor. Does NOT copy notes and bound-value (if any)
	 *
	 * @param desc
	 *            Cannot be null
	 */
	public Desc(Desc<?> desc) {
		this.gzip = desc.gzip;
		this.maxAge = desc.maxAge;
		this.name = desc.getName();
		properties.putAll(desc.properties);
		this.range = desc.range;
		this.ser = desc.ser;
		this.server = desc.server;
		// symlink??
		this.tag = desc.getTag();
		this.type = desc.getType();
		this.version = desc.getVersion();
		assert type != null : desc;
	}

	/**
	 *
	 * @param name
	 *            Can be null (though name must be set before storing)
	 * @param type
	 *            Cannot be null. Artifacts will be grouped by this,
	 *            so its best to specify a super-class / interface. 
	 */
	public Desc(String name, Class<? extends X> type) {
		this.type = type;
		this.name = name;
		assert type != null : name;		
		history = ReflectionUtils.stacktrace();
	}

	/**
	 * Add details about an upstream artifact to this artifact's description.
	 * E.g. suppose artifact A (which may exist in several versions) is used in
	 * making artifact B, then B should record A's details.
	 *
	 * @param prefix
	 * @param upstreamDesc 
	 * NB: the ID for this is lazily evaluated - so it can be unfixed, and will remain unfixed until getId() is called, e.g. by Depot.get/put.
	 */
	public final void addDependency(String role, Desc upstreamDesc) {
		assert upstreamDesc != null;
		// add a key which will identify the dependency
		// (this allows for multiple dependencies to be added)
		Key<Desc> key = new Key("d:" + role);
		put(key, upstreamDesc);
		checkUnset();
	}

	/**
	 * Convenience when working with bound artifacts
	 *
	 * @param upstreamArtifact
	 */
	public final void addDependency(String role, Object upstreamArtifact) {
		assert !(upstreamArtifact instanceof Desc);
		Desc desc = descCache.getDescription(upstreamArtifact);
		assert desc != null : upstreamArtifact;
		addDependency(role, desc);
	}

	/**
	 * Set by new DescCache(), which is called by new Depot(). So: once a depot
	 * has been created, this should not be null.
	 */
	static IDescCache descCache = null;

	/**
	 * Called by depot.get()/put(). Not threadsafe.
	 *
	 * @param artifact
	 *            Must not be null
	 * @return true if an edit to bindings is made. false if no edit is made, or no binding happens.
	 */
	protected final boolean bind(X artifact) {
		assert artifact != null;
		return descCache.bind(artifact, this);
	}

	@Override
	public final <T> boolean containsKey(Key<T> key) {
		return get(key) != null;
	}

	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Desc other = (Desc) obj;
		return getId().equals(other.getId());
	}

	@Override
	public final <T> T get(Key<T> key) {
		assert key != null;
		Object v = properties.get(key.getName());
		// if (v==null) v = defaultProperties.get(key);
		return (T) v;
	}

	/**
	 * @return bound-value (as set by Depot.put or get), or null
	 */
	public final X getBoundValue() {
		// NB: Setting this is managed via DescCache
		X bv = boundValue == null ? null : boundValue.get();
		return bv;
	}

	/**
	 * For use when re-constructing a Desc from it's ID. Warning: Some methods
	 * won't work. This is mainly intended for fetching objects from Depot.
	 *
	 * @param reconstruct
	 *            must always be true
	 * @param id
	 *            as previously saved from {@link #getId()}
	 * @param type
	 */
	public Desc(boolean reconstruct, String id, Class type) {
		assert reconstruct;
		assert id != null : type;
		this.id = id;
		this.type = type;
	}

	/**
	 * Core method: this is the unique id for this artifact.
	 *
	 * @return can be quite long! Includes project, type, key/values, version
	 *         and name. This is _always_ of the form:
	 *         tag/type/server/bumpf/dependency_hash/name.vVersion_/start_end
	 *         Where tag can be null, extra /s may appear depending on length,
	 *         and .vVersion and _/start_end are normally absent. If used,
	 *         start_end are in seconds (millisecond precision would be daft).
	 */
	public final String getId() {
		if (id != null)
			return id;
		StringBuilder sb = new StringBuilder();
		// Have a reliable starting form, suitable for directory structuring.
		sb.append(tag);
		sb.append('/');
		String sname = type.getSimpleName();
		if (Utils.isBlank(sname)) {
			sname = type.getSuperclass().getSimpleName();
		}
		sb.append(sname);
		sb.append('/');
		if (server != null) {
			sb.append(server);
			sb.append('/');
		}

		// key-value bumpf
		ArrayList<String> keys = new ArrayList(properties.keySet());
		ArrayMap<String, String> dependencies = new ArrayMap();
		if ( ! keys.isEmpty()) {
			// sort keys to ensure the same ordering each time
			Collections.sort(keys);
			for (Object _key : keys) {
				String key = _key.toString(); // handle old Keys from old Descs
				Object v = properties.get(key);
				if (v == null)
					continue;
				String vs = str(v);
				// Hashed stuff
				if (v instanceof Desc || key.startsWith("h:")) {
					// done later
					dependencies.put(key, vs);
					continue;
				}
				// normal "exposed" parameters
				sb.append(key);
				sb.append("=");
				sb.append(vs);
				sb.append("_");
			}
			StrUtils.pop(sb, 1);
			sb.append('/');
		}

		// Dependencies... listing these in full is v cumbersome.
		// They also often repeat stuff
		// E.g. the whole chain may share a key/value property
		// So let's hash them -- hopefully this is _safe enough_
		if (!dependencies.isEmpty()) {
			StringBuilder hashme = new StringBuilder();
			for (String key : dependencies) {
				String did = dependencies.get(key);
				hashme.append(key + "=" + did);
			}
			String hashed = StrUtils.md5(hashme.toString());
			sb.append(hashed);
			sb.append('/');
		}

		// too long? hash daft ids (but leave the name alone, so do that next)
		if (sb.length() > 512) {
			String partId = sb.toString();
			sb = new StringBuilder(partId.substring(0, 140)+StrUtils.md5(partId));
		}
		
		// name
		sb.append(name);
		// version (almost) last of all
		if (version != null) {
			sb.append(".v" + version);
		}
		// Range?
		if (range != null) {
			sb.append("_/");
			long s = range.first.getTime() / 1000;
			long e = range.second.getTime() / 1000;
			sb.append(s + "_" + e);
		}
		// save format
		if (ser == KSerialiser.JAVA)
			sb.append(".ser");
		if (gzip)
			sb.append(".gz");
		id = sb.toString();		
		return id;
	}

	protected String str(Object v) {
		if (v instanceof Desc) {
			return ((Desc) v).getId();
		}
		// Convert with Printer (which handles arrays etc. moderately well)
		// If that's not good enough, the user should do the conversion
		// themselves
		// and put in the String.
		String sv = Printer.toString(v);
		return sv;
	}

	/**
	 * @deprecated When we simplify Desc, this will likely go.
	 * You could think of this as the data-artifact's format
	 */
	KSerialiser ser;

	/**
	 * @return a key using the id. May have a very long name!
	 */
	public Key<X> getKey() {
		return new Key<X>(getId());
	}

	@Override
	public Collection<Key> getKeys() {
		return Properties.strings2keys(properties.keySet());
	}

	/**
	 * @return never null or blank.
	 */
	public final String getName() {
		return name;
	}

	public String getTag() {
		return tag;
	}

	public Class getType() {
		return type;
	}

	/**
	 * @deprecated When we simplify Desc, this will likely merge with #getVersion()
	 * @return
	 */
	public Number getVersion() {
		return version;
	}

	/**
	 * hashcode based on {@link #getId()}.
	 */
	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	@Override
	public boolean isTrue(Key<Boolean> key) {
		Boolean v = get(key);
		return v != null && v;
	}

	/**
	 * Set (or remove) the value for key.
	 * Once a key=value property has been set, it cannot be changed! This is to protect against accidentally 
	 * modifying Descs. Multiple calls to set the same value are harmless.
	 *
	 * @param key
	 *            Must not be null. The name should be short, as this can go
	 *            into a filename. Arbitrary limit: 128 chars
	 * @param value
	 *            If null, the key will be removed (does not affect default
	 *            properties). values should only be primitive objects or other
	 *            Descs (see Desc notes).<br>
	 *            Warning: Descs must be repeatable, so beware of using
	 *            toString() methods!
	 * @return The old mapping for this key, or null.
	 *         <p>
	 *         These are slightly different from normal. If you set a property
	 *         to the default, this will actually set it to null (allowing the
	 *         default to be returned in it's stead by get()). Justification:
	 *         suppose you conduct a set of experiments, then decide that what
	 *         was originally a constant will now be a parameter. You set the
	 *         old value as the default. The old experiments will still be
	 *         accessible.
	 */

	@Override
	@SuppressWarnings("unchecked")
	public <T> T put(Key<T> _key, T value) {
		return (T) put(_key.name, value);
	}

	/**
	 * Equivalent to {@link #put(Key, Object)}
	 */
	public Object put(String key, Object value) {
		checkUnset();
		assert key != null;		
		Object old = properties.get(key);
		if (Utils.equals(old, value)) {
			return old; // no-op
		}				
		assert old==null : "Cannot overrite "+key+"="+Printer.toString(old)+" w "+Printer.toString(value)+" in "+this;
		// An arbitrary limit on key-name length
		if (key.length() > MAX_KEY_LENGTH) {
			throw new IllegalArgumentException(key);
		}
		if (value == null)
			return properties.remove(key);
		// is it an allowed class?
		put2_checkValue(key, value);
		return properties.put(key, value);
	}
	
	/**
	 * Set a property on this artifact and all (if any) sub-modules.
	 * @param artifact
	 * @param key
	 * @param value
	 */
	public static void putRecursive(IHasDesc artifact, String key, Object value) {
		artifact.getDesc().put(key, value);	
		if (artifact instanceof ModularXML) {
			IHasDesc[] subs = ((ModularXML) artifact).getModules();
			if (subs==null) return;
			for (IHasDesc sub : subs) {
				putRecursive(sub, key, value);
			}
		}
	}

	transient boolean checkValueFlag = true;
	
	public void setCheckValueFlag(boolean checkValueFlag) {
		this.checkValueFlag = checkValueFlag;
	}
	
	/**
	 * TODO be more lenient, or delete this altogether. This is just for smoking
	 * out bad practice. It currently blocks plenty of no-problem cases (e.g.
	 * ArrayList<String>).
	 *
	 * @param value
	 */
	private void put2_checkValue(String key, Object value) {
		if ( ! checkValueFlag) return;
		Class<? extends Object> vc = value.getClass();
		assert vc.isPrimitive() || vc == Class.class || vc == String.class
				|| vc == Double.class || vc == Long.class
				|| vc == Integer.class || vc == Boolean.class
				|| vc == Desc.class || vc == String[].class
				|| vc == int[].class || vc == double[].class
				|| vc==File.class
				|| value instanceof Enum || value instanceof Time : vc;
		// Don't check toString() length on Desc, as that could prematurely
		// generate an id
		if (value instanceof Desc)
			return;
		// not for hashed stuff
		if (key.startsWith("h:"))
			return;
		// arbitrary limit on length to protect the file system
		if (value.toString().length() > MAX_VALUE_LENGTH) {
			throw new IllegalArgumentException("Too long: " + value);
		}
	}

	public void putAll(IProperties props) {
		for (Key k : props.getKeys()) {
			put(k, props.get(k));
		}
	}

	public Desc<X> putAll(Map props) {
		for (Object k : props.keySet()) {
			put(k.toString(), props.get(k));
		}
		return this;
	}

	/**
	 * Change the name. Use-case: when constructing a Desc by modifying another
	 * Desc.
	 *
	 * @param name
	 */
	public void setName(String name) {
		checkUnset();
		this.name = name;
		assert ! Utils.isBlank(name) : name + " " + type;
	}

	/**
	 * @deprecated When we simplify Desc, this will likely go.
	 * 
	 * If this artifact naturally lives on a particular server.<br>
	 * This is part of the id -- so 
	 * e.g. the artifact myserver1/free_memory != myserver2/free_memory <br>
	 * 
	 * {@link #LOCAL_SERVER} by default.
	 * 
	 * @param server
	 *            The hostname, e.g. "bear.soda.sh", or {@link #LOCAL_SERVER} or
	 *            {@link #CENTRAL_SERVER}.<br>
	 *            Defaults to {@link #LOCAL_SERVER}
	 */
	public void setServer(String server) {
		this.server = server;
		// Please use "bear.soda.sh", not "bear"!
		if (!server.contains(".") && !server.equals(ANY_SERVER)
				&& !server.equals(CENTRAL_SERVER)
				&& !server.equals(LOCAL_SERVER)) {
			Log.w("depot", "Dubious server " + server);
		}
	}

	/**
	 * @param tag
	 *            Typically the Eclipse project name
	 */
	public Desc<X> setTag(String tag) {
		checkUnset();
		this.tag = tag;
		// ??Should this be recursive?? 		
		return this;
	}

	private void checkUnset() {
		if (id == null) return;
		Log.escalate(new IllegalStateException("id already set for "+this+" "+ReflectionUtils.getSomeStack(12)));
		id = null;		
	}

	/**
	 * @deprecated When we simplify Desc, this will likely merge with copy().
	 * 
	 * For changing type - used in conjunction with the copy constructor.
	 *
	 * @param type
	 */
	public void setType(Class<? extends X> type) {
		checkUnset();
		this.type = type;
	}

	/**
	 * Note: setting this to a dynamic value -- e.g. new Time() -- will create a
	 * new separate version of the artifact each time. Don't do it unless that's
	 * what you want.
	 *
	 * @param version
	 */
	public void setVersion(Number version) {
		this.version = version;
	}

	/**
	 * A short descriptive String. Do NOT use this in place of id! It is
	 * truncated and not unique.
	 * NB: this does not lead to id being set.
	 */
	@Override
	public String toString() {
		boolean blankId = id == null;
		String s = "Desc[" + name + " " + StrUtils.ellipsize(getId(), 140)
				+ "]";
		// reset to null
		if (blankId) id = null;
		return s;
	}

	@Deprecated // When we simplify Desc, this will likely go.
	Period range;

	@Deprecated // When we simplify Desc, this will likely go.
	boolean gzip;

	/**
	 * false normally. If true, this Desc should ONLY be used for saving
	 * sym-links
	 */
	boolean symlink;
	
	boolean readOnly;

	Dt maxAge;

	transient com.winterwell.depot.MetaData metadata;

	/**
	 * Set if an IMerger is to be used. Normally null.
	 */
	private transient X before;
	
	/**
	 * The "before" freshly-loaded state of the artifact.
	 * Set if an IMerger is to be used. Normally null.
	 * @return
	 */
	X getBefore() {
		return before;
	}

	/**
	 * Exposes the {@link IDescCache}
	 */
	public static IDescCache getDescCache() {
		assert descCache != null : "Depot not initialised";
		return descCache;
	}

	/**
	 * If this artifact has been bound to a description, then return it.
	 *
	 * @param artifact
	 * @return desc or null
	 */
	public static <X> Desc<X> desc(X artifact) {
		return getDescCache().getDescription(artifact);
	}

	/**
	 * If this artifact naturally lives on a particular server.
	 *
	 * @param server
	 *            The hostname, e.g. "bear.soda.sh", or {@link #LOCAL_SERVER} or
	 *            {@link #CENTRAL_SERVER}.<br>
	 *            Defaults to {@link #LOCAL_SERVER}
	 */
	public String getServer() {
		return server;
	}

	/**
	 * Used by Depot. Not threadsafe.
	 */
	protected void unbind() {
		getDescCache().unbind(getBoundValue(), this);
		boundValue = null;
		before = null;
	}

	/**
	 * @return Descs which this artifact depends on. This is recursive. It
	 *         relies on the dependency Descs being added as Desc values. Can be
	 *         empty, never null.
	 */
	public Set<Desc> getDependencies() {
		Set<Desc> set = new HashSet();
		getDependencies2(set);
		return set;
	}

	void getDependencies2(Set<Desc> set) {
		Collection<Object> vs = properties.values();
		for (Object object : vs) {
			if (object instanceof Desc) {
				Desc d = (Desc) object;
				set.add(d);
				d.getDependencies2(set);
			}
		}
	}

	/**
	 * @deprecated When we simplify Desc, this will likely go.
	 * 
	 * Ranged data can be split across several files. This makes it good for
	 * long running data-streams, as we can fairly quickly access the desired
	 * range.
	 * <p>
	 * Pieces of ranged data are stored as normal (using setRange() to specify
	 * their range), and accessed as normal using Depot.get().
	 * <p>
	 * Behind the scenes, the desired artifact is created on access using
	 * {@link #join(List)} and {@link #filter(Object)} (which should be
	 * over-ridden to support your data type).
	 *
	 * @param start
	 * @param end
	 * @see com.winterwell.depot.FileStore#getRangedData()
	 */
	public void setRange(Time start, Time end) {
		this.range = new Period(start, end);
	}

	/**
	 * @deprecated When we simplify Desc, this will likely go.
	 * @return
	 */
	public Period getRange() {
		return range;
	}

	/**
	 * @deprecated When we simplify Desc, this will likely go.
	 * 
	 * Put a key/value pairing which (like dependencies) should be stored as
	 * part of one big hashcode.
	 *
	 * @param key
	 * @param value
	 */
	public <V> void putHash(Key<V> key, V value) {
		// vs = str(value); Should we hash it here? That would lead to double
		// hashing (a bit wasteful) TODO BUT otherwise the Desc can get bloated
		if (key.getName().startsWith("h:")) {
			put(key, value);
		} else {
			put("h:" + key, value);
		}
	}

	/**
	 * Give Descs a predictable ordering, using their id.
	 */
	@Override
	public int compareTo(Desc o) {
		if (o == null)
			return -1;
		return getId().compareTo(o.getId());
	}

	public boolean containsKey(String key) {
		return containsKey(new Key(key));
	}

	/**
	 * Use XStream's xml serialisation of the value. Store this as a hashed key.
	 * Use-case: You have a config object, and you wish to do versioning on all the fields.
	 * This makes it easy.
	 *
	 * @param config
	 */
	public void setVersionStamp(Object config) {
		assert config != null : this;
		assert ! properties.containsKey(CONFIG_KEY.name) : config;
		String vrsn = XStreamUtils.serialiseToXml(config);
		putHash(CONFIG_KEY, vrsn);
	}

	public String getVersionStamp() {
		return get(CONFIG_KEY);
	}

	public static final Key<String> CONFIG_KEY = new Key("h:_"); // begin with an h for
															// hash, use "_" as
															// the name 'cos
															// what should we
															// call this?

	/**
	 * Set true to mark this artifact as being a symlink.
	 * Usage:
	 * <code><pre>
	 * depot.put(desc1, artifact);
	 * desc2.setSymLink(true);
	 * depot.put(desc2, desc1);
	 * // Now
	 * artifact = depot.get(desc2);
	 * </pre><code>
	 *
	 * NB: Why don't we just infer symlink status? Having an explicit setting acts as a safety check.
	 * @param symlink
	 */
	public void setSymLink(boolean symlink) {
		this.symlink = symlink;
	}

	/**
	 * @deprecated When we simplify Desc, this will likely go.
	 * 
	 * Give Depot a hint for which server to find this on. <br>
	 * Use case 1: if server=any, or if you're trying to fetch a backup copy.<br>
	 * Use case 2: to do a local put of a holding-place object (so code runs smoothly)
	 * but avoid putting to the remote server. Use {@link #LOCAL_SERVER}
	 * <p>
	 * Note: this value is transient
	 * @param serverHint e.g. sampler.soda.sh
	 */
	public void setServerHint(String serverHint) {
		this.serverHint = serverHint;
	}

	/**
	 * If true, it is an error to try and Depot.put() with this. This is transient.
	 * Use case: to avoid remote-puts
	 * @param readOnly
	 */
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	/**
	 * If true, it is an error to try and Depot.put() with this. This is transient.
	 * Use case: to avoid remote-puts
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * True if all of this Desc's settings match up with the other desc.
	 * The other desc can contain extra settings though.
	 * Use-case: for filtering partial matches.
	 * @param other 
	 */
	public boolean partialMatch(Desc other) {
		// TODO a proper unify algorithm
		if (type!=null && ! type.equals(other.getType())) return false;
		if (tag!=null && ! tag.equals(other.getTag())) return false;
		if (version!=null && ! version.equals(other.version)) return false;
		if (server!=null && ! server.equals(other.server)) return false;
		if (name!=null && ! name.equals(other.name)) return false;
		for(String k : properties.keySet()) {
			Object v = properties.get(k);
			Object v2 = other.get(new Key(k));
			if ( ! Utils.equals(v, v2)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * If set, this over-rides the default in DepotConfig.
	 * Note: This is not part of the id.
	 * @param dt
	 */
	public void setMaxAge(Dt dt) {
		maxAge = dt;
	}

	/**
	 * @return Can be null. Provides a place to store metadata for a desc/artifact.
	 */
	public com.winterwell.depot.MetaData getMetaData() {
		return metadata;
	}

	/**
	 * Take a snapshot of this object to be used when saving it to do a merge.
	 * Repeated calls have no effect.
	 * Recurses on sub-modules.
	 * ONLY works with Depot.update() -- not put()
	 */
	public void markForMerge() {
		if (before!=null) return;
		remarkForMerge();
	}

	/**
	 * {@link #markForMerge()} without the repeated-calls = no-op guard.
	 * This will always take a fresh snapshot.
	 * Use-case: after a save, to catch the next set of edits.
	 */
	void remarkForMerge() {
		X bv = getBoundValue();
		assert getBoundValue()!=null;
		// NB: copy will skip over modules, via ModularConvertor
		before = Utils.copy(bv);
		// recurse on sub-modules
		if (getBoundValue() instanceof ModularXML) {
			IHasDesc[] modules = ((ModularXML)bv).getModules();
			for (IHasDesc module : modules) {
				module.getDesc().markForMerge();
			}
		}		
	}

	/**
	 * @return If true, the id has been set (and should not be edited)
	 */
	public boolean isSet() {
		return id != null;
	}

	/**
	 * @deprecated When we simplify Desc, this will likely go.
	 * Equivalent to new {@link #Desc(Desc)}
	 */
	public Desc copy() {
		return new Desc(this);
	}

	/**
	 * Mark the id as unset, allowing for further edits. Be careful with this!
	 * @return true if it was set
	 */
	public boolean unset() {
		boolean wasSet = id!=null;
		id = null;
		return wasSet;
	}

}
