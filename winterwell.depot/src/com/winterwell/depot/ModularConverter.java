package com.winterwell.depot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.path.PathTracker;
import com.thoughtworks.xstream.io.path.PathTrackingWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.XStreamBinaryConverter.BinaryXML;
import com.winterwell.utils.log.KErrorPolicy;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.XStreamUtils;

/**
 * Alter XStream to save IHasDesc objects as their Descs,
 * and inflate them via Depot.
 * <p>
 * To use this, mark your class with @ModularXML
 * 
 * {@link XStreamUtils#setupXStream()} will try to set this up within its XStream.
 * 
 * @author daniel
 * @testedby {@link ModularConverterTest}
 */
public class ModularConverter implements Converter {

	/**
	 * Not sure if this can overlap .
	 */
	private static final String DESC_ATTRIBUTE = "_ddsc";
	
	static ThreadLocal<KErrorPolicy> onNotFound = new ThreadLocal();

	public static KErrorPolicy setOnNotFound(KErrorPolicy policy) {
		KErrorPolicy old = onNotFound.get();
		onNotFound.set(policy);
		return old;
	}
	
	/**
	 * @param artifact
	 * @return Never null, can be empty. Does not include artifact.
	 * Does not recurse to collect sub-sub-modules.
	 */
	public static Collection<IHasDesc> getModules(Object artifact) {
		if (artifact.getClass().isAnnotationPresent(SearchForSubModules.class)) {
			return getModules2_search(artifact);
		}
		if (artifact instanceof IHasDesc) {
			IHasDesc[] ms = ((IHasDesc) artifact).getModules();
			if (ms==null) return Collections.EMPTY_LIST;
			// correct usage check
			for (IHasDesc m : ms) {
				if ( ! m.getClass().isAnnotationPresent(ModularXML.class)) {
					throw new IllegalStateException("in "+artifact.getClass()+" "+m.getClass()+" is not a module");
				}
			}
			return Arrays.asList(ms); 
		}
		return Collections.EMPTY_LIST;
	}

	/**
	 * Walk the object graph (argh!) looking for module parts.
	 * Recurses through most stuff, but not into a module
	 * (the assumption being that will be done separately via another put).
	 * @param artifact
	 * @return
	 */
	static HashSet<IHasDesc> getModules2_search(Object artifact) {
		assert artifact != null;
		HashSet seen = new HashSet();
		HashSet<IHasDesc> found = new HashSet();
		LinkedList agenda = new LinkedList();
		agenda.add(artifact);
		seen.add(artifact);
		try {
			// Efficiency: lets do it with a loop instead of recursion, cos this could create a deep stack.
			while( ! agenda.isEmpty()) {
				Object x = agenda.pop();
				// NB: Do this on objects, not fields, 'cos we do want to process e.g. Object x = new HashMap(); 
				if (getModules2_ignore(x)) continue;
				// TODO a concurrent mod exception was seen here?! August 2012
				List<Field> fields = getModules2_getFields(x.getClass());
				for (Field field : fields) {
					if (ReflectionUtils.isTransient(field)) continue;
					Object v = field.get(x);
					if (v==null) continue;
					if (seen.contains(v)) continue;
					seen.add(v);
					if (v.getClass().isAnnotationPresent(ModularXML.class)) {
						found.add((IHasDesc) v);
						// don't recurse into modules -- assume they'll be examined as part of their own Depot.put
					} else {
						// recurse
						agenda.add(v);
					}
				}				
			}
			return found;
		} catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}
	

	/**
	 * @param x
	 * @return true => ignore x
	 */
	private static boolean getModules2_ignore(Object x) {
		if (x instanceof Map || x instanceof Collection) return false;
		// skip all java stuff -- except collections
		String p = x.getClass().getName();
		if (p!=null && p.startsWith("java.")) {
			return true;
		}
		// HACK: skip Hibernate DBObjects -- they're not serialised by xml
		if (ENTITY_CLASS!=null && x.getClass().isAnnotationPresent(ENTITY_CLASS)) {
			return true;
		}
		return false;
	}
	
	static Class ENTITY_CLASS;


	private static List<Field> getModules2_getFields(Class klass) {
		return ReflectionUtils.getAllFields(klass);
	}


	public ModularConverter() {
		Mapper mapper = XStreamUtils.xstream().getMapper();
		ReflectionProvider reflection = XStreamUtils.xstream().getReflectionProvider();
		reflect = new ReflectionConverter(mapper, reflection);
		try {
			ENTITY_CLASS = Class.forName("javax.persistence.Entity");
		} catch (ClassNotFoundException e) {
			// oh well
		}
	}

	
	/**
	 * Marks that instances of this class should be searched for sub-modules.
	 * This can be slow!
	 * @deprecated It's better to explicitly provide the sub-modules via {@link IHasDesc#getModules()}.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	// Meta-annotation for "Don't throw this away during compilation"
	@Target({ ElementType.TYPE })
	// Meta-annotation for "Only allowed on classes"
	public @interface SearchForSubModules {
	}
	
	@Override
	public boolean canConvert(Class type) {
		java.lang.annotation.Annotation a = type.getAnnotation(ModularXML.class);
		if (a==null) return false;
		// Safety checks
		assert ReflectionUtils.isa(type, IHasDesc.class) : type;
		assert type.getAnnotation(BinaryXML.class) == null : type;		
		return true;
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
//		HierarchicalStreamWriter w = writer.underlyingWriter();
//		System.out.println(w);
		if (writer instanceof PathTrackingWriter) {
			PathTrackingWriter ptw = (PathTrackingWriter) writer;
			PathTracker pt = ReflectionUtils.getPrivateField(ptw, "pathTracker");
			int depth = pt.depth();
//			Path path = pt.getPath();
//			String ps = path.toString();
			if (depth==1) { //ps.indexOf('/', 1) == -1) {
				// Top Level!
				marshal2_top(source, writer, context);
				return;
			}
		}
//		Log.v("depot", "modular save for "+source);
		marshal2_sub(source, writer, context);
	}

	final ReflectionConverter reflect;
	
	private void marshal2_top(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		reflect.marshal(source, writer, context);
	}

	private void marshal2_sub(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) 
	{
		IHasDesc ihd = (IHasDesc) source;
		Desc desc = ihd.getDesc();
		assert desc != null;
		String d = XStreamUtils.serialiseToXml(desc);
		writer.addAttribute(DESC_ATTRIBUTE, d);		
	}

	Depot depot;
	
	@Override
	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) 
	{
		// Is it a Desc?
		String dxml = reader.getAttribute(DESC_ATTRIBUTE);
		if (dxml!=null) {
			Desc d = XStreamUtils.serialiseFromXml(dxml);
			if (depot==null) {
				depot = Depot.getDefault();
			}
			Object arti = depot.get(d);
			if (arti==null) {
				KErrorPolicy err = onNotFound.get();
				// Note: Depot sets this to be noisy: a random null showing up after deserialisation could be a real
				// pain to track down! But Desc.markForMerge() calling Utils.copy() wants it quiet.
				if (err==null) err=KErrorPolicy.RETURN_NULL;
				switch(err) {
				 case REPORT:
					 Log.w("xstream", new NotInDepotException(d));
				 case ACCEPT: case IGNORE: case RETURN_NULL:
					 return null;
				 case THROW_EXCEPTION:
					 throw new NotInDepotException(d);
				 case DIE: 
					 Log.e("xstream", new NotInDepotException(d));
					 System.exit(1);
				 default:
					 throw new IllegalStateException(""+err);
				}					
			}
			return arti;
		}			
		
		// normal unmarshal
		Object obj = reflect.unmarshal(reader, context);
		
		// FIXME sanity check -- bughunt Apr 2014
		if (obj instanceof IHasDesc) {
			// hm -- seeing an assert fail here from WWModel signature=null Jan 2015
			((IHasDesc) obj).getDesc();
		}
		
		return obj;
	}

}

class NotInDepotException extends NullPointerException {

	public NotInDepotException(Desc d) {
		super(d.toString());
	}
	
}