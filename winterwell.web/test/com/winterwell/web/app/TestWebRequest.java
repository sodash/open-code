package com.winterwell.web.app;

import static org.junit.Assert.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import com.winterwell.web.test.TestHttpServletRequest;
import com.winterwell.web.test.TestHttpServletResponse;

public class TestWebRequest extends WebRequest {

	public TestWebRequest() {
		super(new TestHttpServletRequest(), new TestHttpServletResponse());
	}

	public TestHttpServletRequest getTestRequest() {
		return (TestHttpServletRequest) getRequest();
	}
	
}
