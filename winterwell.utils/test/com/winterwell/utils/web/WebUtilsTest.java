package com.winterwell.utils.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.winterwell.utils.FailureException;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Proc;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ITree;
import com.winterwell.utils.containers.Tree;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.time.TUnit;

import junit.framework.TestCase;

/**
 * @tested {@link WebUtils}
 * @author daniel
 * 
 */
public class WebUtilsTest extends TestCase {


	public void testGetHost() {
		String u1 = "https://lookup.london/philpot-lane-mice-londons-tiniest-sculpture/";
		String u3 = WebUtils.getHost(u1);
		System.out.println(u3);
	}
	
	
	public void testResolveUrl_issue4180() {
		String u1 = "http://www.bestofyoutube.com/story.php?title=screaming-frog";
		String u2 = "/video.php?id=2366";
		URI u3 = WebUtils.resolveUri(u1, u2);
		System.out.println(u3);
		assert u3.isAbsolute();
		assert u3.toString().equals("http://www.bestofyoutube.com/video.php?id=2366") : u3;
	}
	
	public void testGetType() {
		{
			String t = WebUtils.getType("/foo.css");
			assert t.equals(".css") : t;
		}
		{
			String t = WebUtils.getType("http://blah.blah.yeh/foo.bar.html?hm#3");
			assert t.equals(".html") : t;
		}
		{
			String t = WebUtils.getType("http://gmpg.org/xfn/11");
			assert t.equals(".org/xfn/11") : t;
		}
	}
	
	public void testURI() {
		// Illegal character in authority at index 7: http://travel.americanexpress.co.uk
		// What a great exception message -- describes most governments.
		// But what is going wrong here??
		URI url = WebUtils.URI("http://travel.americanexpress.co.uk");		
	}
	
	public void testFullHostname() {
		String hn = WebUtils.fullHostname();
		System.out.println("#" + hn + "#");
	}

	private void auxTestResolveURI(String base, String extension,
			String expected) {
		URI uri = WebUtils.resolveUri(base, extension);
		assert uri.toString().equals(expected) : base+" + "+extension+" = "+uri+" vs "+expected;
	}

	// This fails -- use CGIUtils.parseHtmlToTree() instead
	public void devtestParseXmlToTreeGuardianPage() {
		String url = "http://www.guardian.co.uk/technology/2012/feb/29/raspberry-pi-computer-sale-british";
		String html = WebUtils.getPage(url);
		Tree<XMLNode> tree = WebUtils.parseXmlToTree(html);
		System.out.println(tree);
	}

	public void testDig() {
		{
			String dug = WebUtils.dig("winterwell.soda.sh", false);
			assertEquals("egan.soda.sh", dug);
			String ip = WebUtils.dig("www.ed.ac.uk", true);
			Printer.out(ip);
			assert ip.startsWith("129.215") : ip;
		}
		{
			String dug = WebUtils.dig("issues.soda.sh", false);
			assertEquals("egan.soda.sh", dug);
		}
	}

	public void testDigHard() {
		int fails = 0;
		for (int i = 0; i < 100; i++) {
			try {
				String dug = WebUtils.dig("fsdfsdfsdfds.soda.sh", false);
				assertEquals("egan.soda.sh", dug);
				System.out.print(i);
			} catch(FailureException ex) {
				fails++;
				System.err.println(ex);
			}
		}
		System.out.println("\n"+fails);
		assert fails < 10 : fails;
	}

	// public void testURIBehaviour() {
	// URI uri = new File("hello").toURI();
	// String s = uri.getScheme();
	// Printer.out(s);
	// }

	public void testExtractTags() {
		{
			List<String> tags = WebUtils.extractXmlTags("a",
					"Hello <a>yes</a> <a href=''>lah\nlah</a> <a yo/>", true);
			assert tags.contains("<a>yes</a>");
			assert tags.contains("<a href=''>lah\nlah</a>");
			assert tags.contains("<a yo/>");
			assert tags.size() == 3;
		}
		{
			List<String> tags = WebUtils.extractXmlTags("a",
					"Hello <a>yes</a> <a href=''>lah\nlah</a> <a yo/>", false);
			assert tags.contains("yes");
			assert tags.contains("lah\nlah");
			assert tags.size() == 2;
		}
		{
			List<String> tags = WebUtils
					.extractXmlTags(
							"a",
							"<a_bob href=''>yoyo</a_bob> <a>yes</a> <a href=''>lah\nlah</a> <a yo/>",
							false);
			assert tags.contains("yes");
			assert tags.contains("lah\nlah");
			assert tags.size() == 2;
		}
	}

	public void testGetAttribute() {
		{
			String v = WebUtils.getAttribute("src",
					"<script> document.write('<img src=\"foo\" />'); </script>");
			assert v == null;
		}
		{
			String v = WebUtils.getAttribute("href",
					"<a href='abc.com'>woo</a>");
			assert v.equals("abc.com");
		}
		{
			String v = WebUtils.getAttribute("href",
					"<a hreff='abc.com'>woo</a>");
			assert v == null;
		}
		{
			String v = WebUtils
					.getAttribute("href", "<a href=abc.com >woo</a>");
			assertEquals("abc.com", v);
		}
	}

	public void testGetMyIP() {
		List<String> ips = WebUtils.getMyIP();
		Printer.out(ips);
		for (String ip : ips) {
			if (ip.contains("127"))
				return;
		}
		assert false : ips;
	}

	public void testGetXMLReader() throws IOException, SAXException {
		XMLReader r = WebUtils.getXMLReader();
		XmlTreeBuilder xtb = new XmlTreeBuilder();
		r.setContentHandler(xtb);
		String xml = "<xml>foo &amp; bar</xml>";
		r.parse(new InputSource(new StringReader(xml)));

		Tree<XMLNode> tree = xtb.getTree();
		ITree<XMLNode> xmlNode = tree.getNode(0);
		ITree<XMLNode> textNode = tree.getNode(0, 0);
		System.out.println(tree);
		assert xmlNode.getValue().getTag().equals("xml");
		assert textNode.getValue().isTextNode();
		assert textNode.getValue().getText().equals("foo & bar");
	}

	/**
	 * Even if there is a no-arg constructor, it doesn't get called by XStream.
	 * Weird. How does that work?
	 */
	public void testHowXStreamWorks() {
		assert Dummy.cnt == 0;
		Dummy dummy = new Dummy();
		assert Dummy.cnt == 1;
		String dXml = XStreamUtils.serialiseToXml(dummy);
		Object dummy2 = XStreamUtils.serialiseFromXml(dXml);
		assert dummy2 instanceof Dummy;
		assert dummy2 != dummy;
		assert Dummy.cnt == 1;
	}

	public void testParseXml() {
		Document doc = WebUtils.parseXml("<test>stuff</test>");
		assert doc != null;
	}

	public void testParseXmlToTreeSnippet() {
		Tree<XMLNode> tree = WebUtils
				.parseXmlToTree("<test><foo a='1'>bar</foo></test>");
		assert tree.getDepth() == 4 : tree;
		assert tree.getValue() == null;
		assert tree.getOnlyChild().getValue().getTag().equals("test");
		XMLNode textNode = tree.getNode(0, 0, 0).getValue();
		assert textNode.isTextNode();
		assert textNode.getTag() == null;
		assert textNode.getText().equals("bar");
	}

	// public void testSerializeToJSON() {
	// HashMap map = new HashMap(new ArrayMap("a",1,"b","two"));
	// String s = WebUtils.serialiseToJSON(map);
	// This fails! assert s.equals("{\"a\": 1, \"b\": \"two\"}") : s;
	// }

	// public void testXStreamJson() {
	// XStream xstream = new XStream(new JsonHierarchicalStreamDriver());
	// String i = xstream.toXML(17);
	// Object i2 = xstream.fromXML(i);
	// assert Integer.valueOf((String) i2) == 17 : i2;
	// }

	public void testProblemXML() throws Exception {
		// This url causes Xerces DOM reader a problem
		XMLReader r = WebUtils.getXMLReader();
		HttpURLConnection connection = (HttpURLConnection) new URL(
				"http://newsrss.bbc.co.uk/rss/newsonline_uk_edition/livestats/most_read/rss.xml")
				.openConnection();
		InputStream in = connection.getInputStream();
		String xml = FileUtils.read(in);
		File out = new File("test/winterwell/utils/web/bbc_rss.xml");
		out.getParentFile().mkdirs();
		FileUtils.write(out, xml);

		// try {
		r.parse(new InputSource(new StringReader(xml)));
		Tree<XMLNode> tree = WebUtils.parseXmlToTree(xml);
		List<XMLNode> kids = tree.getChildValues();
		XMLNode node = kids.get(0);
		// } catch (Exception e) {
		// WTF?
		// e.printStackTrace();
		// }

		// // removing the meta line seems to help
		// String xml2 = xml.replaceFirst("<\\?.+?\\?>", "").trim();
		// Printer.out(xml2);
		//
		// r.parse(new InputSource(new StringReader(xml2)));
		// Tree<XMLNode> tree = WebUtils.parseXmlToTree(xml2);
		// List<XMLNode> kids = tree.getChildValues();
		// XMLNode node = kids.get(0);
	}

	
	public void offtestRenderReportToPdf() throws IOException {
		String url = "http://local.soda.sh/reports.html?iobj=stross_80988%40soda.sh%3AReport&urisig=4f780f0b9c8d48ed7513c40ab95454e8&as=daniel%40winterwellassociates.com%40soda.sh";
//		String url = "http://local.soda.sh/reports";
		File pdf = File.createTempFile("testreport", ".pdf");
		WebUtils.renderUrlToPdf(url, pdf, true, "By SoDash", TUnit.MINUTE.dt);
		Proc p = new Proc("gnome-open " + pdf.getAbsolutePath());
		p.run();
	}
	
	public void offtestRenderToPdf() {
		{
			String html = "<html><head></head><body><h1>PDF ROCKS</h1></body></html>";
			File pdf = new File("test/pdftest1.pdf");
			WebUtils.renderToPdf(html, pdf, null, false);
			Proc p = new Proc("gnome-open " + pdf.getAbsolutePath());
			p.run();
		}
		{
			String html = "<html><head></head><body><h1>PDF ROCKS</h1></body></html>";
			File pdf = new File("test/pdftest1.pdf");
			WebUtils.renderToPdf(html, pdf, null, true);
			Proc p = new Proc("gnome-open " + pdf.getAbsolutePath());
			p.run();
		}
		{
			String html = "<html><head></head><body>PDF <script>document.write(' + Javascript '); /*setTimeout(\"document.append('Rocks!');\",100);*/</script></body></html>";
			File pdf = new File("test/pdftest2.pdf");
			WebUtils.renderToPdf(html, pdf, null, false);
			// "--window-status FLOT_DONE"); //"--javascript-delay 2000");
			Proc p = new Proc("gnome-open " + pdf.getAbsolutePath());
			p.run();
		}
	}

	public void testResolveUri_absolute1() {
		auxTestResolveURI("http://winterstein.me.uk", "http://google.com/",
				"http://google.com/");
	}

	// TEST RESOLVE URI

	public void testResolveUri_tough1() {
		auxTestResolveURI("http://foo.bar/tough1/yeah", "/index",
				"http://foo.bar/index");
		auxTestResolveURI("https://foo.bar/tough1/yeah.html", "/index.php",
				"https://foo.bar/index.php");
	}
	
	public void testResolveUri_absolute2() {
		auxTestResolveURI("http://foo.bar", "http://foo.bar/index",
				"http://foo.bar/index");
	}

	public void testResolveUri_javascript() {
		auxTestResolveURI("http://foo.bar/images/etc/", "javascript:init()",
				"javascript:init()");
	}

	public void testResolveUri_relative1() {
		auxTestResolveURI("http://winterstein.me.uk", "images/dan.jpg",
				"http://winterstein.me.uk/images/dan.jpg");
		
		// ./ in-this-directory relative paths
		auxTestResolveURI("http://bikeradar.com/forums/", "./viewforum.php",
				"http://bikeradar.com/forums/viewforum.php");
		
		// This shouldn't work
//		auxTestResolveURI("http://bikeradar.com/forums", "./viewforum.php",
//				"http://bikeradar.com/forums/viewforum.php");
	}
	

	public void testResolveUri_relative2() {
		auxTestResolveURI("http://foo.bar", "/index", "http://foo.bar/index");
	}
	
	public void testResolveUri_relative3() {
		auxTestResolveURI("http://foo.bar/images/flunkey.jpg", "monkey.jpg",
				"http://foo.bar/images/monkey.jpg");
	}

	public void testResolveUri_relative4() {
		auxTestResolveURI("http://foo.bar/images/", "monkey.jpg",
				"http://foo.bar/images/monkey.jpg");
	}

	public void testResolveUri_relative5() {
		auxTestResolveURI("http://foo.bar/images/", "/text/",
				"http://foo.bar/text/");
	}

	public void testResolveUri_relative6() {
		auxTestResolveURI("http://foo.bar/images/", "./text/",
				"http://foo.bar/images/text/");
	}

	public void testResolveUri_relative7() {
		auxTestResolveURI("http://foo.bar/images/etc/", "../../text/",
				"http://foo.bar/text/");
	}

	public void testResolveUri_relative8() {
		auxTestResolveURI("http://foo.bar/images/etc/", "..",
				"http://foo.bar/images/");
	}

	public void testSerializeToXML() {
		String nll = XStreamUtils.xstream().toXML(null);

		assert XStreamUtils.serialiseToXml(null).equals("<null/>");
		assertEquals("<S> </S>", XStreamUtils.serialiseToXml(" "));
		String one = XStreamUtils.serialiseToXml(1);
		assertEquals("<i>1</i>", one);

		// just wanted to check this has a sane serialisation
		String en = XStreamUtils.serialiseToXml(Locale.ENGLISH);
		assert en.length() < 100;
	}

	public void testStripTags() {
		String a = WebUtils.stripTags("a <b><!--This is a comment--> c");
		assert a.equals("a  c") : a;
		String b = WebUtils
				.stripTags("a <b> c < 2<font sasa sasa/>.<a href=''>my stuff\n\n</a>");
		assert b.equals("a  c < 2.my stuff\n\n") : b;
		String c = WebUtils
				.stripTags("<!-- This is a tricksy, >hobbity comment-->Hello!");
		assertEquals("Hello!", c);
		String d = WebUtils
				.stripTags("<!DOCTYPE madeup><!--comment <stuff> more comment -->Hel<tag></tag>lo!");
		assertEquals("Hello!", d);
		{
			String txt = "@greenpartydan <I can believe their story #Meerkats";
			String txt2 = WebUtils.stripTags(txt);
			assertEquals(txt, txt2);
		}
		{
			String txt = "hello <script type='javascript'>\nfunction foo() {\n"
					+ "document.write('<div></div>');\n}\n</script>world";
			String txt2 = WebUtils.stripTags(txt);
			assertEquals("hello world", txt2);
		}
		{ // style block
			String txt = "foo<style type=\"text/css\" media=\"all\">a.new,#quickbar a.new{color:#ba0000}\n\n"
					+ "/* cache key: enwiki:resourceloader:filter:minify-css:5:f2a9127573a22335c2a9102b208c73e7 */</style>bar";
			String txt2 = WebUtils.stripTags(txt);
			assertEquals("foobar", txt2);
		}
		{ // weird comment
			String txt = "<!--[if lt IE 7]><style type=\"text/css\">foo</style><![endif]-->";
			String txt2 = WebUtils.stripTags(txt);
			assertEquals("", txt2);
		}
		{ // multiple blocks
			String wikiText = "<head> \n"
					+ "<title>Cat - Wikipedia, the free encyclopedia</title> \n"
					+ "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" /> \n"
					+ "<meta http-equiv=\"Content-Style-Type\" content=\"text/css\" /> \n"
					+ "<meta name=\"generator\" content=\"MediaWiki 1.17wmf1\" /> \n"
					+ "<link rel=\"apple-touch-icon\" href=\"http://en.wikipedia.org/apple-touch-icon.png\" /> \n"
					+ "<link rel=\"shortcut icon\" href=\"/favicon.ico\" /> \n"
					+ "<link rel=\"search\" type=\"application/opensearchdescription+xml\" href=\"/w/opensearch_desc.php\" title=\"Wikipedia (en)\" /> \n"
					+ "<link rel=\"EditURI\" type=\"application/rsd+xml\" href=\"http://en.wikipedia.org/w/api.php?action=rsd\" /> \n"
					+ "<link rel=\"copyright\" href=\"http://creativecommons.org/licenses/by-sa/3.0/\" /> \n"
					+ "<link rel=\"alternate\" type=\"application/atom+xml\" title=\"Wikipedia Atom feed\" href=\"/w/index.php?title=Special:RecentChanges&amp;feed=atom\" /> \n"
					+ "<link rel=\"stylesheet\" href=\"http://bits.wikimedia.org/en.wikipedia.org/load.php?debug=false&amp;lang=en&amp;modules=mediawiki%21legacy%21commonPrint%7Cmediawiki%21legacy%21shared%7Cskins%21vector&amp;only=styles&amp;skin=vector\" type=\"text/css\" media=\"all\" /> \n"
					+ "<meta name=\"ResourceLoaderDynamicStyles\" content=\"\" /><link rel=\"stylesheet\" href=\"http://bits.wikimedia.org/en.wikipedia.org/load.php?debug=false&amp;lang=en&amp;modules=site&amp;only=styles&amp;skin=vector\" type=\"text/css\" media=\"all\" /> \n"
					+ "<style type=\"text/css\" media=\"all\">a.new,#quickbar a.new{color:#ba0000}\n"
					+ " \n"
					+ "/* cache key: enwiki:resourceloader:filter:minify-css:5:f2a9127573a22335c2a9102b208c73e7 */</style> \n"
					+ "<script src=\"/w/extensions/OggHandler/OggPlayer.js?12\" type=\"text/javascript\"></script><script type=\"text/javascript\"> \n"
					+ "wgOggPlayer.msg = {\"ogg-play\": \"Play\", \"ogg-pause\": \"Pause\", \"ogg-stop\": \"Stop\", \"ogg-no-player\": \"Sorry, your system does not appear to have any supported player software.\\nPlease \\x3ca href=\\\"http://www.mediawiki.org/wiki/Extension:OggHandler/Client_download\\\"\\x3edownload a player\\x3c/a\\x3e.\", \"ogg-player-videoElement\": \"Native browser support\", \"ogg-player-oggPlugin\": \"Browser plugin\", \"ogg-player-cortado\": \"Cortado (Java)\", \"ogg-player-vlc-mozilla\": \"VLC\", \"ogg-player-vlc-activex\": \"VLC (ActiveX)\", \"ogg-player-quicktime-mozilla\": \"QuickTime\", \"ogg-player-quicktime-activex\": \"QuickTime (ActiveX)\", \"ogg-player-totem\": \"Totem\", \"ogg-player-kaffeine\": \"Kaffeine\", \"ogg-player-kmplayer\": \"KMPlayer\", \"ogg-player-mplayerplug-in\": \"mplayerplug-in\", \"ogg-player-thumbnail\": \"Still image only\", \"ogg-player-selected\": \"(selected)\", \"ogg-use-player\": \"Use player:\", \"ogg-more\": \"More…\", \"ogg-download\": \"Download file\", \"ogg-desc-link\": \"About this file\", \"ogg-dismiss\": \"Close\", \"ogg-player-soundthumb\": \"No player\", \"ogg-no-xiphqt\": \"You do not appear to have the XiphQT component for QuickTime.\\nQuickTime cannot play Ogg files without this component.\\nPlease \\x3ca href=\\\"http://www.mediawiki.org/wiki/Extension:OggHandler/Client_download\\\"\\x3edownload XiphQT\\x3c/a\\x3e or choose another player.\"};\n"
					+ "wgOggPlayer.cortadoUrl = \"http://upload.wikimedia.org/jars/cortado.jar\";\n"
					+ "wgOggPlayer.extPathUrl = \"/w/extensions/OggHandler\";\n"
					+ "</script><style type=\"text/css\" media=\"all\"> \n"
					+ ".ogg-player-options {\n"
					+ "	border: solid 1px #ccc;\n"
					+ "	padding: 2pt;\n"
					+ "	text-align: left;\n"
					+ "	font-size: 10pt;\n"
					+ "}\n"
					+ " \n"
					+ ".center .ogg-player-options ul {\n"
					+ "	margin: 0.3em 0px 0px 1.5em;\n"
					+ "}\n"
					+ "</style><script type=\"text/javascript\">wgNamespaceNumber=0;wgAction=\"view\";wgPageName=\"Cat\";wgMainPageTitle=\"Main Page\";wgWikimediaMobileUrl=\"http:\\/\\/en.m.wikipedia.org\\/wiki\";</script><script src=\"http://bits.wikimedia.org/w/extensions-1.17/WikimediaMobile/MobileRedirect.js?8.2\" type=\"text/javascript\"></script><!--[if lt IE 7]><style type=\"text/css\">body{behavior:url(\"/w/skins-1.17/vector/csshover.min.htc\")}</style><![endif]--></head>\n";
			String txt2 = WebUtils.stripTags(wikiText);
			txt2 = StrUtils.compactWhitespace(txt2).trim();
			assertEquals("Cat - Wikipedia, the free encyclopedia", txt2);
		}
	}

	public void testUrlEncode() throws UnsupportedEncodingException {
		{
			String a = WebUtils.urlEncode("a+b");
			String b = WebUtils.urlEncode("a b");
			assert !a.equals(b);
		}
		{ // problem with "s
			String a = WebUtils.urlEncode("Clegg OR \"Vince Cable\"");
			String b = WebUtils.urlDecode(a);
			assertEquals("Clegg OR \"Vince Cable\"", b);
		}
		{ // curiosity
			String a = WebUtils.urlEncode("a..b"); // doesn't get encoded
			String b = WebUtils.urlEncode("a;"); // gets encoded
			System.out.println(a + " " + b);

			String str = new String("/.");
			byte[] ba = str.getBytes(StrUtils.ENCODING_UTF8);
			for (int j = 0; j < ba.length; j++) {
				System.out.print('%');
				char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
				// // converting to use uppercase letter as part of
				// // the hex value if ch is a letter.
				// if (Character.isLetter(ch)) {
				// ch -= caseDiff;
				// }
				System.out.print(ch);
				ch = Character.forDigit(ba[j] & 0xF, 16);
				// if (Character.isLetter(ch)) {
				// ch -= caseDiff;
				// }
				System.out.print(ch);
			}
		}
	}

	public void testGetDomain() {
		{
			String d = WebUtils.getDomain("good-loop-xmas.com");
			assert d.equals("good-loop-xmas.com") : d;
		}
		{
			String d = WebUtils.getDomain("https://good-loop-xmas.com?foo=bar");
			assert d.equals("good-loop-xmas.com") : d;
		}
		{ 
			String d = WebUtils.getDomain("https://foo.soda.sh/bar");
			assert d.equals("soda.sh") : d;
		}
		{ 
			String d = WebUtils.getDomain("http://foo.soda.sh");
			assert d.equals("soda.sh") : d;
		}
		{ 
			String d = WebUtils.getDomain("http://soda.sh");
			assert d.equals("soda.sh");
		}
		{ 
			String d = WebUtils.getDomain("http://soda.com/whatever");
			assert d.equals("soda.com");
		}
		{
			String d = WebUtils.getDomain("http://www.runnersworld.co.uk/gear/gear-pick-merrell-barefoot-road-glove-dash-2/9500.html");
			assert d.equals("runnersworld.co.uk") : d;
		}
		{
			String d = WebUtils.getDomain("www.foobar.com");
			assert d.equals("foobar.com") : d;
		}
		{
			String d = WebUtils.getDomain("https://lookup.london/philpot-lane-mice-londons-tiniest-sculpture/");
			assert d.equals("lookup.london") : d;
		}
		{
			String d = WebUtils.getDomain("lookup.london");
			assert d.equals("lookup.london") : d;
		}
	}
	
	public void testUrlRegex() {
		{ // chop trailing punctuation
			Matcher m = WebUtils.URL_REGEX
					.matcher("hello http://whatever.com/?yes=no.");
			assert m.find();
			assertEquals("http://whatever.com/?yes=no", m.group());
		}
		{ // just a domain?
			Matcher m = WebUtils.URL_REGEX
					.matcher("foo http://guardian.co.uk bar");
			assert m.find();
			assertEquals("http://guardian.co.uk", m.group());
		}
		{ // allow google ugly urls
			Matcher m = WebUtils.URL_REGEX
					.matcher("http://maps.google.com/maps?oe=UTF-8&q=39+grassmarket&ie=UTF8&hl=en&hq=&hnear=39+Grassmarket,+Edinburgh,+City+of+Edinburgh+EH1+2,+United+Kingdom&layer=c&cbll=55.947368,-3.196047&panoid=dZwAoV-1fYO10lzhNCToyQ&cbp=12,198.65,,0,-9.62&ll=55.947308,-3.196256&spn=0.01103,0.038581&z=15");
			assert m.find();
			assertEquals(
					"http://maps.google.com/maps?oe=UTF-8&q=39+grassmarket&ie=UTF8&hl=en&hq=&hnear=39+Grassmarket,+Edinburgh,+City+of+Edinburgh+EH1+2,+United+Kingdom&layer=c&cbll=55.947368,-3.196047&panoid=dZwAoV-1fYO10lzhNCToyQ&cbp=12,198.65,,0,-9.62&ll=55.947308,-3.196256&spn=0.01103,0.038581&z=15",
					m.group());
		}
		{ // Wikipedia uses unescaped 's
			Matcher m = WebUtils.URL_REGEX
					.matcher("What about http://en.wikipedia.org/wiki/Battle_of_Rorke's_Drift?");
			assert m.find();
			assertEquals(
					"http://en.wikipedia.org/wiki/Battle_of_Rorke's_Drift",
					m.group());
		}
		{ // Wikipedia also uses :s
			Matcher m = WebUtils.URL_REGEX
					.matcher("What about http://en.wikipedia.org/Special:Random?");
			assert m.find();
			assertEquals("http://en.wikipedia.org/Special:Random", m.group());
		}
		{ // Facebook likes #s and !s
			Matcher m = WebUtils.URL_REGEX
					.matcher("Check out http://www.facebook.com/#!/bloiffy!");
			assert m.find();
			assertEquals("http://www.facebook.com/#!/bloiffy", m.group());
		}
		{ // LinkedIn is insane
			Matcher m = WebUtils.URL_REGEX
					.matcher("http://www.linkedin.com/profile?viewProfile=&key=13553626&goback=.nmp_*1_*1&trk=NUS_UNIU_VIRAL-profile");
			assert m.find();
			assertEquals(
					"http://www.linkedin.com/profile?viewProfile=&key=13553626&goback=.nmp_*1_*1&trk=NUS_UNIU_VIRAL-profile",
					m.group());
		}
		{ // https
			Matcher m = WebUtils.URL_REGEX
					.matcher("https://www.example.com/fubar");
			assert m.find();
			assertEquals("https://www.example.com/fubar", m.group());
		}
	}
	
	

	public void testUrlOrDomainRegex() {
		{ // chop trailing punctuation
			Matcher m = WebUtils.URL_WEB_DOMAIN_REGEX
					.matcher("hello http://whatever.com/?yes=no.");
			assert m.find();
			assertEquals("http://whatever.com/?yes=no", m.group());
		}
		{ // just a domain?
			Matcher m = WebUtils.URL_WEB_DOMAIN_REGEX
					.matcher("foo http://guardian.co.uk bar");
			assert m.find();
			assertEquals("http://guardian.co.uk", m.group());
		}
		{ // allow google ugly urls
			Matcher m = WebUtils.URL_WEB_DOMAIN_REGEX
					.matcher("http://maps.google.com/maps?oe=UTF-8&q=39+grassmarket&ie=UTF8&hl=en&hq=&hnear=39+Grassmarket,+Edinburgh,+City+of+Edinburgh+EH1+2,+United+Kingdom&layer=c&cbll=55.947368,-3.196047&panoid=dZwAoV-1fYO10lzhNCToyQ&cbp=12,198.65,,0,-9.62&ll=55.947308,-3.196256&spn=0.01103,0.038581&z=15");
			assert m.find();
			assertEquals(
					"http://maps.google.com/maps?oe=UTF-8&q=39+grassmarket&ie=UTF8&hl=en&hq=&hnear=39+Grassmarket,+Edinburgh,+City+of+Edinburgh+EH1+2,+United+Kingdom&layer=c&cbll=55.947368,-3.196047&panoid=dZwAoV-1fYO10lzhNCToyQ&cbp=12,198.65,,0,-9.62&ll=55.947308,-3.196256&spn=0.01103,0.038581&z=15",
					m.group());
		}
		{ // Wikipedia uses unescaped 's
			Matcher m = WebUtils.URL_WEB_DOMAIN_REGEX
					.matcher("What about http://en.wikipedia.org/wiki/Battle_of_Rorke's_Drift?");
			assert m.find();
			assertEquals(
					"http://en.wikipedia.org/wiki/Battle_of_Rorke's_Drift",
					m.group());
		}
		{ // Wikipedia also uses :s
			Matcher m = WebUtils.URL_WEB_DOMAIN_REGEX
					.matcher("What about http://en.wikipedia.org/Special:Random?");
			assert m.find();
			assertEquals("http://en.wikipedia.org/Special:Random", m.group());
		}
		{ // Facebook likes #s and !s
			Matcher m = WebUtils.URL_WEB_DOMAIN_REGEX
					.matcher("Check out http://www.facebook.com/#!/bloiffy!");
			assert m.find();
			assertEquals("http://www.facebook.com/#!/bloiffy", m.group());
		}
		{ // LinkedIn is insane
			Matcher m = WebUtils.URL_WEB_DOMAIN_REGEX
					.matcher("http://www.linkedin.com/profile?viewProfile=&key=13553626&goback=.nmp_*1_*1&trk=NUS_UNIU_VIRAL-profile");
			assert m.find();
			assertEquals(
					"http://www.linkedin.com/profile?viewProfile=&key=13553626&goback=.nmp_*1_*1&trk=NUS_UNIU_VIRAL-profile",
					m.group());
		}
		{ // https
			Matcher m = WebUtils.URL_WEB_DOMAIN_REGEX
					.matcher("https://www.example.com/fubar");
			assert m.find();
			assertEquals("https://www.example.com/fubar", m.group());
		}
		
		
		// JUST DOMAINS
		{
			Matcher m = WebUtils.URL_WEB_DOMAIN_REGEX
					.matcher("at sodash.com blah blah");
			assert m.find();
			assertEquals("sodash.com", m.group());
		}
	}

	public void testXPathQuery() {
		{
			String xml = "<foo>\n" + "<bar><t>Hello</t></bar>\n"
					+ "<bar><t>World</t></bar>\n" + "</foo>";
			List<Node> nodes = WebUtils.xpathQuery("//bar/t", xml, false);
			assert nodes.size() == 2 : Printer.toString(nodes);
			assert nodes.get(0).getTextContent().equals("Hello");
			assert nodes.get(1).getTextContent().equals("World");
		}
		{
			String xml = "<foo><bar a='1'><t>One</t></bar>\n"
					+ "<bar a='2'><t>Hello</t></bar>\n\n"
					+ "<bar a='2'><t>World</t></bar>\n\n" + "</foo>";
			List<Node> nodes = WebUtils.xpathQuery("//bar[@a='2']/t", xml,
					false);
			assert nodes.size() == 2 : Printer.toString(nodes);
			assert nodes.get(0).getTextContent().equals("Hello");
			assert nodes.get(1).getTextContent().equals("World");
		}
	}

	public void testXPathQuery2() {
		{
			String xml = "<foo>" + "<bar id='b1'><t>Hello</t></bar>"
					+ "<bar id='b2'><t>World</t></bar>" + "</foo>";
			Document doc = WebUtils.parseXml(xml);
			Node bar1 = doc.getFirstChild().getFirstChild();
			String id = WebUtils.getAttribute("id", bar1);
			assert id.equals("b1") : id;
			String text = bar1.getTextContent();
			assert text.equals("Hello") : text;
			List<Node> nodes = WebUtils.xpathQuery("//t", bar1);
			assert nodes.size() == 1 : Printer.toString(nodes);
			assert nodes.get(0).getTextContent().equals("Hello");
		}
	}
	
	static class Dummy {
		static int cnt;

		Dummy() {
			cnt++;
			Printer.out("Dummy " + cnt);
		}
	}
}