package com.winterwell.web.fields;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.winterwell.utils.IBuildStrings;
import com.winterwell.utils.IProperties;
import com.winterwell.utils.Key;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.ISerialize;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.IWidget;
import com.winterwell.web.WebInputException;
import com.winterwell.utils.Environment;
import com.winterwell.utils.Printer;

/**
 * Describes a form field. toString() returns the field's name.
 * <p>
 * If for some reason you wish to give a negative output, then "false" is the
 * preferred coding for false/null/off.
 * 
 * @author daniel
 */
public class AField<X> extends Key<X> implements Serializable, IWidget,
		ISerialize<X> {

	private static final Key<IProperties> HTTP_REQUEST_PROPERTIES = new Key<IProperties>(
			"AField.Props");

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static IProperties getRequestProperties() {
		return Environment.get().get(HTTP_REQUEST_PROPERTIES);
	}

	/**
	 * Set this request as the thread-local request object. It is used to supply
	 * default values for {@link #getValue()}, thus preserving use input between
	 * calls.
	 */
	public static IProperties setRequest(HttpServletRequest req) {
		RequestProps props = new RequestProps(req);
		return setRequest(props);
	}

	/**
	 * Set this request as the thread-local request object. It is used to supply
	 * default values for {@link #getValue()}, thus preserving use input between
	 * calls.
	 */
	public static IProperties setRequest(IProperties props) {
		Environment.get().put(HTTP_REQUEST_PROPERTIES, props);
		return props;
	}

	public String cssClass = "";

	public String getCssClass() {
		return cssClass;
	}
	
	/**
	 * null by default
	 */
	protected String id;

	protected String onChange;
	protected boolean required;
	private int size;

	protected String tooltip;

	/**
	 * This is the type attribute for the input tag - it is *not* the type of
	 * the field!
	 */
	private String type;

	/**
	 * Convenience for making a simple text field from a Key
	 * @param key
	 */
	public AField(Key key) {
		this(key.getName(), "text");
	}
	
	public AField(String name) {
		this(name, "text");
	}

	public AField(String name, String type) {
		this(name, type, false);
	}

	public AField(String name, String type, boolean required) {
		super(name);
		this.type = type;
		this.required = required;
	}

	/**
	 * Add all the just-this-once attributes (tabindex, etc) to the opening
	 * input tag. Call from within appendHtmlTo.
	 * 
	 * @param page
	 * @param attributes
	 *            should not be null.
	 */
	protected void addAttributes(StringBuilder page, Object... attributes) {
		ArrayMap<String, Object> attrs = new ArrayMap(attributes);
		for (String k : attrs.keySet()) {
			Object v = attrs.get(k); // allow Strings, ints, boolean
			String vs = v.toString();
			page.append(k + "='" + WebUtils.attributeEncode(vs) + "' ");
		}
	}

	/**
	 * Convenience wrapper for
	 * {@link WebUtils2#addQueryParameter(String, String, Object)}.
	 * 
	 * @param url
	 *            If this is a StringBuilder it will be modified directly!
	 *            Otherwise a new StringBuilder will be created
	 * @param value
	 *            can be null in which case nothing is added
	 * @return url with value encoded as a GET parameter
	 */
	public StringBuilder addQueryParameter(CharSequence url, X value) {
		if (value == null)
			return StrUtils.sb(url);
		StringBuilder sb = url instanceof StringBuilder ? (StringBuilder) url
				: new StringBuilder(url);
		WebUtils2.addQueryParameter(sb, this.getName(), toString(value));
		return sb;
	}
	
	@Override
	public final void appendHtmlTo(IBuildStrings page) {
		appendHtmlTo(page.sb());
	}

	public final void appendHtmlTo(IBuildStrings page, X value,
			Object... attributes) {
		appendHtmlTo(page.sb(), value, attributes);
	}

	/**
	 * Wrapper for {@link #appendHtmlTo(StringBuilder, Object)} which fetches a
	 * value from the request.
	 * 
	 * @param page
	 */
	@Override
	public final void appendHtmlTo(StringBuilder page) {
		appendHtmlTo(page, getValue());
	}
	
	/**
	 * @param cssClass e.g. form-control
	 * @return this
	 */
	public <F extends AField> F setCssClass(String cssClass) {
		this.cssClass = cssClass;
		return (F) this;
	}

	/**
	 * Append the html for this field to a page. A potentially slightly more
	 * efficient base for {@link #getHtml(Object)}.
	 * 
	 * @param page
	 * @param value
	 *            The current value. can be null
	 * @param attributes
	 *            any once-only HTML options that should be set.
	 */
	public void appendHtmlTo(StringBuilder page, X value, Object... attributes) {
		String v = value == null ? "" : toString(value);
		// Escape quotes
		v = WebUtils.attributeEncode(v);
		// size (irrelevant for many fields)
		String sizeAttr = "";
		if ("text".equals(type) || size > 0) {
			int len = Math.min(Math.max(v.length(), 20), 60);
			sizeAttr = " size='" + (size > 0 ? size : len) + "'";
		}
		// Use " to enclose the value since ' does not get escaped, so 's in v
		// can cause problems
		page.append("<input type='" + type + "' name='" + WebUtils.attributeEncode(getName())
				+ "' value=\"" + v + '"' + sizeAttr + " class='" + cssClass
				+ "' ");
		if (id != null) {
			page.append("id='" + id + "' ");
		}
		if (onChange != null) {
			page.append("onChange='" + WebUtils.attributeEncode(onChange)
					+ "' ");
		}
		if (tooltip != null) {
			page.append("title='" + WebUtils.attributeEncode(tooltip) + "' ");
		}
		if (required) {
			page.append("required "); // only works for html5 browser, but
										// harmless elsewhere
		}
		addAttributes(page, attributes);
		// XHTML ending
		page.append("/>\n");
	}

	/**
	 * Uses {@link #getValueClass()}, and claims to convert any subclass.
	 */
	@Override
	public boolean canConvert(Class klass) {
		return ReflectionUtils.isa(klass, getValueClass());
	}

	/**
	 * Subclasses should override and convert from String to whatever. This is
	 * also the place to perform validation.
	 * 
	 * @param v
	 *            Never null or blank. "false" is the preferred coding for
	 *            false/null/off. This has already been url-decoded.
	 * @return value converted into correct form. Can be null for unset
	 * @throws Exception
	 *             This will be converted into a {@link WebInputException} by
	 *             {@link #getValue(HttpServletRequest)}.
	 */
	@Override
	public X fromString(String v) throws Exception {
		return (X) v;
	}

	/**
	 * HTML code for this form field. If the field had a value in the request
	 * received, then this will be used to set the value. This is for handling
	 * form errors, where it is only polite to preserve the input data rather
	 * than making the poor schmuck enter it all again. Use
	 * {@link #getHtml(null)} if you want to ensure a blank value.
	 */
	public final String getHtml() {
		X v = getValue();
		return getHtml(v);
	}

	/**
	 * @param value
	 *            Will be converted by {@link #toString(Object)}. Can be null.
	 * @return An &lt;input&gt; element with name, value, type and cssClass set
	 * @deprecated OK, but use {@link #getHtml()} instead for most uses
	 *             Over-ride {@link #appendHtmlTo(StringBuilder, Object)} if you
	 *             need to.
	 */
	@Deprecated
	public final String getHtml(X value) {
		StringBuilder sb = new StringBuilder();
		appendHtmlTo(sb, value);
		return sb.toString();
	}

	/**
	 * Get the String value for this parameter. Used by
	 * {@link #getValue(HttpServletRequest)}.
	 * <p>
	 * TODO testing across J2EE containers, browsers and nations.
	 *  
	 * Warning: null and "" are made equivalent here (since many web forms conflate them with "key=")
	 * 
	 * @param request
	 * @return value or null if unset/blank (uses {@link Utils#isBlank(String)})
	 * @throws MissingFieldException
	 *             if the field is required and unset
	 */
	public String getStringValue(HttpServletRequest request)
			throws MissingFieldException {
		// UTF8 please... but there were bugs
		// String enc = request.getCharacterEncoding();
		// if(enc == null) {
		// request.setCharacterEncoding("UTF-8");
		
		// This should perform url decoding
		// Get the array, in case we have some nulls (as a checkbox hack we use can create)
		String[] vs = request.getParameterValues(getName());
		String v = null;
		if (vs!=null) {
			for (String _v : vs) {
				if (Utils.isBlank(_v)) continue;
				v = _v;
				break;
			}
		}
		// null? (or "" or " ", since those would fail the isBlank() above)
		if (v==null) {
			if (required)
				throw new MissingFieldException(this);
			return null;
		}

		// v = WebUtils.urlDecode(v); // WTF? This isn't done already by
		// HttpServletRequest?
		// Problem showed up with Spoon's video link. should test this
		// rigorously!
		// This *breaks* the text editor when %s are used, so something is wrong
		// somewhere!
		// Enforce UTF8?
		try {
			byte[] bytes = v.getBytes("UTF-8");
			String v2 = new String(bytes, "UTF-8");
			v = v2;
		} catch (UnsupportedEncodingException e) {
			Log.report(e);
		}
		// Using Jetty 6 with Get from Java Everything just works? :)
		// Using Jetty 6 with Post from FireFox 3:
		// if ISO-8859 is specified, ISO-8859 are fine & beyond gets html
		// encoded
		// if encoding UTF-8 is specified, garbage comes out
		//
		return v;
	}

	public final String getType() {
		return type;
	}

	/**
	 * @return value to set as default in the html
	 */
	protected final X getValue() {
		IProperties props = Environment.get().get(HTTP_REQUEST_PROPERTIES);
		if (props == null)
			return null;
		X v = props.get(this);
		return v;
	}

	/**
	 * Get the value for this parameter.
	 * 
	 * @param request
	 * @return the value, converted into the correct class, or null if unset
	 * @see #getStringValue(HttpServletRequest)
	 * @see #fromString(String) Over-ride this to implement type conversion.
	 */
	/* TODO: Consider refactoring to take a RequestState */
	public final X getValue(HttpServletRequest request)
			throws WebInputException {
		String v = getStringValue(request);
		if (v == null)
			return null;
		try {
			return fromString(v);
		} catch (WebInputException e) {
			throw e;
		} catch (Exception e) {
			Log.w("web.input", e);
			throw new WebInputException("Form value for " + getName()
					+ " is invalid: " +StrUtils.ellipsize(v, 300), e);
		}
	}

	/**
	 * Extract a get arg
	 * 
	 * @param uri
	 * @return value set in this uri, or null
	 */
	public X getValue(URI uri) {
		return getValue(uri.toString());
	}
	
	/**
	 * Extract a get arg
	 * 
	 * @param uri
	 * @return value set in this uri, or null
	 */
	public X getValue(String url) {
		String v = WebUtils2.getQueryParameter(url, getName());
		if (v == null)
			return null;
		try {
			return fromString(v);
		} catch (WebInputException e) {
			throw e;
		} catch (Exception e) {
			throw new WebInputException("Form value for " + getName()
					+ " is invalid: " + v, e);
		}
	}

	/**
	 * TODO this is only implemented in a handful of places! It defaults to String
	 * 
	 * @return the class of values, i.e. whatever X is bound to
	 */
	public Class<X> getValueClass()	
	{
		return (Class) String.class;
	}

	public boolean isRequired() {
		return required;
	}

	public StringBuilder queryParameter(X value) {
		StringBuilder sb = new StringBuilder(getName());
		sb.append("=");
		if (value != null) {
			sb.append(WebUtils.urlEncode(toString(value)));
		}
		return sb;
	}

	/**
	 * Input element id for use in JavaScript
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * This is the type attribute for the input tag (e.g. text, or hidden) - it
	 * is *not* the (Java) type of the field!
	 */
	public void setInputType(String type) {
		this.type = type;
	}

	/**
	 * Javascript handler. Will be encoded using
	 * {@link WebUtils#attributeEncode(String)} ad wrapped in quote marks.
	 * 
	 * @param onChange
	 */
	public void setOnChange(String onChange) {
		this.onChange = onChange;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	/**
	 * Sets how wide a text or password field should be. It has no effect on any
	 * other type of field.
	 * 
	 * @param size
	 */
	public void setSize(int size) {
		this.size = size;
	}

	/**
	 * @param tooltip
	 * @return this for convenient chaining in field initialisers
	 */
	public AField<X> setTooltip(String tooltip) {
		this.tooltip = tooltip;
		return this;
	}

	/**
	 * @param value
	 *            never null
	 * @return string representation for value. This is a normal unencoded
	 *         string. attribute character encoding is handled elsewhere. The
	 *         default implementation just uses toString().<br>
	 * @throws Exception 
	 * @see convertString(String) which is the inverse of this
	 */
	@Override
	public String toString(X value) {
		if (value == null)
			throw new RuntimeException();
		String v = value.toString();
		return v;
	}
}

/**
 * Provide values for sending out via {@link #getHtml()}.
 */
final class RequestProps implements IProperties {

	private final Map<Key, Object> properties = new HashMap<Key, Object>();
	private final HttpServletRequest req;

	public RequestProps(HttpServletRequest req) {
		assert req != null;
		this.req = req;
	}

	@Override
	public <T> boolean containsKey(Key<T> key) {
		T result = get(key);
		return (result != null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Key<T> key) {
		Object v = properties.get(key);
		if (v == null) {
			AField field = (AField) key;
			try {
				// Were we sent a value? If so copy it back out
				v = field.getValue(req);
			} catch (MissingFieldException e) {
				// Ignore
			}
		}
		return (T) v;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Key> getKeys() {
		Set<Key> keys = new HashSet<Key>();
		keys.addAll(properties.keySet());
		for (Object k : req.getParameterMap().keySet()) {
			keys.add(new Key(k.toString()));
		}
		return keys;
	}

	@Override
	public boolean isTrue(Key<Boolean> key) {
		Boolean v = get(key);
		return v != null && v;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T put(Key<T> key, T value) {
		if (value == null)
			return (T) properties.remove(key);
		else
			return (T) properties.put(key, value);
	}

	@Override
	public String toString() {
		return Printer.toString(properties);
	}

}
