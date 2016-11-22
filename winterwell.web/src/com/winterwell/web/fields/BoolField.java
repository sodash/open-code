package com.winterwell.web.fields;

import javax.servlet.http.HttpServletRequest;

import com.winterwell.utils.Utils;
import com.winterwell.web.WebInputException;

/**
 * A hidden boolean field. Unlike {@link Checkbox} this can return true false or
 * null!
 * <p>
 * To support submitting false from an html checkbox, this will check for "name_OFF".
 * So if you do:
 * <code><pre>
 * 	&lt;input type='checkbox' name='foo' &gt;
 * 	&lt;input type='hidden' name='foo_OFF' &gt;
 * </pre></code>
 * Then a form submit will always return true/false.
 * This differs from Checkbox's behaviour, in that it can still return null if
 * the request comes from a different form, without the hidden value.
 * 
 * @author daniel
 * 
 */
public class BoolField extends AField<Boolean> {
	private static final long serialVersionUID = 1L;

	/**
	 * A hidden boolean field. Unlike {@link Checkbox} this can return true false or
	 * null!
	 */
	public BoolField(String name) {
		super(name, "hidden");
	}
	
	@Override
	public Class getValueClass() {
		return Boolean.class;
	}
	
	@Override
	public String getStringValue(HttpServletRequest request)
			throws MissingFieldException 
	{
		String v = super.getStringValue(request);
		if (v!=null) return v;
		// Was a separate OFF sent? This is useful with html checkboxes
		String[] vs = request.getParameterValues(getName()+"_OFF");
		if (vs==null) return null;
		for (String _v : vs) {
			if (Utils.isBlank(_v)) continue;
			return _v;
		}
		return null;
	}		

	@Override
	public Boolean fromString(String s) {
//		// TODO #12-3.1
//		// handle "unknown"
//		if (s.equals("unknown")) return null;
//		return Utils.yes(s);
		
		// "on" will be sent by a value-less checkbox
		// - eg if the sending form is hand-written html
		if (s.equals("true") || s.equals("on") || s.equals("1"))
			return Boolean.TRUE;
		// "off" shouldn't happen, but just in case
		if (s.equals("false") || s.equals("off") || s.equals("0"))
			return Boolean.FALSE;
		if (s.equals("unknown"))
			return null;
		throw new WebInputException("Invalid boolean value: " + s);
	}

	@Override
	public String toString(Boolean value) {
		if (value == null)
			return "unknown";
		return value ? "true" : "false";
	}
}
