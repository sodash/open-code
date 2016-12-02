package com.winterwell.utils.web;

import org.junit.Test;

import com.winterwell.utils.containers.Tree;

public class HtmlParserTest {

	@Test
	public void testParseHtmlToTree() throws Exception {
		String url = "http://www.guardian.co.uk/technology/2012/feb/29/raspberry-pi-computer-sale-british";
		String html = WebUtils.getPage(url);
		Tree<XMLNode> tree = HtmlParser.parseHtmlToTree(html);
		System.out.println(tree);
	}
}
