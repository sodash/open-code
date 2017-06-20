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
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.winterwell.utils.Environment;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * Parse Unix style command line arguments. Also handles Java properties objects
 * (as created from .properties files).
 * 
 * Sets fields annotated with {@link Option} in a settings object.
 * 
 * <h3>Best Practice Example</h2>
 * <pre><code>
 * public static void main(String[] args) {
 * 	MyConfig config = new MyConfig();
 * 	ArgsParser ap = new ArgsParser(config);
 * 	// Load a properties file
 * 	File myPropertiesFile;
 * 	ap.set(myPropertiesFile);
 * 	// Then load any system properties
 * 	ap.setFromSystemProperties("MyApp");
 * 	// Then load from command line args
 * 	ap.setFromMain(args);
 *	// Now do your thing...
 * }
 * </code></pre>
 * @author daniel
 */
public class ArgsParser {

	private static final Map<Class,ISerialize> convertors = new HashMap();
	
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
	 * Fill in config from:
	 * 
	 * 1. Command line args
	 * 2. Properties file
	 * 3. {@link Environment#getProperties()}
	 * 4. System properties
	 * 
	 * @param settings
	 * @param args Can be null
	 * @param propertiesFile Can be null. Can not exist (ignored with a log message)
	 * @param leftoverArgs Can be null. If not null, args which are not picked out as options will be added
	 * to this.
	 * @return settings (same object as input)
	 */
	public static <S> S getConfig(S settings, String[] args, File propertiesFile, List<String> leftoverArgs) {
		assert settings != null;
		ArgsParser ap = new ArgsParser(settings);
		// Order: the last setting wins, so start with the "least important" source
		// system props first
		ap.setFromSystemProperties(null);
		// shared properties
		Properties eprops = Environment.getProperties();
		if (eprops!=null) {
			ap.set(eprops);
		}
		// properties file
		if (propertiesFile!=null) {
			if (propertiesFile.exists()) {
				ap.set(propertiesFile);
			} else {
				Log.d("config", settings.getClass()+": No properties file: "+propertiesFile+" = "+propertiesFile.getAbsolutePath());
			}
		}		
		// Command line arguments (can override properties file)
		if (args!=null) {
			String[] _nonOptions = ap.setFromMain(args);
			if (leftoverArgs!=null) {
				for (String s : _nonOptions) {
					leftoverArgs.add(s);
				}
			}
		}		
		// done
		ap.checkRequiredSettings();
		return settings;
	}
	
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
			// Date
			if (type == Date.class)
				return DateFormat.getInstance().parse(string);
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
			throw new IllegalArgumentException("Unrecognised type: " + type);
		} catch (Exception e) {
			throw new ParseException(e.getMessage(), 1);
		}
	}

	Map<Field, Object> field2default = new HashMap<Field, Object>();

	private List<Field> missingArgs;

	/**
	 * Reusable
	 */
	List<Field> requiredArgs = new ArrayList<Field>();

	/**
	 * Temporary copy used during a parse
	 */
	List<Field> requiredArgs2;

	private final Object settings;

	/**
	 * The tokens do NOT include the leading "-"
	 */
	final Map<String, Field> token2field = new HashMap<String, Field>();

	private Map<Field, ArgsParser> field2subparser = new HashMap();

	/**
	 * Create an ArgsParser which will set {@link Option} fields in the given
	 * settings object.
	 * 
	 * @param settings
	 */
	public ArgsParser(Object settings) {
		assert settings != null;
		this.settings = settings;
		// Setup token->field map
		parseSettingsObject(settings.getClass());
	}
	
	/** @deprecated better to provide an object*/
	ArgsParser(Class settingsClass) {
		Object obj = null; 
		try {
			obj = settingsClass.newInstance();			
		} catch (Exception ex) {
		}
		this.settings = obj;
		// Setup token->field map
		parseSettingsObject(settingsClass);
	}

	private boolean checkField(Class<?> type) {
		if (convertors.containsKey(type)) return true;
		for(Class k : recognisedTypes) {
			if (ReflectionUtils.isa(type, k)) return true;
		}
		// TODO is it a recursive thing?
		return false;
	}

	private void checkRequiredSettings() {
		if (requiredArgs2==null || requiredArgs2.size() == 0)
			return;
		missingArgs = requiredArgs2;
		throw new IllegalArgumentException("Missing required arguments: "+Containers.apply(missingArgs, f -> f.getName()));
	}

	public List<Field> getMissingArguments() {
		if (missingArgs == null)
			throw new IllegalStateException("parse has not run and failed");
		return missingArgs;
	}

	/**
	 * @return a message describing the available options.
	 */
	public String getOptionsMessage() {
		StringBuilder msg = new StringBuilder();
		msg.append("Options:" + StrUtils.LINEEND);
		HashSet<Field> options = new HashSet<Field>(token2field.values());
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
	 * @param args
	 *            The arguments as passed into main()
	 * @return the non-options arguments, i.e. the ones after the options
	 * @throws RuntimeException
	 *             with a usage message. The missing arguments (if this is the
	 *             problem) can then be found via {@link #getMissingArguments()}
	 */
	public String[] setFromMain(String[] args) {
		requiredArgs2 = new ArrayList<Field>(requiredArgs);
		try {
			// Look for settings
			int i = 0;
			for (; i < args.length; i++) {
				String a = args[i];
				// require and chop the leading -
				if ( ! a.startsWith("-")) {
					continue;
				}
				a = a.substring(1, a.length());
				Field field = token2field.get(a);
				if (field == null) {
					break;
				}
				// set field & advance i appropriately
				i = parse2_1arg(args, i, field);
			}
			// OK?
			checkRequiredSettings();
			// return remainder
			return Arrays.copyOfRange(args, i, args.length);
		} catch (Exception e) {
			if (missingArgs == null) {
				missingArgs = new ArrayList<Field>(0);
			}
			throw Utils.runtime(e);
		}
	}

	private int parse2_1arg(String[] args, int i, Field field)
			throws IllegalAccessException, ParseException {
		requiredArgs2.remove(field);
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
			field.set(settings, v);
		} else {
			// Take next argument as parameter
			i++;
			Object v = convert(field.getType(), args[i]);
			field.set(settings, v);
		}
		return i;
	}

	/**
	 * Convenience for {@link #set(Properties)}
	 * 
	 * @param propertiesFile
	 *            Must exist
	 * @return The fields that got set
	 */
	public List<Field> set(File propertiesFile) {
		try {
			File absFile = propertiesFile.getAbsoluteFile();
			Properties props = new Properties();
			props.load(FileUtils.getReader(absFile));
			return set(props);
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	/**
	 * @return the settings object which we're filling in.
	 */
	public Object getSettings() {
		return settings;
	}

	/**
	 * Set settings fields from a Java properties object. Tokens are the same as
	 * in the command-line, except that any leading - or -- is stripped off.
	 * 
	 * @param properties
	 * @return the fields that were set.
	 * @throws WrappedException
	 *             if a property value cannot be converted. It will set as many
	 *             properties as it can before throwing any exception.
	 */
	public List<Field> set(Map properties) {
		ArrayList<Field> set = new ArrayList();
		List<Exception> errors = new ArrayList();
		// keys
		Collection<String> keys;
		if (properties instanceof Properties) {
			// this includes defaults if present
			Enumeration ekeys = ((Properties)properties).propertyNames();
			keys = Containers.getList(ekeys);
		} else {
			keys = properties.keySet();
		}
		// Look for settings
		for (String a : keys) {
			String v = StrUtils.str(properties.get(a));
			if (v==null) continue;			
			setOneKeyValue(a, v, set, errors);
		} // ./loop
		
		// OK?
		if (errors.size() != 0) {
			throw Utils.runtime(errors.get(0));
		}
		return set;
	}

	private boolean setOneKeyValue(String a, String v, List<Field> set, List<Exception> errors) {
		// special case: config is a Properties object
		if (settings instanceof Properties) {
			((Properties)settings).setProperty(a, v);
			return true;
		}
		// normal case?
		Field field = token2field.get(a);
		if (field != null) {
			set2(field, v, set, errors);
			return true;
		}
		// a map or recursive field?
		if ( ! a.contains(".")) return false;
		String[] bits = a.split("\\.");
		if (bits.length==1) return false;
		field = token2field.get(bits[0]);
		if (field==null) return false;
		// recursive?
		ArgsParser ap2 = field2subparser.get(field);
		if (ap2!=null) {
			ap2.setOneKeyValue(bits[1], v, set, errors);
			return true;
		}
		// map?
		if (ReflectionUtils.isa(field.getType(), Map.class)) {
			if (bits.length > 2) {
				errors.add(new IllegalArgumentException("Cannot set nested map key: "+a));
				return false;
			}
			try {
				Map map = (Map) field.get(settings);
				if (map==null) {
					map = (Map) (field.getType().isInterface()? new ArrayMap() : field.getType().newInstance());
					field.set(settings, map);
				}
				map.put(bits[1], v);
				return true;
			} catch (Exception e) {
				throw Utils.runtime(e);
			}						
		}			
		return false;
	}

	private boolean set2(Field f, String prop, List<Field> set, List<Exception> errors) {
		assert f != null : prop;
		try {
			Object v = convert(f.getType(), prop);
			f.set(settings, v);
			set.add(f);
			return true;
		} catch (Exception e) {
			errors.add(e);
			return false;
		}
	}

	/**
	 * setup the {@link #token2field} map
	 * 
	 * @param settings
	 * @throws IllegalArgumentException
	 */
	 Map<String,Field> parseSettingsObject(Class settingsClass) throws IllegalArgumentException {
		 final HashMap classToken2field = new HashMap();
		 // Get annotated fields
		 List<Field> fields = ReflectionUtils.getAnnotatedFields(settingsClass,
				Option.class, true);
		 // Get tokens
		 for (Field field : fields) {
			// Is this OK?
			boolean ok = checkField(field.getType());
			if ( ! ok) {
				// Support recursive settings
				ArgsParser ap2 = null;
				try {
					Object v = field.get(settings);
					if (v!=null) ap2 = new ArgsParser(v);					
				} catch (IllegalAccessException e) {
				}				
				if (ap2==null) ap2 = new ArgsParser(field.getType());
				if (ap2.token2field.isEmpty()) {
					throw new IllegalArgumentException("Unrecognised type: " + field.getType()+" "+field.getName());
				}
				field2subparser.put(field, ap2);
			}
			// Get tokens
			Option arg = field.getAnnotation(Option.class);
			for (String t : tokens(field, arg).split(",")) {
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
			if (settings!=null && field.getType() != Boolean.class
					&& field.getType() != boolean.class) {
				try {
					if ( ! field.isAccessible()) {
						field.setAccessible(true);
					}
					Object d = field.get(settings);
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
		return getClass().getSimpleName() + '\n' + getOptionsMessage();
	}

	/**
	 * Look in the system properties (i.e. -D arguments passed into the JVM)
	 * @param namespace Can be null or blank. 
	 * If not blank, then this will look for namespace.property  
	 */
	public List<Field> setFromSystemProperties(String namespace) {
		if (namespace!=null && namespace.isEmpty()) namespace = null;
		Properties systemProps = System.getProperties();
		if (namespace==null || namespace.isEmpty()) {
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
	}

	/**
	 * Convenience for {@link #getConfig(Object, String[], File, List)} with just a file.
	 * Suggested usage:
	 * <code>
	 * MyConfig myconfig = ArgsParser.getConfig(new MyConfig(), new File("myconfig.properties")); // simples :)
	 *  </code>
	 * 
	 * @param config
	 * @param propertiesFile Can be null. Doesn't have to exist. If this file exists, load properties from it.
	 * In SoDash, this is normally "config/MySubsystem.properties"
	 * @return config (same object), with properties set
	 */
	public static <S> S getConfig(S config, File propertiesFile) {
		return getConfig(config, null, propertiesFile, null);
	}
	

}
