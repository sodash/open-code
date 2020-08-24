package com.winterwell.utils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.time.Time;

/**
 * Reflection-related utility functions.
 * 
 * See also Bob's Classpath
 * 
 * @testedby {@link ReflectionUtilsTest}
 */
public class ReflectionUtils {
		
	
	/**
	 * 
	 * @param klass
	 * @param f
	 * @return set-f method or null
	 */
	public static Method getSetter(Class klass, Field f) {
		Method m = getMethod(klass, "set"+StrUtils.toTitleCase(f.getName()));
		return m;
	}
	
	/**
	 * A slightly shallow clone WILL copy Collections, Maps and Arrays (to allow
	 * for modification), but will _not_ recurse.
	 * <p>
	 * Compare with Object.clone() which does a shallow copy of all fields. Or
	 * Utils.copy(Object) which does a recursive deep copy.
	 * 
	 * @param original
	 * @param blankClone
	 * @return blankClone, now filled in -- setting all non-transient fields to
	 *         match original.
	 */
	public static <X> X cloneSlightlyShallow(X original, X blankClone) {
		try {
			List<Field> fields = getAllFields(original.getClass());
			for (Field field : fields) {
				if (isTransient(field)) {
					// null it out -- the default clone() method may have set it
					field.set(blankClone, null);
					continue;
				}

				Object v = field.get(original);
				Object v2 = v;

				// copy it?
				if (v instanceof Map) {
					if (v instanceof Cloneable) {
						v2 = invoke(v, "clone");
					} else {
						Map copy = (Map) v.getClass().newInstance();
						copy.putAll((Map) v);
					}
				} else if (v instanceof Collection) {
					if (v instanceof Cloneable) {
						v2 = invoke(v, "clone");
					} else {
						Collection copy = (Collection) v.getClass()
								.newInstance();
						copy.addAll((Collection) v);
					}
				} else if (v.getClass().isArray()) {
					// bugger -- array copy is a pain!
					v2 = copyArray(v);
				}

				field.set(blankClone, v2);
			}
			return blankClone;
		} catch (Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	public static boolean isTransient(Field field) {
		int mods = field.getModifiers();
		return Modifier.isTransient(mods);
	}

	/**
	 * Ugly hack to handle different primitives. TODO are there better ways? Use
	 * v.getClass().getComponentType()? Array.newInstance()?
	 * 
	 * @param v
	 * @return shallow copy
	 */
	static Object copyArray(Object v) {
		int len = Array.getLength(v);
		if (v instanceof boolean[]) {
			return Arrays.copyOf((boolean[]) v, len);

		} else if (v instanceof byte[]) {
			return Arrays.copyOf((byte[]) v, len);

		} else if (v instanceof char[]) {
			return Arrays.copyOf((char[]) v, len);

		}
		if (v instanceof double[]) {
			return Arrays.copyOf((double[]) v, len);

		}
		if (v instanceof float[]) {
			return Arrays.copyOf((float[]) v, len);

		}
		if (v instanceof int[]) {
			return Arrays.copyOf((int[]) v, len);

		}
		if (v instanceof long[]) {
			return Arrays.copyOf((long[]) v, len);

		}
		if (v instanceof short[]) {
			return Arrays.copyOf((short[]) v, len);

		}
		return copyArray2(v, len);
	}

	static <T> T[] copyArray2(Object v, int len) {
		return Arrays.copyOf((T[]) v, len);
	}

	/**
	 * @deprecated It's a bad idea to rely on this.
	 * 
	 * MAY BE VERY SLOW - may attempt to load every damn class!
	 * 
	 * @param implementMe
	 * @param packageIndicator
	 *            The search will start from here. It recursively explores all
	 *            sub-packages.
	 * @return classes implementing implementMe
	 * @testedby {@link ReflectionUtilsTest#testFindClasses()}
	 */
	public static List<Class> findClasses(Class implementMe,
			Class packageIndicator) {
		Package p = packageIndicator.getPackage();
		URL r = packageIndicator.getResource("");
		String dir = r.getFile();
		return findClasses(implementMe, new File(dir), p.getName());
	}

	public static List<Class> findClasses(Class implementMe, File dir,
			String packageName) {
		List<Class> found = new ArrayList<Class>();
		List<File> classFiles = FileUtils.find(dir, ".*\\.class");
		// // sort? -- age (the useful sort order) of class file is irrelevant
		// if (sortFiles!=null) {
		// Collections.sort(classFiles, sortFiles);
		// }
		for (File file : classFiles) {
			// try to load class
			String cName = FileUtils.getBasename(file);
			if (cName.contains("$")) {
				continue;
			}
			cName = packageName + "." + cName;
			try {
				Class c = Class.forName(cName);
				// ignore interfaces
				if (c.isInterface()) {
					continue;
				}
				if (isa(c, implementMe)) {
					found.add(c);
				}
			} catch (Exception e) {
				// oh well
			}
		}
		return found;
	}

	/**
	 * @param clazz
	 * @return Instance fields - public and private - which can be accessed from
	 *         this class. Excludes: static fields and fields which cannot be
	 *         accessed due to hardline JVM security.<br>
	 *         Includes: non-static final fields<br>
	 *         Field objects will have setAccessible(true) called on them as
	 *         needed to try & make private fields accessible.
	 */
	public static List<Field> getAllFields(Class clazz) {
		ArrayList<Field> list = new ArrayList<Field>();
		ReflectionUtils.getAllFields2(clazz, list);
		return list;
	}

	private static void getAllFields2(Class clazz, ArrayList<Field> list) {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			// exclude static
			int m = field.getModifiers();
			if (Modifier.isStatic(m)) {
				continue;
			}
			if (!field.isAccessible()) {
				try {
					field.setAccessible(true);
				} catch (SecurityException e) {
					// skip over this field
					continue;
				}
			}
			list.add(field);
		}
		// recurse
		Class superClass = clazz.getSuperclass();
		if (superClass == null)
			return;
		getAllFields2(superClass, list);
	}

	/**
	 * @param object
	 * @param annotation
	 * @param incPrivate
	 *            If true, will return private and protected fields (provided
	 *            they can be set accessible).
	 * @return (All fields / accessible public fields) in object which are
	 *         annotated with annotation
	 */
	public static List<Field> getAnnotatedFields(Class klass,
			Class<? extends Annotation> annotation, boolean incPrivate) {
		List<Field> allFields = incPrivate ? getAllFields(klass)
				: Arrays.asList(klass.getFields());
		List<Field> fields = new ArrayList<Field>();
		for (Field f : allFields) {
			if (f.isAnnotationPresent(annotation)) {
				fields.add(f);
			}
		}
		return fields;
	}

	/**
	 * Recurse to get a private field which may be declared in a super-class.
	 * Note: {@link Class#getField(String)} will only retrieve public fields.
	 * 
	 * @param klass
	 * @param fieldName
	 * @return Field or null
	 */
	public static Field getField(Class klass, String fieldName) {
		Utils.check4null(klass, fieldName);
		try {
			Field f = klass.getDeclaredField(fieldName);
			return f;
		} catch (NoSuchFieldException e) {
			klass = klass.getSuperclass();
			if (klass == null)
				return null;
		}
		return getField(klass, fieldName);
	}


	/**
	 * Where is this class from?
	 * 
	 * @param klass
	 * @return
	 */
	public static File getFile(Class klass) {
		try {
			URL rs = klass.getResource(klass.getSimpleName() + ".class");
			return new File(rs.toURI());
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * 
	 * @param clazz
	 * @param methodName
	 * @return The first method with matching name (ignores the parameters), or null if it isn't there. 
	 */
	static Method getMethod(Class<?> clazz, String methodName) {
//		clazz.getMethod(name, parameterTypes)
		// ??why not use class.getMethod or getDeclaredMethod?? they prob have hierarchy privacy issues
		// ignore the parameter types - but iterate over all methods :(
		for (Method m : clazz.getMethods()) {
			if (m.getName().equals(methodName))
				return m;
		}
		return null;
	}

	public static <X> X getPrivateField(Object obj, String fieldName) {
		Field f = ReflectionUtils.getField(obj.getClass(), fieldName);
		f.setAccessible(true);
		try {
			return (X) f.get(obj);
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * 
	 * @param obj
	 * @param property
	 *            i.e. "name" maps to the method getName()
	 * @return null if the property is not present (so this is ambiguous)
	 */
	public static Object getProperty(Object obj, String property) {
		try {
			String mName = "get" + StrUtils.toTitleCase(property);
			Method m = obj.getClass().getMethod(mName);
			if (!m.isAccessible()) {
				m.setAccessible(true);
			}
			try {
				return m.invoke(obj);
			} catch (Exception e) {
				throw Utils.runtime(e);
			}
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	public static boolean hasField(Class klass, String field) {
		return getField(klass, field) != null;
	}

	/**
	 * @param klass
	 * @param methodName
	 * @return true if klass has a public method of that name
	 */
	public static boolean hasMethod(Class klass, String methodName) {
		for (Method m : klass.getMethods()) {
			if (m.getName().equals(methodName))
				return true;
		}
		return false;
	}

	public static <X> X invoke(Object obj, String methodName, Object... args) {
		Method m = getMethod(obj.getClass(), methodName);
		if (m == null)
			throw Utils.runtime(new NoSuchMethodException(obj.getClass() + "."
					+ methodName));
		try {
			m.setAccessible(true);
			return (X) m.invoke(obj, args);
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * The equivalent of instanceof, but for Class objects. 'cos I always forget
	 * how to do this.
	 * 
	 * @param possSubType Can be null (returns false)
	 * @param superType
	 * @return true if possSubType <i>is</i> a subType of superType
	 */
	public static boolean isa(Class possSubType, Class superType) {
		if (possSubType==null) return false;
		return superType.isAssignableFrom(possSubType);
	}

	/**
	 * Output manifest info on jar files.
	 * E.g.
	 * java -cp winterwell-utils.jar com.winterwell.utils.ReflectionUtils MyJarFile.jar
	 * 
	 * @param args
	 *            jar filenames
	 */
	public static void main(String[] args) {
		assert args.length != 0;
		for (String string : args) {
			System.out.println("Info on jar " + string);
			try {
				JarFile jar = new JarFile(new File(string));
				Manifest manifest = jar.getManifest();
				Map<String, Attributes> entries = manifest.getEntries();
				Attributes v = manifest.getMainAttributes();
				for (Object k : v.keySet()) {
					System.out.println(k + ": " + v.get(k));
				}
				for (String a : entries.keySet()) {
					v = entries.get(a);
					for (Object k : v.keySet()) {
						System.out.println(a + k + ": " + v.get(k));
					}
				}
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	public static void setPrivateStaticField(Object obj, String fieldName, Object value) {
		Field f = ReflectionUtils.getField(obj.getClass(), fieldName);
		if (f==null) {
			throw Utils.runtime(new NoSuchFieldException(fieldName));
		}
		checkCanSetField(f);
		try {
			f.setAccessible(true);
			int m = f.getModifiers();
			// coerce to non-final?
			if ( ! Modifier.isStatic(m)) {
				throw new IllegalArgumentException("Not static "+f);
			}
		    Field modifiersField = Field.class.getDeclaredField("modifiers");
		    modifiersField.setAccessible(true);
		    modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
			f.set(obj, value);
		} catch(Exception iae) {
			throw Utils.runtime(iae);		     
		} 
	}

	private static void checkCanSetField(Field f) {
		if (f.getType() == Boolean.class || isaNumber(f.getType())) {
			//  why not??
			throw new IllegalArgumentException("Too primitive - Dont set "+f);
		}
	}

	/**
	 * Set a field, which can be private. Throws exceptions if the field does
	 * not exist or if you cannot do this.
	 * 
	 * @param obj
	 * @param fieldName
	 * @param value
	 */
	public static void setPrivateField(Object obj, String fieldName,
			Object value) {
		Field f = ReflectionUtils.getField(obj.getClass(), fieldName);
		if (f==null) {
			throw Utils.runtime(new NoSuchFieldException(fieldName));
		}
//		checkCanSetField(f);
		f.setAccessible(true);
		try {
			// Do it!
			setPrivateField2(obj, f, value);
			return;
			
		} catch(IllegalArgumentException ex) {
			// coerce type?
			try {
				Class<?> ft = f.getType();
				if (ReflectionUtils.isaNumber(ft)) {
					// coerce
					double dbl = MathUtils.toNum(value);
					if (ft == Integer.class || ft==int.class) {
						f.set(obj, (int) dbl);
						return;
					}
					if (ft == Long.class || ft==long.class) {
						f.set(obj, (long) dbl);
						return;
					} 
					if (ft == Float.class || ft==float.class) {
						f.set(obj, (float) dbl);
						return;
					}
					if (ft == BigDecimal.class) {
						f.set(obj, new BigDecimal(dbl));
						return;
					}
					if (ft == BigInteger.class) {
						f.set(obj, new BigInteger(value.toString()));
						return;
					}
				}
				if (ft == Time.class) {
					f.set(obj, new Time(value.toString()));
					return;
				}
				if (ft.isEnum()) {
					Object[] cs = ft.getEnumConstants();
					for (Object c : cs) {
						if (c.toString().equals(value)) {
							f.set(obj, c);
							return;
						}
					}
				}
				if (ft == String.class) {
					f.set(obj, value.toString());
					return;
				}
			} catch(Exception ex2) {
				// fail :(
				ex2.printStackTrace();
			}
			throw Utils.runtime(ex);
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	private static void setPrivateField2(Object obj, Field f, Object value) throws IllegalArgumentException, IllegalAccessException {
		// special cases
		Class<?> typ = f.getType();
		if ( ! typ.isPrimitive()) {
			f.set(obj, value);
			return;
		}		
		if (isaNumber(typ)) {
			double v = MathUtils.toNum(value);
			if (typ==double.class) {				
				f.setDouble(obj, v);
			} else if (typ==long.class) {
				Long lv = value instanceof Long? (Long) value : (long) v;
				f.setLong(obj, lv);
			} else if (typ==float.class) {				
				f.setFloat(obj, (float) v);			
			} else if (typ==int.class) {				
				f.setInt(obj, (int) v);			
			} else if (typ==short.class) {				
				f.setShort(obj, (short) v);			
			} else {
				throw new IllegalArgumentException("TODO support "+typ+" field:"+f);
			}
			return;
		}
		if (typ==boolean.class) {
			boolean v = Utils.yes(value);
			f.setBoolean(obj, v);
			return;
		}
		// try anyway
		f.set(obj, value);
	}

	/**
	 * @return total memory used, in bytes. Runs garbage collection to try and
	 *         give a better measure of what's currently in play. Warning: this
	 *         is a bit of a slow call.
	 */
	public static long getUsedMemory() {
		Runtime rt = Runtime.getRuntime();
		rt.runFinalization();
		rt.gc();
		// The methods above shouldn't need it, but add in a pause anyway
		Utils.sleep(10);
		return rt.totalMemory() - rt.freeMemory();
	}

	/**
	 * TODO TEST!! This has changed
	 * 
	 * @return total available memory, in bytes. Does not run GC, so this is a fast
	 *         call.
	 * @see Runtime#freeMemory() -- which ignores the heap's capacity to grow, so is less
	 * useful! Runtime#freeMemory() can be thought of as "fast memory".
	 */
	public static long getAvailableMemory() {
		Runtime rt = Runtime.getRuntime();
		long maxMem = rt.maxMemory();
		long freeMem = rt.freeMemory();
		long totalMem = rt.totalMemory();
		long used = totalMem - freeMem;
		long available = maxMem - used;
		return available;
	}

	/**
	 * @return a String showing the current stack
	 */
	public static String stacktrace() {
		try {
			throw new Exception();
		} catch (Exception e) {
			StackTraceElement[] trace = e.getStackTrace();
			StringBuilder sb = new StringBuilder();
			for (int i = 1; i < trace.length; i++) {
				StackTraceElement stackTraceElement = trace[i];
				sb.append(stackTraceElement.toString());
				sb.append('\n');
			}
			return sb.toString();
		}
	}

	/**
	 * Who called this method?
	 * 
	 * NB: This always skips the actual calling method, stacktrace #-0
	 * 
	 * @param ignore
	 *            list of fully-qualified-class or method names to ignore (will
	 *            then search higher up the stack)
	 * @return Can be a dummy entry if the filters exclude everything. Never
	 *         null.
	 * @see #getSomeStack(int, String...)
	 */
	public static StackTraceElement getCaller(String... ignore) {
		return getCaller(1, ignore);
	}


	/**
	 * Who called this method?
	 * 
	 * @param ignore
	 *            list of fully-qualified-class or method names to ignore (will
	 *            then search higher up the stack)
	 * @param up 0 = get the method directly above the one calling this.           
	 * @return Can be a dummy entry if the filters exclude everything. Never
	 *         null.
	 * @see #getSomeStack(int, String...)
	 */
	public static StackTraceElement getCaller(int up, String... ignore) {
		List<String> ignoreNames = Arrays.asList(ignore);
		try {
			throw new Exception();
		} catch (Exception e) {
			StackTraceElement[] trace = e.getStackTrace();
			for (int i = 2+up; i < trace.length; i++) {
				String clazz = trace[i].getClassName();
				String method = trace[i].getMethodName();
				if (ignoreNames.contains(clazz) || ignoreNames.contains(method)) {
					continue;
				}
				return trace[i]; // new Pair<String>(clazz, method);
			}
			return new StackTraceElement("filtered", "?", null, -1);
		}
	}

	/**
	 * Who called this method? Returns the lowest parts of the stack.
	 * 
	 * @param depth
	 *            How many elements to aim for. Can be set very high for all-of-them.
	 * @param ignore
	 *            list of fully-qualified-class or method names to ignore (will
	 *            then search higher up the stack)
	 * @return Can be empty if the filters exclude everything. Never null.
	 * @see #getCaller(String...)
	 */
	public static List<StackTraceElement> getSomeStack(int depth,
			String... ignore) {
		assert depth > 0 : depth;
		List<String> ignoreNames = Arrays.asList(ignore);
		try {
			throw new Exception();
		} catch (Exception e) {
			StackTraceElement[] trace = e.getStackTrace();
			List<StackTraceElement> stack = new ArrayList(depth);
			for (int i = 2; i < trace.length; i++) {
				String clazz = trace[i].getClassName();
				String method = trace[i].getMethodName();
				if (ignoreNames.contains(clazz) || ignoreNames.contains(method)) {
					continue;
				}
				stack.add(trace[i]);
				if (stack.size() == depth)
					break;
			}
			return stack;
		}
	}

	/**
	 * Like class.getSimpleName() -- but if given an anonymous class, it will
	 * return the super-classes' name (rather than null)
	 * 
	 * @param class1
	 * @return name Never null or empty
	 */
	public static String getSimpleName(Class class1) {
		String name = class1.getSimpleName();
		if (!name.isEmpty()) {
			return name;
		}
		return getSimpleName(class1.getSuperclass());
	}

	public static boolean isaNumber(Class<?> type) {
		return isa(type, Number.class) || type == int.class
				|| type == double.class || type == long.class
				|| type == float.class;
	}

	public static Map<Field, Pair> diff(Object a, Object b, int depthTODO) {
		try {
			List<Field> afs = getAllFields(a.getClass());
			List<Field> bfs = getAllFields(b.getClass());
			Map<Field, Pair> diff = new HashMap();
			for (Field f : afs) {
				f.setAccessible(true);
				Object af = f.get(a);
				if (!bfs.contains(f)) {
					diff.put(f, new Pair(af, null));
					continue;
				}
				Object bf = f.get(b);
				if (Utils.equals(af, bf))
					continue;
				diff.put(f, new Pair(af, bf));
			}
			bfs.removeAll(afs);
			for (Field f : bfs) {
				f.setAccessible(true);
				Object bf = f.get(b);
				diff.put(f, new Pair(null, bf));
			}
			return diff;
		} catch (Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	/**
	 * @return e.g. 1.6
	 */
	public static double getJavaVersion() {
		String version = System.getProperty("java.version");
		if (version == null) {
			// WTF?!
			return 1.5;
		}
		int pos = 0, count = 0;
		for (; pos < version.length() && count < 2; pos++) {
			if (version.charAt(pos) == '.')
				count++;
		}
		pos--;
		return Double.parseDouble(version.substring(0, pos));
	}

	/**
	 * Rethrow serious exceptions -- like out-of-memory, or thread-death.
	 * Use-case: You want to catch assertion errors, but not thread-death.
	 * @param ex
	 * @throws ex if ex is an Error, apart from an AssertionError
	 */
	public static void rethrowBad(Throwable ex) {
		if (ex instanceof WrappedException) {
			ex = ex.getCause();
		}
		if (ex instanceof Error) {
			if (ex instanceof AssertionError) return;		
			// It's a bad one!
			throw (Error) ex;
		}
		return;
	}

	/**
	 * Set a Java-bean property.
	 * This prefers to call obj.setProperty(value), but will fallback to obj.property = value;
	 * It does NOT do private methods or fields. 
	 * @param obj
	 * @param propertyName
	 * @param value
	 */
	public static void setProperty(Object obj, String propertyName, Object value) {
		try {
			// Is there a setter?
			Class<? extends Object> klass = obj.getClass();
			// Convert fooBar to setFooBar
			String setter = "set"+Character.toUpperCase(propertyName.charAt(0))+(propertyName.length()==1? "" : propertyName.substring(1));
			Method m = getMethod(klass, setter);
			if (m != null) {
				m.invoke(obj, value);
				return;
			}
			// Poke the field
			Field f = getField(klass, propertyName);
			checkCanSetField(f);
			setPrivateField2(obj, f, value);
		} catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	/**
	 * @deprecated
	 * TODO STATUS: Not Written!
	 * @param a
	 * @param b
	 * @return true if a.x equals b.x for all fields f which are not null in a and b
	 */
	public static boolean equalish(Object a, Object b) {
		return true;
	}

	public static List getEnumValues(Class type) {
//		Class<?> klass = Class.forName("sun.misc.SharedSecrets");
//		Method getAccess = klass.getMethod("getJavaLangAccess");
//		jla = getAccess.invoke(null);
//		return Arrays.asList(SharedSecrets.getJavaLangAccess().getEnumConstantsShared(type));
		return Arrays.asList("TODO enum values from "+type);
	}

	public static void shallowCopy(Object from, Object to) {
		List<Field> fields = getAllFields(from.getClass());
		try {
			for (Field field : fields) {
				Object v = field.get(from);
				field.set(to, v);
			}
		} catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	/**
	 * 
	 * @param up Number of steps up the stacktrace to look. 
	 * 0=the class which calls this (ie equiv to getClass().getSimpleName() but slower), 
	 * 1=the method before, etc.
	 * 1 is probably the most commonly wanted value.
	 * @return
	 */
	public static String getCallingClassSimpleName(int up) {
		StackTraceElement caller = ReflectionUtils.getCaller(up);
		String cn = caller.getClassName();
		int ldot = cn.lastIndexOf('.');
		if (ldot != -1) cn = cn.substring(ldot+1);
		return cn;
	}

	/**
	 * Like {@link Class#isAnnotationPresent(Class)}, but without a compile-time dependency on the annotation. 
	 * 
	 * @param type
	 * @param annotationName
	 * @return
	 */
	public static boolean hasAnnotation(Class<?> type, String annotationName) {
		Annotation[] annos = type.getAnnotations();
		Annotation anno = Containers.first(Arrays.asList(annos), 
				a -> annotationName.equals(a.getClass().getSimpleName())
				);
		return anno!=null;
	}

	public static Class loadClassFromFile(File classDir, File classFile) {
		return loadClassFromFile(classDir, classFile, null);
	}
	
	public static Class loadClassFromFile(File classDir, File classFile, Collection<File> classPath) {
		try {
		    // Convert File to a URL
		    URL url = classDir.toURL();          // file:/c:/myclasses/
			URL[] urls;
			if (Utils.isEmpty(classPath)) {
			    urls = new URL[]{url};				
			} else {
				ArrayList<URL> listUrls = new ArrayList();
				for(File f : classPath) {
					listUrls.add(f.toURL());
				}
				listUrls.add(url);
				urls = listUrls.toArray(new URL[0]);
			}

		    // Create a new class loader with the directory
		    ClassLoader cl = new URLClassLoader(urls);

		    // Load in the class; MyClass.class should be located in
		    // the directory file:/c:/myclasses/com/mycompany
//		    "com.mycompany.MyClass";
		    String rel = FileUtils.getRelativePath(classFile, classDir);
		    rel = rel.substring(0, rel.length() - 6); // pop .class
		    Class cls = cl.loadClass(rel.replace('/', '.'));
		    return cls;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	
	/**
	 * 
	 * @return system level cpu use, or -1 if unknown
	 */
	public static double getSystemCPU() {
		try {
			// see https://stackoverflow.com/questions/47177/how-do-i-monitor-the-computers-cpu-memory-and-disk-usage-in-java
			// NB: the sun version is not in OpenJDK :( import com.sun.management.OperatingSystemMXBean;
			OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
			double cpuLoad = call(operatingSystemMXBean, "getSystemCpuLoad");		
			return cpuLoad;
		} catch(Throwable ex) {
			return -1;
		}
	}
	
	public static <X> X call(Object object, String methodName, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method m = getMethod(object.getClass(), methodName);
		return (X) m.invoke(object, args);
	}

	/**
	 * 
	 * @return JVM cpu use, or -1 if unknown
	 */
	public static double getJavaCPU() {
		try {
			// see https://stackoverflow.com/questions/47177/how-do-i-monitor-the-computers-cpu-memory-and-disk-usage-in-java
			OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
			double cpuLoad = call(operatingSystemMXBean, "getProcessCpuLoad");		
			return cpuLoad;
		} catch(Throwable ex) {
			return -1;
		}
	}

	public static void TODOgetClassPath() throws IOException {
//		or see Classpath in Bob
//		
//		ClassLoader cl = ClassLoader.getSystemClassLoader();
//		URL r = cl.getResource(ReflectionUtils.class.getCanonicalName());
//		
//		String[] bits = ("META-INF ReflectionUtils ReflectionUtils.class com.winterwell.utils.ReflectionUtils.class "+ReflectionUtils.class.getCanonicalName()).split(" ");
//		for(String s : bits) {
//			Printer.out("\n"+s);
//			Enumeration<URL> rs = cl.getResources(s);		
//			while(rs.hasMoreElements()) {
//				Printer.out(rs.nextElement());
//			}
//		}
	}

}
