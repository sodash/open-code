package com.winterwell.utils.io;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.winterwell.depot.IInit;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * Parse Unix style command line arguments. Also handles Java properties objects
 * (as created from .properties files).
 * 
 * Supports Map fields, via the format "field.key=value"
 * However these must be Map<String,String>, as there is no type info to convert the value.
 * 
 * Sets fields annotated with {@link Option} in a config object.
 * 
 * <h3>Best Practice Example</h2>
 * <pre><code>
 * public static void main(String[] args) {
 * 	MyConfig config = new ArgsParser(new MyConfig())
 * 		// Load a properties file
 * 		.set(new File("my.properties")
 * 		// Then load any system properties
 * 		.setFromSystemProperties("MyApp")
 * 		// Then load from command line args
 * 		.setFromMain(args)
 *		// done
 * 		.get();
 *	// Now do your thing...
 * }
 * </code></pre>
 * @author daniel
 * 
 * @testedby  ConfigBuilderTest}
 */
public class ConfigBuilder {

	private static final Map<Class,ISerialize> convertors = new HashMap();
	
	public static void addConvertor(Class klass, ISerialize fn) {
		assert klass != null;
		convertors.put(klass, fn);
	}
	
	/**
	 * 
	 * @param k
	 * @return true if k should be protected
	 */
	public static boolean protectPasswords(String k) {
		String[] sens = "login password pwd token auth key secret private".split(" ");
		String kl = k.toLowerCase();
		for(String sen : sens) {
			if (kl.contains(sen)) {
				return true;
			}
		}
		return false;

	}
	
	static List<Class> recognisedTypes = Arrays.asList((Class) 
			// Any enum
			Enum.class,
			// The primitives
			String.class,
			Double.class, Float.class, Integer.class, Long.class, double.class,
			float.class, int.class, long.class, Boolean.class, boolean.class,
			Class.class,
			// time objects
			Date.class, Time.class, Dt.class,
			// IO
			File.class, URI.class,
			// COllections
			List.class, Set.class, Map.class);
	
	/**
	 * Create an instance of type from a string representation.
	 * 
	 * @param type
	 * @param string
	 * @return
	 * @throws ParseException
	 */
	public static Object convert(Class<?> type, String string)
			throws ParseException {		
		try {
			// pluggable converter, such as an AField
			ISerialize conv = convertors.get(type);
			if (conv!=null) {
				Object v = conv.fromString(string);
				return v;
			}
			// Built-ins			
			if (type == String.class)
				return string;
			// Numbers
			if (Number.class.isAssignableFrom(type)) {
				if (type == Double.class)
					return Double.valueOf(string);
				if (type == Float.class)
					return Float.valueOf(string);
				if (type == Integer.class)
					return Integer.valueOf(string);
				if (type == Long.class)
					return Long.valueOf(string);
			}
			if (type.isPrimitive()) {
				if (type == double.class)
					return Double.valueOf(string);
				if (type == float.class)
					return Float.valueOf(string);
				if (type == int.class)
					return Integer.valueOf(string);
				if (type == long.class)
					return Long.valueOf(string);
				if (type == boolean.class)
					return Boolean.valueOf(string);
			}
			if (type == Boolean.class) {
				return Boolean.valueOf(string);
			}
			// Date
			if (type == Date.class)
				return DateFormat.getInstance().parse(string);
			if (type==Time.class) {
				return TimeUtils.parseExperimental(string);
			}
			if (type==Dt.class) {
				return TimeUtils.parseDt(string);
			}
			// File
			if (type == File.class)
				return new File(string);
			if (type==List.class)
				return StrUtils.split(string);
			// class
			if (type== Class.class) {
				return Class.forName(string);
			}
			// enum
			if (type.isEnum()) {				
				Object ev = Enum.valueOf((Class)type, string);
				return ev;
			}
			throw new IllegalArgumentException("Unrecognised type: " + type+" Odd? "+recognisedTypes.contains(type));
		} catch (Exception e) {
			// HACK: allow a few falsy values for null
			try {
				boolean y = Utils.yes(string);
				if ( ! y) {
					Log.i(LOGTAG, "Handling "+type.getSimpleName()+" "+string+" as null");
					return null;
				}
			} catch(Exception ex) {
				// not a truthy/falsy value then. oh well
			}
			throw new ParseException(e.getMessage(), 1);
		}
	}

	Map<Field, Object> field2default = new HashMap<Field, Object>();

	/**
	 * Reusable
	 */
	List<Field> requiredArgs;

	private final Object config;

	/**
	 * The tokens do NOT include the leading "-"
	 */
	final Map<String, Field> token2field = new HashMap<String, Field>();

	private Map<Field, ConfigBuilder> field2subparser = new HashMap();

	private boolean parseFlag;

	private List<String> remainderArgs;


	/**
	 * Create an ArgsParser which will set {@link Option} fields in the given
	 * config object.
	 * 
	 * @param config A config object to setup. NOT the class. 
	 */
	public ConfigBuilder(Object config) {
		assert config != null;
		assert ! (config instanceof Class) : "Input an object, not the class"; 
		this.config = config;
		// Setup token->field map
		Class<? extends Object> k = config.getClass();
		parseConfigObject(k);
	}	
	
	private boolean checkField(Class<?> type) {
		if (convertors.get(type) != null) return true;
		for(Class k : recognisedTypes) {
			if (ReflectionUtils.isa(type, k)) return true;
		}
		// does it have a String constructor?
		try {
			ISerialize fn = new SerializeViaConstructor(type);
			addConvertor(type, fn);
			Log.d(LOGTAG, "Add reflection based String convertor for "+type);
			return true;
		} catch (NoSuchMethodException | SecurityException e) {
			// nope
		}
		// TODO is it a recursive thing?
		return false;
	}


	public List<Field> getMissingProperties() {		
		List<Field> reqs = new ArrayList(getRequiredProperties());
		reqs.removeAll(source4setFields.keySet());
		return reqs;
	}

	public List<Field> getRequiredProperties() {
		if ( ! parseFlag) {
			parseConfigObject(config.getClass());
		}
		return requiredArgs;
	}

	/**
	 * @param helpForPostOptionsArguments e.g. "[file...] These will be processed. Default: loop over the current directory"
	 * @return a message describing the available options.
	 */
	public String getOptionsMessage(String helpForPostOptionsArguments) {
		StringBuilder msg = new StringBuilder();
		if ( ! Utils.isBlank(helpForPostOptionsArguments)) {
			msg.append("usage: [options...] "+helpForPostOptionsArguments + StrUtils.LINEEND);
		}
		msg.append("Options:" + StrUtils.LINEEND);
		List<Field> options = new ArraySet<Field>(token2field.values()).asList();
		Containers.sortBy(options, Field::getName);
		for (Field f : options) {
			Option arg = f.getAnnotation(Option.class);
			String desc = arg.description();
			if (Utils.isBlank(desc)) {
				desc = f.getName();
			}
			msg.append("  " + tokens(f, arg) + '\t' + desc);
			if (field2default.get(f) != null) {
				msg.append(" Default: " + field2default.get(f));
			}
			// special support for enums
			if (ReflectionUtils.isa(f.getType(), Enum.class)) {
				List evals = ReflectionUtils.getEnumValues(f.getType());
				msg.append(" "); StrUtils.join(msg, evals, "|");
			}
			msg.append(StrUtils.LINEEND);
		}
		return msg.toString();
	}

	/**
	 * After {@link #setFromMain(String[])}, this will hold the remaining unused arguments
	 * @return
	 */
	public List<String> getRemainderArgs() {
		return remainderArgs;
	}
	
	/**
	 * @param args
	 *            The arguments as passed into main() Can be null
	 * @return the non-options arguments, i.e. the ones after the options
	 * @throws RuntimeException
	 *             with a usage message. The missing arguments (if this is the
	 *             problem) can then be found via {@link #getMissingArguments()}
	 */
	public ConfigBuilder setFromMain(String[] args) {
		if (args==null) return this;
		source = "main args";
		sources.add(source);
		try {
			// Look for config
			int i = 0;
			for (; i < args.length; i++) {
				String a = args[i];
				// require and chop the leading -
				if ( ! a.startsWith("-")) {
					// end of options - return the rest as leftover
					break;
				}
				a = a.substring(1, a.length());
				// TODO refactor setOneKeyValue() so this can use the same get-field
				Field field = token2field.get(a);
				if (field == null) {
					Log.w(LOGTAG, config.getClass()+" Unrecognised option: "+a+" from main args "+Printer.toString(args));
					break; // goes into the remainder
//					// advance i anyway??
//					if (args.length > i+1 && ! args[i+1].startsWith("-")) i++;
//					continue;
				}
				// set field & advance i appropriately
				i = parse2_1arg(args, i, field);
			}
			// return remainder
			remainderArgs = Arrays.asList(Arrays.copyOfRange(args, i, args.length));
			return this;
		} catch (Exception e) {
			throw Utils.runtime(e);
		} finally {
			source = null;
		}
	}

	public static final String LOGTAG = "config";
	
	private int parse2_1arg(String[] args, int i, Field field) throws IllegalAccessException, ParseException 
	{
		if (field.getType() == Boolean.class
				|| field.getType() == boolean.class) {
			boolean v = true;
			// Did they specify the value (which is optional for booleans)?
			if (i + 1 < args.length) {
				String a2 = args[i + 1].toLowerCase();
				if (a2.equals("true") || a2.equals("false")) {
					v = a2.equals("true");
					i++; // advance to consume v
				}
			}
			fieldSet(field, v);			
		} else {
			// Take next argument as parameter
			i++;
			Object v = convert(field.getType(), args[i]);
			fieldSet(field, v);
		}
		return i;
	}

	Map<Field,Object> source4setFields = new ArrayMap();

	/**
	 * Debug info: how was a field set?
	 * @param fieldName
	 * @return source e.g. File, or null
	 */
	public Object getSourceForField(String fieldName) {
		Utils.check4null(fieldName);
		for(Field f : source4setFields.keySet()) {
			if (f.getName().equals(fieldName)) return source4setFields.get(f);
		}
		return null;
	}
	
	private boolean debug;

	private final List<Object> sources = new ArrayList();
	
	public List<Object> getSources() {
		return Collections.unmodifiableList(sources);
	}
	
	private void fieldSet(Field field, Object v) throws IllegalArgumentException, IllegalAccessException {
		Object prev = source4setFields.put(field, Utils.or(source, "unknown"));
		if (prev!=null) {
			// log the override
			Log.i(LOGTAG, "... "+config.getClass().getSimpleName()+"."+field.getName()+" source "+source+" overrode "+prev);
		}
		field.set(config, v);
	}

	/**
	 * Convenience for {@link #set(Properties)}
	 * 
	 * @param propertiesFile
	 *            Must exist
	 * @return The fields that got set
	 */
	public ConfigBuilder set(File propertiesFile) {
		if (propertiesFile==null) return this;		
		if ( ! propertiesFile.exists()) {
			Log.d(LOGTAG, config.getClass().getSimpleName()+": No properties file: "+propertiesFile
				+ (propertiesFile.isAbsolute()? "" : " = "+propertiesFile.getAbsolutePath())
			);
			return this;
		}		
		source = propertiesFile.getAbsoluteFile();
		sources.add(source);
		try {
			File absFile = propertiesFile.getAbsoluteFile();
			Properties props = new Properties();
			props.load(FileUtils.getReader(absFile));
			if (props.isEmpty()) {
				Log.d(LOGTAG, config.getClass().getSimpleName()+": No props in properties file: "+propertiesFile+" = "+propertiesFile.getAbsolutePath());
				return this;
			}
			return set(props);
		} catch (IOException e) {
			throw new WrappedException(e);
		} finally {
			source = null;
		}
	}

	/**
	 * @return the config object which we're filling in.
	 * This will check all the required fields have been set.
	 * This will call init() if the config implements {@link IInit}.
	 */
	public <S> S get() {
		List<Field> missing = getMissingProperties();
		if ( ! missing.isEmpty()) {
			throw new IllegalArgumentException("Missing required arguments: "+Containers.apply(missing, f -> f.getName()));
		}
		// init?
		if (config instanceof IInit) {
			((IInit) config).init();
		}
		// debug?
		if (debug) {
			try {
				for(Field f : source4setFields.keySet()) {
					Log.d(LOGTAG, config.getClass().getSimpleName()+"."+f.getName()
									+" was set from "+source4setFields.get(f)
									+(protectPasswords(f.getName())? "" : " to "+f.get(config))
									);
				}
			} catch(Exception ex) {
				Log.e(LOGTAG+".debug.fail", ex);
			}
		}
		return (S) config;
	}

	/**
	 * Set config fields from a Java properties object. Tokens are the same as
	 * in the command-line, except that any leading - or -- is stripped off.
	 * 
	 * @param properties
	 * @return the fields that were set.
	 * @throws WrappedException
	 *             if a property value cannot be converted. It will set as many
	 *             properties as it can before throwing any exception.
	 */
	public ConfigBuilder set(Map properties) {
		if (source==null) {
			source = "Map";
			sources.add(source);
		}
		List<Exception> errors = new ArrayList();
		// keys
		Collection<String> keys;
		if (properties instanceof Properties) {
			// this includes defaults if present
			Enumeration ekeys = ((Properties)properties).propertyNames();
			keys = Containers.asList(ekeys);
		} else {
			keys = properties.keySet();
		}
		// Look for config
		for (String a : keys) {
			String v = StrUtils.str(properties.get(a));
			if (v==null) continue;			
			setOneKeyValue(a, v, errors);
		} // ./loop
		
		// OK?
		if (errors.size() != 0) {
			throw Utils.runtime(errors.get(0));
		}
		return this;
	}

	/**
	 * Where most stuff actually gets set
	 * @param a The argument
	 * @param v value
	 * @param errors
	 * @return
	 */
	private boolean setOneKeyValue(String a, String v, List<Exception> errors) {
		// trim strings (strings loaded from .properties can have trailing whitespace)
		if (v != null) v = v.trim();
		// special case: config is a Properties object
		if (config instanceof Properties) {
			((Properties)config).setProperty(a, v);
			return true;
		}
		// normal case?
		Field field = token2field.get(a);
		if (field != null) {
			set2(field, v, errors);
			return true;
		}
		// a map or recursive field?
		if ( ! a.contains(".")) return false;
		String[] bits = a.split("\\.");
		if (bits.length==1) return false;
		field = token2field.get(bits[0]);
		if (field==null) return false;
		// recursive?
		ConfigBuilder ap2 = field2subparser.get(field);
		if (ap2!=null) {
			ap2.setOneKeyValue(bits[1], v, errors);
			return true;
		}
		// map?
		if (ReflectionUtils.isa(field.getType(), Map.class)) {
			if (bits.length > 2) {
				errors.add(new IllegalArgumentException("Cannot set nested map key: "+a));
				return false;
			}
			try {
				Map map = (Map) field.get(config);
				if (map==null) {
					map = (Map) (field.getType().isInterface()? new ArrayMap() : field.getType().newInstance());
					fieldSet(field, map);
				}
				map.put(bits[1], v);
				return true;
			} catch (Exception e) {
				throw Utils.runtime(e);
			}						
		}			
		return false;
	}

	Object source;
	
	private boolean set2(Field f, String prop, List<Exception> errors) {
		assert f != null : prop;
		try {
			Object v = convert(f.getType(), prop);
			fieldSet(f, v);
			return true;
		} catch (Exception e) {
			errors.add(e);
			return false;
		}
	}

	/**
	 * setup the {@link #token2field} map
	 * 
	 * @param config
	 * @throws IllegalArgumentException
	 */
	 Map<String,Field> parseConfigObject(Class configClass) throws IllegalArgumentException {
		 assert configClass != Class.class; // WTF?
		 parseFlag = true;
		 requiredArgs = new ArrayList();
		 final HashMap classToken2field = new HashMap();
		 // Get annotated fields
		 List<Field> fields = ReflectionUtils.getAnnotatedFields(configClass,
				Option.class, true);
		 // Get tokens
		 for (Field field : fields) {
			// Is this OK?
			boolean ok = checkField(field.getType());
			if ( ! ok) {
				// Support recursive config
				ConfigBuilder ap2 = null;
				try {
					Object v = field.get(config);
					if (v!=null) ap2 = new ConfigBuilder(v);					
				} catch (IllegalAccessException e) {
				}				
				if (ap2==null) ap2 = new ConfigBuilder(field.getType());
				if (ap2.token2field.isEmpty()) {
					throw new IllegalArgumentException("Unrecognised type: " + field.getType()+" "+field.getName());
				}
				field2subparser.put(field, ap2);
			}
			// Get tokens
			Option arg = field.getAnnotation(Option.class);
			String[] ts = tokens(field, arg).split(",\\w*");
			for (String t : ts) {
				if ( ! t.startsWith("-"))
					throw new IllegalArgumentException(
							"Invalid token (all tokens must begin with a -): "
									+ t);
				// chop the -
				t = t.substring(1).trim();
				assert ! t.isEmpty();
				classToken2field.put(t, field);
			}
			// Required?
			if (arg.required()) {
				requiredArgs.add(field);
			}
			// Default
			if (config!=null && field.getType() != Boolean.class
					&& field.getType() != boolean.class) {
				try {
					if ( ! field.isAccessible()) {
						field.setAccessible(true);
					}
					Object d = field.get(config);
					field2default.put(field, d);
				} catch (IllegalAccessException e) {
					throw new IllegalArgumentException(e);
				}
			}
			// ok
		}
		token2field.putAll(classToken2field);
		return classToken2field;
	}

	 
	private String tokens(Field f, Option arg) {
		String tokens = arg.tokens();
		if (tokens == null || tokens.length() == 0) {
			tokens = "-" + f.getName();
		}
		return tokens;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '\n' + getOptionsMessage(null);
	}

	
	/**
	 * Look in the system properties (i.e. -D arguments passed into the JVM)
	 * @param namespace Can be null or blank. 
	 * If not blank, then this will look for namespace.property  
	 */
	public ConfigBuilder setFromSystemProperties(String namespace) {
		if (namespace!=null && namespace.isEmpty()) namespace = null;
		source = "System.properties"+(Utils.isBlank(namespace)? "" : " namespace:"+namespace);
		sources.add(source);
		try {
			Properties systemProps = System.getProperties();
			if (Utils.isBlank(namespace)) {
				return set(systemProps);
			}
			// Copy with namespace filter and removal
			assert ! namespace.endsWith(".");
			namespace += ".";
			HashMap map = new HashMap(systemProps.size());
			for(Map.Entry me : systemProps.entrySet()) {
				String key = me.getKey().toString();
				if ( ! key.startsWith(namespace)) {
					continue;
				}
				key = key.substring(namespace.length());
				if (key.isEmpty()) continue;
				map.put(key, me.getValue());
			}
			return set(map);
		} finally {
			source = null;
		}
	}

	/**
	 * If true, get() will cause the source of each setting to be output to logs 
	 * @param b
	 * @return 
	 */
	public ConfigBuilder setDebug(boolean b) {
		debug = b;
		return this;
	}

	public List<File> getFileSources() {
		return Containers.filterByClass(getSources(), File.class);
	}

	public static long bytesFromString(String fileSize) {
		fileSize = fileSize.toLowerCase().trim();
		if (fileSize.endsWith("b")) fileSize = fileSize.substring(0, fileSize.length() -1);
		long mult = 1;
		String n = fileSize;
		if (fileSize.endsWith("g")) {
			mult = 1000000000;
			n = fileSize.substring(0, fileSize.length()-1);
		}
		if (fileSize.endsWith("m")) {
			// see https://en.wikipedia.org/wiki/Megabyte
			mult = 1000000;
			n = fileSize.substring(0, fileSize.length()-1);
		}
		if (fileSize.endsWith("k")) {
			mult = 1024;
			n = fileSize.substring(0, fileSize.length()-1);
		}
		return (long) (MathUtils.toNum(n)*mult);
//		??MathUtils.getNumber(_num)
	}
	

}
