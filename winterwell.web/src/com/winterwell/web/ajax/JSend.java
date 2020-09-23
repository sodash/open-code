package com.winterwell.web.ajax;

import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.utils.FailureException;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.WebEx;
import com.winterwell.web.app.WebRequest;

/**
 * 
 * {
status: "success"|"fail"|"error", 
message: String, // optional error message 
data: any, // the ajax payload
code: Number // optional numeric code for errors
}
 *
 * Note - this can co-exist with {@link JsonResponse}
 * 
 * c.f. 
 * https://labs.omniti.com/labs/jsend
 * https://stackoverflow.com/questions/50873541/should-i-use-jsend-for-wrapping-json-ajax-responses-or-is-there-a-more-standard
 * 
 * Note: requires Gson set in Dep, as JThing requires this
 * 
 * @author daniel
 *
 */
public class JSend implements IHasJson {

	public JSend() {	
	}
	
	/**
	 * Send this response!
	 * @param request
	 */
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

	/**
	 * NB: success is the default
	 * @param status
	 * @return
	 */
	public JSend setStatus(KAjaxStatus status) {
		this.status = status;
		return this;
	}

	public JThing getData() {
		if (data==null && status!=KAjaxStatus.success) {
			check();
		}
		return data;
	}
	
	public Map getDataMap() {
		return data.map();
	}

	public JSend check() {
		if (status==KAjaxStatus.success) {
			return this;
		}
		if (code==null) {
			throw new FailureException(getMessage());
		}
		if (code == 404) {
			throw new WebEx.E404(null, getMessage());
		}
		throw WebEx.fromErrorCode(code, null, getMessage());
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
		
		// recognise failure codes
		if (status==KAjaxStatus.success) {
			if (code >= 500) status = KAjaxStatus.error;
			else if (code >= 400) status = KAjaxStatus.fail;
		}
		
		return this;
	}

	KAjaxStatus status = KAjaxStatus.success;

	JThing data;
	
	String message;
	
	/**
	 * A numeric code corresponding to the error, if applicable
	 */
	Integer code;
	
	@Override
	public String toJSONString() {
		// TODO for the case where data has a json String, we could be more efficient
		String json = IHasJson.super.toJSONString();
		return json;
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
	
	@Override
	public String toString() {
		return "JSend[ " + toJSONString() + " ]";
	}

	/**
	 * Parse a json response -- using the Eclipse JSON "vanilla" parser, which returns Map/List/primitive.
	 * If you wish to use Gson, do this yourself (as we avoid the dependency here).
	 *  
	 * @param json
	 * @return
	 */
	public static JSend parse(String json) {
		Map jobj = (Map) JSON.parse(json);
		return parse2_create(jobj);
	}

	public static JSend parse2_create(Map jobj) {
		JSend jsend = new JSend();
		Object _code = jobj.get("code");
		if (_code!=null) {
			jsend.setCode((Integer) _code); 
			// should we assume 200 if no info??
		}
		
		String msg = (String) jobj.get("message");
		// HACK a JsonResponse format?
		if (msg==null) {
			Object msgs = jobj.get("messages");
			if (msgs!=null) {
				List<Object> listmsgs = Containers.asList(msgs);
				if ( ! listmsgs.isEmpty()) {
					Object m0 = listmsgs.get(0);
					if (m0 instanceof Map && jsend.code==null) {
						Object code = ((Map) m0).get("code");
						if (code!=null) jsend.setCode((int) MathUtils.toNum(code));
					}
					msg = m0.toString();
				}
			}
		}		
		jsend.setMessage(msg);
		
		Object s = jobj.get("status");
		if (s==null) {
			// HACK a JsonResponse format?
			Object success = jobj.get("success");
			if (success!=null) {
				if (Utils.yes(success)) {
					s = KAjaxStatus.success;
				} else {
					// check code??
					s = KAjaxStatus.error;
				}
			}
		}
		if (s instanceof String) s = KAjaxStatus.valueOf((String)s);
		jsend.setStatus((KAjaxStatus) s);
		Object _data = jobj.get("data");
		// HACK: JsonResponse format?
		if (_data==null) {
			_data = jobj.get("cargo");		
		}
		if (_data != null) {	
			JThing thing = new JThing();
			// the incoming json-object might already have been converted into a POJO!
			if (_data instanceof String || _data instanceof Map || _data instanceof List || _data.getClass().isArray()
					|| ReflectionUtils.isaNumber(_data.getClass())) 
			{
				// normal case: its a json object still
				thing.setJsonObject(_data);
			} else {
				thing.setJava(_data);
			}
			jsend.setData(thing);
		}
		return jsend;
	}

	public boolean isSuccess() {
		return getStatus()==KAjaxStatus.success;
	}
}
