/**
 * 
 */
package com.winterwell.web.fields;

import com.winterwell.utils.web.WebUtils;

/**
 * A String field whose {@link #getValue(javax.servlet.http.HttpServletRequest)}
 * method strips out all xml tags. This means that script tags, links (and any
 * other malicious html) will be stripped. This does NOT protect against
 * command-line or SQL hacks!
 * 
 * @author daniel
 * @testedby  SafeStringTest}
 */
public class SafeString extends AField<String> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SafeString(String name) {
		super(name);
	}

	public SafeString(String name, String type) {
		super(name, type);
	}

	public SafeString(String name, String type, boolean required) {
		super(name, type, required);
	}

	@Override
	public String fromString(String s) {
		s = WebUtils.stripTags(s);
		return s;
	}

}
