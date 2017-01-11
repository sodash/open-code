package com.winterwell.web;

import java.net.URI;
import java.util.Date;

public class RSSItem {

	public String author;
	public Date date;
	public String desc;
	public URI link;
	public String title;

	public RSSItem(String title, URI link, String desc, Date date) {
		if (date == null) {
			date = new Date();
		}
		this.title = title;
		this.link = link;
		this.desc = desc;
		this.date = date;
	}

	public void appendTo(StringBuilder rss) {
		rss.append("<item>");
		RSSWriter.titleLinkDesc(rss, title, link, desc);
		if (author != null) {
			rss.append("<author>" + author + "</author>\n");
		}
		rss.append("<pubDate>" + date.toGMTString() + "</pubDate>\n");
		rss.append("</item>\n");
	}

	public void setAuthor(String author) {
		this.author = author;
	}
}