package winterwell.utils.web;

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
 * Separated out to isolate the XStream dependency.
 * 
 * @author daniel
 * @testedby {@link XStreamUtilsTest}
 */
public class XStreamUtils extends com.winterwell.utils.web.XStreamUtils {

}
