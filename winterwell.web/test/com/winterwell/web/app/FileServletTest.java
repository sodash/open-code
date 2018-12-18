package com.winterwell.web.app;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.winterwell.web.test.TestHttpServletResponse;

public class FileServletTest {

	@Test
	public void testServeDirectorySmokeTest() throws IOException {
		FileServlet fs = new FileServlet(new File("test"));
		TestWebRequest request = new TestWebRequest();
		fs.serveDirectory(new File("test"), request);
		TestHttpServletResponse resp = (TestHttpServletResponse) request.getResponse();
		String out = resp.getOutputBufferContents();
		System.out.println(out);
	}

}
