package com.winterwell.web.app;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.winterwell.utils.Dep;

public class ShareServletTest {

	@Test
	public void testDoScreenshot() throws IOException {
		Dep.set(Uploader.class, new Uploader(new File("temp-test"), "/temp-test"));
		ShareServlet ss = new ShareServlet();
		String url = ss.doScreenshot("https://www.bbc.co.uk/news/science-environment-53415294");
		System.out.println(url);
	}

}
