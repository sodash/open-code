package com.winterwell.web.ajax;

import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.app.WebRequest;

/**
 * c.f. https://stackoverflow.com/questions/50873541/should-i-use-jsend-for-wrapping-json-ajax-responses-or-is-there-a-more-standard
 * @author daniel
 *
 */
public class JSend implements IHasJson {

	public JSend() {	
	}
	
	public void send(WebRequest request) {
		WebUtils2.sendJson(request, toJSONString());
	}
	
	/**
	 * Normal case: success - here's your data
	 * @param data
	 */
	public JSend(Object dataPOJO) {
		this(new JThing(dataPOJO));
	}

	public JSend(JThing data) {
		this.data = data;
		setStatus(KAjaxStatus.success);
	}

	public KAjaxStatus getStatus() {
		return status;
	}

	public JSend setStatus(KAjaxStatus status) {
		this.status = status;
		return this;
	}

	public JThing getData() {		
		return data;
	}

	public JSend setData(Map data) {
		return setData(new JThing().setMap(data));
	}
	
	public JSend setData(JThing data) {
		this.data = data;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public JSend setMessage(String message) {
		this.message = message;
		return this;
	}

	public Integer getCode() {
		return code;
	}

	public JSend setCode(Integer code) {
		this.code = code;
		return this;
	}

	KAjaxStatus status;

	JThing data;
	
	String message;
	
	/**
	 * A numeric code corresponding to the error, if applicable
	 */
	Integer code;
	
	@Override
	public String toJSONString() {
		// TODO for the case where data has a json String, we could be more efficient
		return IHasJson.super.toJSONString();
	}
	
	@Override
	public Object toJson2() throws UnsupportedOperationException {
		return new ArrayMap(
			"status", status,
			"data", data==null? null : data.toJson2(),
			"message", message,
			"code", code
				);
	}
	
	public static JSend parse(String json) {
		Map jobj = (Map) JSON.parse(json);
		JSend jsend = new JSend();
		jsend.setCode((Integer) jobj.get("code"));
		jsend.setMessage((String) jobj.get("message"));
		Object s = jobj.get("status");
		if (s instanceof String) s = KAjaxStatus.valueOf((String)s);
		jsend.setStatus((KAjaxStatus) s);
		Object _data = jobj.get("data");
		if (_data != null) {
			String djson = JSON.toString(_data);
			jsend.setData(new JThing().setJson(djson));
		}
		return jsend;
	}
}
