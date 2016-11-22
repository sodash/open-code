package winterwell.web;

import java.net.URI;
import java.util.Date;

import winterwell.utils.web.WebUtils;

/**
 * TODO <author> is an optional sub-element of <item>.It's the email address of
 * the author of the item.
 * 
 * 
 * @testedby {@link RSSWriterTest}
 * @author daniel
 */
public class RSSWriter {

	public static class RSSItem {

		private String author;
		private Date date;
		private String desc;
		private URI link;
		private String title;

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
			titleLinkDesc(rss, title, link, desc);
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

	static String encode(String string) {
		string = string.replace("&", "&amp;");
		string = string.replace("<", "&lt;");
		string = string.replace(">", "&gt;");
		return string;
	}

	/**
	 * 
	 * @param rss
	 * @param title
	 *            Will have any tags stripped from it
	 * @param link
	 * @param desc
	 *            Can be null
	 */
	static void titleLinkDesc(StringBuilder rss, String title, URI link,
			String desc) {
		assert title != null;
		if (desc == null) {
			desc = "";
		}
		title = WebUtils.stripTags(title);
		desc = encode(desc);
		rss.append("<title>" + title + "</title>\n" + "<link>"
				+ encode(link.toString()) + "</link>\n" + "<description>"
				+ desc + "</description>\n");
	}

	private final StringBuilder rss = new StringBuilder();

	/**
	 * 
	 * @param title
	 * @param link
	 * @param desc
	 *            Can be null
	 */
	public RSSWriter(String title, URI link, String desc) {
		rss.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
		rss.append("<rss version=\"2.0\">\n");
		rss.append("<channel>\n");
		titleLinkDesc(rss, title, link, desc);
	}

	public void addItem(RSSItem item) {
		item.appendTo(rss);
	}

	public void addItem(String title, URI link, String desc, Date date) {
		RSSItem item = new RSSItem(title, link, desc, date);
		item.appendTo(rss);
	}

	public String getRSS() {
		return rss + "</channel>\n</rss>\n";
	}
}
