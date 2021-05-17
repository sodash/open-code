package com.winterwell.web.fields;

import java.util.regex.Matcher;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.WebInputException;

public class UrlField extends AField<String> {
	
	UrlField() {
		this("dummy");
	}
	
	public UrlField(String name) {
		super(name, "url");
	}
	
	@Override
	public String fromString(String v) throws Exception {
		// make sure we have http://
		if ( ! v.startsWith("http")) {
			// Could it be a domain name?
			Matcher m = WebUtils2.URL_WEB_DOMAIN_REGEX.matcher(v);
			if (m.matches()) {
				String v2 = "http://"+v;
				Log.d("UrlField", "edit "+name+": Modified "+v+" to "+v2);
				v = v2;
			} else {
				throw new WebInputException(this, "Not a url or a web-domain: "+v);
			}
		}
		// cap the length
		if (v.length() > 3000) {
			throw new WebInputException(this, "Value too long: "+v.length()+" chars: "+StrUtils.ellipsize(v, 140));
		}
		return v;
	}

	private static final long serialVersionUID = 1L;

}
