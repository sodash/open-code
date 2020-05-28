package com.winterwell.web.app;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import com.winterwell.utils.Utils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.test.TestHttpServletRequest;
import com.winterwell.web.test.TestHttpServletResponse;

public class TestWebRequest extends WebRequest {

	public TestWebRequest() {
		this(new TestHttpServletRequest(), new TestHttpServletResponse());
	}

	public TestWebRequest(TestHttpServletRequest req, TestHttpServletResponse resp) {
		super(req,resp);
	}

	public TestHttpServletRequest getTestRequest() {
		return (TestHttpServletRequest) getRequest();
	}

	public static TestWebRequest fromUrl(String url) {
		try {
			Map<String, String> ps = WebUtils2.getQueryParameters(url);
			TestHttpServletRequest req = new TestHttpServletRequest(ps);
			
			String pathInfo = new URI(url).getPath();
			req.setPathInfo(pathInfo);
			
			return new TestWebRequest(req, new TestHttpServletResponse());
		} catch (Exception ex) {
			throw Utils.runtime(ex);
		}
			
	}
	
}
