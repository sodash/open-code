package com.winterwell.utils.io;

import java.io.Serializable;
import java.util.Arrays;

import org.junit.Test;

import com.winterwell.utils.io.XStreamBinaryConverter.BinaryXML;

import com.thoughtworks.xstream.XStream;
import com.winterwell.utils.web.XStreamUtils;

public class XStreamBinaryConverterTest {

	@Test
	public void testDummyClass() {
		Dummy d1 = new Dummy();
		d1.a = "Hello World";
		d1.b = new double[] { 0, 1, 2, 3, 4, 5 };
		XStreamUtils.xstream().registerConverter(new XStreamBinaryConverter());

		String xml = XStreamUtils.serialiseToXml(d1);

		assert !xml.contains("Hello World") : xml;
		System.out.println(xml);

		Dummy d2 = XStreamUtils.serialiseFromXml(xml);

		assert d2.a.equals(d1.a);
		assert Arrays.equals(d2.b, d1.b);
	}

	// @Test This doesn't work
	public void testLegacy() {
		Dummy d1 = new Dummy();
		d1.a = "Hello World";
		d1.b = new double[] { 0, 1, 2, 3, 4, 5 };

		String xml = new XStream().toXML(d1);

		XStreamUtils.xstream().registerConverter(new XStreamBinaryConverter());

		Dummy d2 = XStreamUtils.serialiseFromXml(xml);

		assert d2.a.equals(d1.a);
		assert Arrays.equals(d2.b, d1.b);
	}

	@BinaryXML
	static class Dummy implements Serializable {
		private static final long serialVersionUID = 1L;
		String a;
		double[] b;

	}
}