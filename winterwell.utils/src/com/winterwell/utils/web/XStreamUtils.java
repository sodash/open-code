package com.winterwell.utils.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import winterwell.utils.Key;
import winterwell.utils.Utils;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.containers.ArrayMap;

import winterwell.utils.containers.ArraySet;
import winterwell.utils.io.XStreamBinaryConverter;
import winterwell.utils.io.XStreamBinaryConverter.BinaryXML;
import winterwell.utils.reporting.Log;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.CompactWriter;

/**
 * Special case serialiser for {@link ConcurrentHashMap}.
 * <p>
 * NB: ConcurrentHashMap also has a special case in Java serialisation.
 * 
 * @author daniel
 */
final class ConcurrentMapConverter implements SingleValueConverter {

	@Override
	public boolean canConvert(Class type) {
		return type == ConcurrentHashMap.class;
	}

	@Override
	public Object fromString(String str) {
		HashMap<?, ?> amap = XStreamUtils.serialiseFromXml(str);
		ConcurrentHashMap map = new ConcurrentHashMap(amap);
		return map;
	}

	@Override
	public String toString(Object obj) {
		Map<?, ?> map = (Map) obj;
		HashMap<?, ?> amap = new HashMap(map);
		return XStreamUtils.serialiseToXml(amap);
	}

}

/**
 * Special case serialiser for {@link Timestamp}, due to bugs in transmission of
 * Timestamps between different servers.
 * <p>
 * This loses some accuracy (nano vs milli-seconds) -- does anyone care?
 * 
 * @author daniel
 */
final class TimestampConverter implements SingleValueConverter {

	@Override
	public boolean canConvert(Class type) {
		return type == Timestamp.class;
	}

	@Override
	public Object fromString(String str) {
		// TODO should we also handle the xml from an un-modified XStream here?
		Long utc = Long.valueOf(str);
		return new Timestamp(utc);
	}

	@Override
	public String toString(Object obj) {
		Timestamp ts = (Timestamp) obj;
		return Long.toString(ts.getTime());
	}

}

/**
 * Separated out to isolate the XStream dependency.
 * 
 * @author daniel
 * @testedby {@link XStreamUtilsTest}
 */
public class XStreamUtils {

	private static XStream _xstream;

	// static final XStream JSON_XSTREAM = new XStream(new
	// JsonHierarchicalStreamDriver());

	/**
	 * Setup aliases to give prettier shorter xml. E.g. &lt;S>hello&lt;/S>
	 * instead of &lt;java.lang.String>hello&lt;/java.lang.String>
	 */
	static void initShorterXml(XStream xstream) {
		// prettier shorter xml
		xstream.alias("key", Key.class);
		xstream.alias("map", HashMap.class);
		xstream.alias("amap", ArrayMap.class);
		xstream.alias("cmap", ConcurrentHashMap.class);
		xstream.alias("list", ArrayList.class);
		xstream.alias("set", HashSet.class);
		xstream.alias("aset", ArraySet.class);
		// primitives (this is ugly)
		xstream.alias("I", Integer.class);
		xstream.alias("i", int.class);
		xstream.alias("L", Long.class);
		xstream.alias("l", long.class);
		xstream.alias("D", Double.class);
		xstream.alias("d", double.class);
		xstream.alias("S", String.class);
		xstream.alias("B", Boolean.class);
		xstream.alias("b", boolean.class);
	}

	/**
	 * Does NOT close reader (better the caller does that in a finally block)
	 * 
	 * @param xml
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <X> X serialiseFromXml(InputStream xml) {
		X x = (X) xstream().fromXML(xml);
		return x;
	}

	/**
	 * Does NOT close reader (better the caller does that in a finally block)
	 * 
	 * @param xml
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <X> X serialiseFromXml(Reader xml) {
		X x = (X) xstream().fromXML(xml);
		return x;
	}

	@SuppressWarnings("unchecked")
	public static <X> X serialiseFromXml(String xml) {
		try {
			return (X) xstream().fromXML(xml);
		} catch(Throwable ex) {
			// add in a bit more info - like the top level class
			throw new WrappedException("Cause: "+StrUtils.ellipsize(xml, 140), ex);
		}
	}

	/**
	 * Uses XStream
	 * <p>
	 * <h3>Thread Safety</h3>
	 * XStream serialisation is not thread safe :( If your object is edited by
	 * another thread during a save, it can lead to a corrupted version getting
	 * saved.
	 * <p>
	 * This method synchronises on the top-level object to provide a bit of
	 * safety. However there are cases where this is not enough: e.g. if child
	 * objects have synchronised blocks but the top level object doesn't.
	 * 
	 * @param object
	 * @return an xml representation for object
	 * @see #serialiseFromXml(String)
	 */
	public static String serialiseToXml(Object object) {
		// neither can nor need to sychronise on null - this is what xstream
		// would return
		if (object == null)
			return "<null/>";
		StringWriter sw = new StringWriter();
		serialiseToXml(sw, object);
		return sw.toString();
	}

	/**
	 * Uses XStream
	 * <p>
	 * <h3>Thread Safety</h3>
	 * XStream serialisation is not thread safe :( If your object is edited by
	 * another thread during a save, it can lead to a corrupted version getting
	 * saved.
	 * <p>
	 * This method synchronises on the top-level object to provide a bit of
	 * safety. However there are cases where this is not enough: e.g. if child
	 * objects have synchronised blocks but the top level object doesn't.
	 * 
	 * @param writer
	 *            Serialise out to here. This will NOT be closed here.
	 * @param object
	 * @see #serialiseFromXml(String)
	 */
	public static void serialiseToXml(Writer writer, Object object) {
		// We neither can nor need to sychronise on null - this is what xstream
		// would return
		if (object == null) {
			try {
				writer.write("<null/>");
				return;
			} catch (IOException e) {
				throw Utils.runtime(e);
			}
		}
		synchronized (object) {
			try {
				CompactWriter compact = new CompactWriter(writer);
				xstream().marshal(object, compact);
			} catch(Throwable ex) {
				// add in a bit more info
				throw new WrappedException("Cause: "+object.getClass()+": "+object, ex);
			}
		}
	}

	static void setupXStream() {
		try {
			_xstream = new XStream();
			// be robust & keep on truckin
			_xstream.ignoreUnknownElements();
			// prettier shorter xml
			XStreamUtils.initShorterXml(_xstream);
			// Binary fields
			_xstream.registerConverter(new XStreamBinaryConverter());
			// ConcurrentHashMaps
			_xstream.registerConverter(new ConcurrentMapConverter());
			// Hack: SQL Timestamps
			_xstream.registerConverter(new TimestampConverter());
			// HACK: Depot support -- via dynamic loading (no explicit
			// dependency)
			try {
				Class k = Class
						.forName("com.winterwell.depot.ModularConverter");
				Converter conv = (Converter) k.newInstance();
				_xstream.registerConverter(conv);
			} catch (Exception ex) {
				// oh well -- no depot support
				Log.i("xstream", "Oh well -- no ModularConverter: " + ex);
			}

			// Backwards-compatibility: XStream changed how it formats URIs
			_xstream.registerConverter(new URIConverter());

			// return _xstream;
		} catch (Throwable e) {
			Log.report(e);
			// return null;
		}
	}

	/**
	 * Default XStream instance with:<br>
	 * - shorter xml c.f. {@link #initShorterXml(XStream)}<br>
	 * - {@link BinaryXML} via {@link XStreamBinaryConverter}<br>
	 * - better ConcurrentHashMap output via {@link ConcurrentMapConverter}
	 */
	public static XStream xstream() {
		if (_xstream == null) {
			setupXStream();
		}
		return _xstream;
	}

}

/**
 * XStream pre v1.4 had an ugly output for URIs. So they changed it -- and broke
 * backwards compatibility! This class provides backwards compatibility.
 * 
 * @see XStreamUtilsTest#testUpgradeXStreamVersion()
 * @author daniel
 */
final class URIConverter implements Converter {

	@Override
	public boolean canConvert(Class type) {
		return type == URI.class;
	}

	/**
	 * Produces the same output as XStream 1.4
	 */
	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		writer.setValue(source.toString());
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		// Drill down to get the url.
		// For new saves, we have <uri>http://winterstein.me.uk</uri>
		// For old saves, it's:
		// <java.net.URI serialization="custom">
		// <java.net.URI><default>
		// <string>http://winterstein.me.uk</string>
		// </default></java.net.URI></java.net.URI>
		// (so 3 drill-downs do the trick)
		if (!reader.hasMoreChildren()) {
			return WebUtils.URI(reader.getValue());
		}
		reader.moveDown();
		reader.moveDown();
		reader.moveDown();
		return WebUtils.URI(reader.getValue());
		// while(true) { // a bit less efficient
		// String v = reader.getValue();
		// if (WebUtils.URL_REGEX.matcher(v).matches()) {
		// return v;
		// }
		// reader.moveDown();
		// }
	}

}
