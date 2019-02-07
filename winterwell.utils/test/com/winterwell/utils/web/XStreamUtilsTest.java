package com.winterwell.utils.web;

import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.Collections;

import org.junit.Test;

import com.winterwell.utils.containers.ArrayMap;

/**
 * @tested {@link XStreamUtils}
 * 
 * @author daniel
 * 
 */
public class XStreamUtilsTest {

	@Test
	public void testUpgradeArrayMap() {
		{
			String xml = XStreamUtils
					.serialiseToXml(new ArrayMap("foo", "bar"));
			System.out.println(xml);
		}
		{
			ArrayMap map = new ArrayMap("foo", Collections.singletonList("bar"));
			map.keySet();
			map.values();
			map.entrySet();

			String xml = XStreamUtils.serialiseToXml(map);
			System.out.println(xml);
		}
		{ // The old format seems to still work :)
			String xml = "<amap><keys><S>msg.tos</S></keys><values><java.util.Collections_-SingletonList><element class=\"S\">adrianboeh@twitter</element></java.util.Collections_-SingletonList></values><mod>&#x1;</mod></amap>";
			Object u = XStreamUtils.serialiseFromXml(xml);
			System.out.println(u);
		}
		{
			String xml = "<amap><keys><S>msg.tos</S></keys><values><singleton-list><S>chriss_graham@twitter</S></singleton-list></values><mod>&#x1;</mod></amap>";
			Object u = XStreamUtils.serialiseFromXml(xml);
			System.out.println(u);
		}
	}

	@Test
	public void testBadXML() throws URISyntaxException {
		try {
			Object obj = XStreamUtils.serialiseFromXml("fooy yeah");
			assert false;
		} catch (Exception ex) {
			// good
			System.out.println(ex);
		}
	}

	/**
	 * They changed the format?! OK the old format was verbose & bogus, but
	 * breaking backwards compatibility sucks even worse. Fixed via
	 * {@link URIConverter}
	 */
	@Test
	public void testUpgradeXStreamVersion() throws URISyntaxException {
		{
			String xml = XStreamUtils.serialiseToXml(new URI(
					"http://winterstein.me.uk"));
			System.out.println(xml);
			Object u = XStreamUtils.serialiseFromXml(xml);
			System.out.println(u);
		}
		{ // This is "old" xstream output, which is upsetting the new xstream :(
			String uri = "<java.net.URI serialization=\"custom\">\n"
					+ "<java.net.URI>" + "	<default>"
					+ "	<string>http://winterstein.me.uk</string>"
					+ "	</default>" + "	</java.net.URI>" + "</java.net.URI>";
			Object u = XStreamUtils.serialiseFromXml(uri);
			System.out.println(u);
		}
		{
			String xml = "<uri>http://www.circus-london.co.uk</uri>";
			Object u = XStreamUtils.serialiseFromXml(xml);
			System.out.println(u);
		}
	}

	@Test
	public void testDeSerialiseTimestamp() {
		{ // this works fine
			Timestamp ts = new Timestamp(System.currentTimeMillis());
			String xml = XStreamUtils.serialiseToXml(ts);
			System.out.println(xml);
			Object ts2 = XStreamUtils.serialiseFromXml(xml);
			System.out.println(ts2);
			assert ts.equals(ts2);
		}
		if (false) {
			// This xml was sent by dev.soda.sh
			// -- a different-versions-of-linux/postgres issue?
			String xml = "<sql-timestamp>292269055-12-02 23:00:00.0</sql-timestamp>";
			Object ts = XStreamUtils.serialiseFromXml(xml);
			System.out.println(ts);
		}
	}

	@Test
	public void testSerialiseToXml_null() {		
		String nll = XStreamUtils.serialiseToXml(null);
		StringWriter writer = new StringWriter();
		XStreamUtils.serialiseToXml(writer, null);
		String nll2 = writer.toString();
		
		assert nll.equals("<null/>");
		assert nll2.equals("<null/>");
	}

	@Test
	public void testSpecialChars() { // https://stackoverflow.com/questions/22956533/xdocument-will-not-parse-html-entities-e-g-xc-but-xmldocument-will
		// TODO: get rid of \f because it gives XML error and they're useless
		String ff = XStreamUtils.serialiseToXml("Form feed: \f");
		System.out.println(ff);
		Object s = XStreamUtils.serialiseFromXml("<S>&#xc;</S>");
		System.out.println(ff);
	}
	
}
