package com.winterwell.utils.web;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.w3c.dom.Document;

import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.SysOutCollectorStream;
import com.winterwell.utils.time.TUnit;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.WebPage;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.JettyLauncher;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.test.TestHttpServletResponse;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;

public class WebUtils2Test {


	@Test
	public void testAddCookie() {
		JettyLauncher jl = new JettyLauncher(FileUtils.getWorkingDirectory(), 8961);
		jl.addServlet("/dummy", new DummyServlet());
		jl.run();
		
		FakeBrowser fb = new FakeBrowser();
		String foo = fb.getPage("http://localhost:8961/dummy");
		Map<String, String> cookies = fb.getHostCookies("localhost");
		Printer.out(cookies);
		assert cookies.containsKey("foo");
	}

		
	@Test // MimeUtil v 1.3. works -- v2.1 breaks
	public void testGetMimeType_uncommon() {
//		MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
		File f = new File("test/more.txt");
		assert f.isFile() : f.getAbsolutePath();
		Collection mts = MimeUtil.getMimeTypes(f);
		Printer.out(mts);
		Object mt = Containers.first(mts);
		assert mt.toString().startsWith("text/") : mt;
	}
	
	@Test
	public void testQuotedPrintableEncodeDecode() {
		String qp = "<div align=3D\"center\" >=09=09Hello!</div>";
		String plain = WebUtils2.decodeQuotedPrintable(qp);
		String enc = WebUtils2.encodeQuotedPrintable(plain);
		assert plain.equals("<div align=\"center\" >		Hello!</div>") : plain;
		assert enc.equals(qp) : enc;
	}

	
	@Test
	public void testXmlDocToString() {
		String xml = "<foo blah='whatever'><bar src=''>huh?</bar></foo>";
		Document doc = WebUtils2.parseXml(xml);
		String xml2 = WebUtils2.xmlDocToString(doc);
		String c = StrUtils.compactWhitespace(xml2.trim()).replaceAll("[\r\n]", "").replace('"', '\'');
		assert c.equals(xml) : xml2+" -> "+c;
	}

	@Test
	public void testCanonicalEmailString() {
		{
			String e = WebUtils2.canonicalEmail("Bob <Bob@FOO.COM>");
			assert e.equals("bob@foo.com");
		}
		{
			String e = WebUtils2.canonicalEmail("Alice.1@FOO.bar.co.uk");
			assert e.equals("alice.1@foo.bar.co.uk");
		}
	}

}


class DummyServlet extends HttpServlet {			
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		WebRequest state = new WebRequest(req, resp);
		state.setCookie("foo", "bar", TUnit.MINUTE.dt, null);
		WebPage wp = new WebPage();
		wp.sb().append("Hello World");
		state.setPage(wp);
		state.sendPage();
	}
	
}
