package com.winterwell.web.app;

import com.winterwell.utils.web.WebUtils;
import com.winterwell.web.test.TestHttpServletRequest;
import com.winterwell.web.test.TestHttpServletResponse;

public class WebRequestTest {

//	@Test -- sadly the test things dont cut it yet
	public void testGetSlug() {
		TestHttpServletRequest req = new TestHttpServletRequest();
		req.setUri("http://foo.example.com/servy/"+WebUtils.urlEncode("<sluggy@garden>")+".html?foo=1");
		TestHttpServletResponse resp = new TestHttpServletResponse();
		WebRequest twr = new WebRequest(req, resp);
		String slug = twr.getSlug();
		assert slug.equals("foo") : slug;
	}

}
