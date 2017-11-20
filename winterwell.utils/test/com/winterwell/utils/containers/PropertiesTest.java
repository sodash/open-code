package com.winterwell.utils.containers;

import org.junit.Test;

import com.winterwell.utils.web.XStreamUtils;

public class PropertiesTest {

	@Test
	public void testXMLFail() {
		String xml = "<props class='com.winterwell.utils.containers.Properties'><properties/></props>";
		Object props = XStreamUtils.serialiseFromXml(xml);
		System.out.println(props);
	}
}
