package com.winterwell.utils.web;

import java.io.File;
import java.util.Collection;

import org.junit.Test;
import org.w3c.dom.Document;

import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.SysOutCollectorStream;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;

public class WebUtils2Test {

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
