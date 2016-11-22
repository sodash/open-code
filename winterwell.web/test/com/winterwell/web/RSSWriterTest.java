package com.winterwell.web;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Date;

import com.winterwell.web.RSSWriter;

import junit.framework.TestCase;

public class RSSWriterTest extends TestCase {

	public void testAddItem() {
		try {
			URI uri = new URI("http://www.winterwell.com");
			RSSWriter rssw = new RSSWriter("title", uri, "description");
			URI uri2 = new URI("http://www.xml.com");
			rssw.addItem("item", uri2, "description about xml",
					Date.valueOf("2009-02-17"));
			assert rssw
					.getRSS()
					.contains(
							"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
									+ "<rss version=\"2.0\">\n"
									+ "<channel>\n"
									+ "<title>title</title>\n"
									+ "<link>http://www.winterwell.com</link>\n"
									+ "<description>description</description>\n"
									+ "<item><title>item</title>\n"
									+ "<link>http://www.xml.com</link>\n"
									+ "<description>description about xml</description>\n"
									+ "<pubDate>17 Feb 2009 00:00:00 GMT</pubDate>\n"
									+ "</item>\n" + "</channel>\n</rss>\n");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public void testGetRSS() throws URISyntaxException {
		URI uri = new URI("http://www.winterwell.com");
		RSSWriter rssw = new RSSWriter("title", uri, "description");
		assert rssw.getRSS().equals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
						+ "<rss version=\"2.0\">\n" + "<channel>\n"
						+ "<title>title</title>\n"
						+ "<link>http://www.winterwell.com</link>\n"
						+ "<description>description</description>\n"
						+ "</channel>\n</rss>\n");
	}

}
