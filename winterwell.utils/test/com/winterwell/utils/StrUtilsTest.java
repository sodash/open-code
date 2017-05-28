package com.winterwell.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.log.KErrorPolicy;

import junit.framework.TestCase;

public class StrUtilsTest extends TestCase {

	@Test
	public void testToCamelCase() {
		Assert.assertEquals("whatTheDickens", StrUtils.toCamelCase("What the 	Dickens "));
		Assert.assertEquals("", StrUtils.toCamelCase(""));
		Assert.assertEquals("f", StrUtils.toCamelCase("f"));
		Assert.assertEquals("xa", StrUtils.toCamelCase("XA"));
	}

	
	@Test
	public void testTrimPunctuation() {
		Assert.assertEquals("hello, world", StrUtils.trimPunctuation("[hello, world]"));
		Assert.assertEquals("hello, world] what", StrUtils.trimPunctuation("[hello, world] what"));
	}
	
	@Test
	public void testEscape() {
		{
			String o = "a@b@@c @ d:e::f :";
			String e = StrUtils.escape(o, '@', ':');
			String p = StrUtils.unescape(e, ':');
			assert p.equals(o) : p;
		}
		{
			String o = "a@b@@c @ d:e::f :";
			String e = StrUtils.escape(o, ':', ':');
			String p = StrUtils.unescape(e, ':');
			assert p.equals(o) : p;
		}
	}
	
	@Test
	public void testIsNumber() {
		assert StrUtils.isNumber("1234.0097");
		assert ! StrUtils.isNumber("1234d");
		assert ! StrUtils.isNumber("1234L");
		assert ! StrUtils.isNumber("1234f");
	}

	@Test
	public void testIsPunctuation() {
		assert StrUtils.isPunctuation("=");
		assert ! StrUtils.isPunctuation("a'b");		
		assert StrUtils.isPunctuation(":");
		assert StrUtils.isPunctuation(")");
		assert StrUtils.isPunctuation("(");
		assert StrUtils.isPunctuation("()");
		assert ! StrUtils.isPunctuation("(a)");
		assert StrUtils.isPunctuation(":)");
		assert ! StrUtils.isPunctuation("don't");
		assert StrUtils.isPunctuation("'!'");
		assert ! StrUtils.isPunctuation("'ق'");
	}

	@Test
	public void testIsSubset() {
		assert StrUtils.isSubset(new String[]{"a"}, new String[]{"a","b"});
		assert StrUtils.isSubset(new String[]{"b"}, new String[]{"a","b"});
		assert StrUtils.isSubset(new String[]{"b", "a"}, new String[]{"a","b"});
		assert ! StrUtils.isSubset(new String[]{"c"}, new String[]{"a","b"});
		assert ! StrUtils.isSubset(new String[]{"a", "c"}, new String[]{"a","b"});
		
		assert StrUtils.isSubset(new String[0], new String[0]);
		assert ! StrUtils.isSubset(new String[]{"a"}, new String[0]);
	}

	@Test
	public void testEndsWith() {
		{
			StringBuilder sb = new StringBuilder("Foo");
			assert ! StrUtils.endsWith(sb, "foo");
			assert StrUtils.endsWith(sb, "Foo");
			assert StrUtils.endsWith(sb, "oo");
			assert StrUtils.endsWith(sb, "o");
			assert ! StrUtils.endsWith(sb, "ooo");
			assert StrUtils.endsWith(sb, "");
		}
		{
			StringBuilder sb = new StringBuilder();
			assert ! StrUtils.endsWith(sb, "foo");
			assert StrUtils.endsWith(sb, "");
		}
	}
	
	@Test
	public void testIsInteger() {
		assert StrUtils.isInteger("12");
		assert StrUtils.isInteger("1");
		assert StrUtils.isInteger("12000000");
		assert StrUtils.isInteger("-1");
		assert StrUtils.isInteger("-123");
		
		assert ! StrUtils.isInteger("foo");
		assert ! StrUtils.isInteger("12_4");
		assert ! StrUtils.isInteger("12_4-1");
		assert ! StrUtils.isInteger("13-1");
	}
	
	/**
	 * For trying out random regexs
	 */
	@Test
	public void testRegexScratch() {
		Pattern WORD = Pattern.compile("\\b!?[a-z\\-]+\\b");
		Matcher m = WORD.matcher("a -boo AND -!none-of-topic");
		assert m.find();
		assert m.group().equals("a");
		assert m.find();
		assert m.group().equals("boo");
		assert m.find();
		assert m.group().equals("none-of-topic") : m.group();
	}
	
	public void testNormaliseSpaces() {
		String s = "no break:\u00A0space:\u0020figure space:\u2007narrow no-break space:\u202Fword joiner:\u2060zero width no-break space:\uFEFF!";
		System.out.println(s);
		String s2 = StrUtils.normalise(s);
		String s3 = StrUtils.compactWhitespace(s);
		assertEquals("no break: space: figure space: narrow no-break space: word joiner:zero width no-break space:!", s2);

		// compactWhitespace doesn't remove zero-width no-break space characters \u2060 and \uFEFF
		assertEquals(s2.length() + 2, s3.length());
	}

	public void testRemovePunctuation() {
		String s = "Hello! yeah; don't @me :(";
		String s2 = StrUtils.removePunctuation(s);
		assert s2.equals("Hello yeah don't me") : s2;
		
		String arabic = "قطار تحرير";
		String a2 = StrUtils.removePunctuation(arabic);
		assert arabic.equals(a2);
		
		{	// With urls
			String m = "Hello! sodash.com winterstein.me.uk,";
			String m2 = StrUtils.removePunctuation(m);
			assert m2.equals("Hello sodash.com winterstein.me.uk") : m;
		}
		{	// With urls
			String m = "https://fooyah.com/what?boo&sucks#baby";
			String m2 = StrUtils.removePunctuation(m);
			assert m2.equals(m) : m2;
		}
	}

	public void testAppendingArabic() {
		// weird bug in dubai csv writing
		String arabic = "قطار تحرير";
		StringBuilder sb = new StringBuilder();
		sb.append("foo bar, ");
		sb.append(arabic);
		sb.append(",1");
		sb.append(",2");
		System.out.println(sb);
		String[] bits = sb.toString().split(",");
		assert bits[0].trim().equals("foo bar");
		assert bits[1].trim().equals(arabic);
		assert bits[2].trim().equals("1");
		assert bits[3].trim().equals("2");
		// The print goes wrong -- but the bits are right?!
	}

	public void testCompactWhitespace() {
		String cmpct = StrUtils
				.compactWhitespace("   hello \n world \r\n \t \t a  b \tc d");
		assert cmpct.equals("hello world a b c d") : cmpct;
	}

	public void testEllipsize() {
		{
			String s = StrUtils.ellipsize("hello world!!!", 0);
			assertEquals("", s);
		}
		{
			String s = StrUtils.ellipsize("hello world!!!", 2);
			assertEquals("", s);
		}
		{
			String s = StrUtils.ellipsize("hello world!!!", 3);
			assertEquals("...", s);
		}
		{
			String s = StrUtils.ellipsize("hello world!!!", 20);
			assertEquals("hello world!!!", s);
		}
		{
			String s = StrUtils.ellipsize("helloworldandstuff!!!", 10);
			assertEquals("hellowo...", s);
		}
		{
			String s = StrUtils.ellipsize("hello world!!!", 10);
			assertEquals("hello...", s);
		}
	}

	public void testExtractHeader() {
		{
			StringBuilder txt = new StringBuilder("Foo: bar\n" + "ABC: 123\n"
					+ "\n" + "The Meat");
			Map<String, String> headers = StrUtils.extractHeader(txt);
			assert headers.size() == 2;
			assert headers.get("foo").equals("bar") : Printer.toString(headers);
			assert headers.get("abc").equals("123") : Printer.toString(headers);
			assert txt.toString().equals("The Meat") : txt;
		}
		{ // Blank string
			StringBuilder txt = new StringBuilder("");
			Map<String, String> headers = StrUtils.extractHeader(txt);
			assert headers.size() == 0;
			assert txt.toString().equals("") : txt;
		}
		{ // No Header
			StringBuilder txt = new StringBuilder("# Hello\nWorld\n");
			Map<String, String> headers = StrUtils.extractHeader(txt);
			assert headers.size() == 0;
			assert txt.toString().equals("# Hello\nWorld\n") : txt;
		}
	}

	public void testFindLenient() {
		{
			Pair<Integer> place = StrUtils
					.findLenient(
							"you're just confident that you can make it work.",
							"Interviewee:	I was definitely optimistic.  It's funny because my partner often, when he talks about our history, he will say we were a little bit stupid, and what he means by that is that you're part optimistic, you're part naïve, and you're just confident that you can make it work.  And if you don't have that, I don't believe you can be successful in trying to start a business because there are so many things that you can't possibly know you're going to be confronted with.  ",
							0);
			assert place != null;
		}
		{ // .s and whitespace?
			Pair<Integer> place = StrUtils.findLenient("Hello\n - world. Yes.",
					"Hello\n - world.\n\r\n Yes. or no.", 0);
			assert place != null;
		}
	}

	public void testIsWord() {
		assert StrUtils.isWord("rabbit");
		assert StrUtils.isWord("Foobar");
		assert !StrUtils.isWord("foo bar");
		assert !StrUtils.isWord(" bollocks");
		assert !StrUtils.isWord("the quick brown fox");
		assert StrUtils.isWord("12");
		// a bit arbitrary!
		assert !StrUtils.isWord("a-b");
		assert !StrUtils.isWord("1,2");
		assert !StrUtils.isWord("1.2");
	}

	public void testIsWordlike() {
		assert StrUtils.isWordlike("rabbit");
		assert StrUtils.isWordlike("Foobar");
		assert !StrUtils.isWordlike("foo bar");
		assert !StrUtils.isWordlike(" bollocks");
		assert !StrUtils.isWordlike("the quick brown fox");
		assert StrUtils.isWordlike("12");
		assert StrUtils.isWordlike("a-b");
		assert StrUtils.isWordlike("1.2");

		// But not this
		assert !StrUtils.isWordlike("1,2");
		assert !StrUtils.isWordlike("John's"); // questionable!
	}

	@Test
	public void testJoin() {
		{
			String joined = StrUtils.join(Arrays.asList("a", "b", "c"), ", ");
			assert joined.equals("a, b, c");
		}
		{
			String joined = StrUtils.join(Arrays.asList("a"), ", ");
			assert joined.equals("a");
		}
	}

	public void testMd5() {
		assert StrUtils.md5("pankHurst").equals(
				"b523c9f59b6a8ffad10158ecb89c658b");
	}

	public void testSha1() {
		assert StrUtils.sha1("pankHurst").equals(
				"914e1ca52f7dd8c59e412521a434e4876202d355");
		System.out.println(StrUtils.sha1("However long the string, we get back only 40 characters -- try it and see :)").length());
	}

	public void testNormalise() {
		{
			String norm = StrUtils.normalise("abc");
			assertEquals("abc", norm);
		}
		{
			String norm = StrUtils.normalise("äbç");
			assertEquals("abc", norm);
		}
		{
			String norm = StrUtils.normalise("“don’t go ‘back there”");
			assertEquals("\"don't go 'back there\"", norm);
		}
		{
			String norm = StrUtils.normalise("donʼt");
			assertEquals("don't", norm);
		}
		if (false) { // This fails -- they're treated as non-letters & discarded
			// by the code that discards umlauts :(
			String norm = StrUtils.normalise("∀∃∅", KErrorPolicy.ACCEPT);
			assertEquals("∀∃∅", norm);
		}
		{
			String norm = StrUtils.normalise("fu-bar_12.3;,'\"+@#?<>[]{}:*!");
			assertEquals("fu-bar_12.3;,'\"+@#?<>[]{}:*!", norm);
		}
		// { // cannot convert to ascii!
		// String norm = StringUtils.normalise("екатерина");
		// assertEquals("ekateriha", norm);
		// }
		{ // Thai
			String norm = StrUtils.normalise("ஓஔரஶ௵௹");
			assert !norm.isEmpty() : norm;
			assert norm.replaceAll("\\?", "").isEmpty() : norm;
		}
		{ // bug
			String norm = StrUtils.normalise(" ١٠٠ ");
			String norm2 = StrUtils.normalise(" ١٠٠ ", KErrorPolicy.ACCEPT);
		}
		{
			String norm2 = StrUtils.normalise("５１４０ｖ", KErrorPolicy.ACCEPT);
			System.out.println(norm2);
		}
	}

	public void testPop() {
		{ // int
			StringBuilder sb = new StringBuilder("hello");
			StrUtils.pop(sb, 1);
			assert sb.toString().equals("hell");
		}
		{ // String no
			StringBuilder sb = new StringBuilder("a");
			StrUtils.pop(sb, "+");
			assert sb.toString().equals("a");
		}
		{ // String no 2
			StringBuilder sb = new StringBuilder(
					"(NOT (twitter OR \"facebook\"))");
			StrUtils.pop(sb, " OR ");
			assert sb.toString().equals("(NOT (twitter OR \"facebook\"))") : sb;
		}
		{ // String yes
			StringBuilder sb = new StringBuilder("a+b+");
			StrUtils.pop(sb, "+");
			assert sb.toString().equals("a+b");
		}
		{ // String yes
			StringBuilder sb = new StringBuilder("a foo ");
			StrUtils.pop(sb, " foo ");
			assert sb.toString().equals("a");
		}
	}

	public void testPunctuationRegex() {
		assert StrUtils.ASCII_PUNCTUATION.matcher("foo.bar").find();
		assert !StrUtils.ASCII_PUNCTUATION.matcher("foobar").find();
		// why we don't use \\W
		// assert ! Pattern.compile("\\W").matcher("äbç").find();
		// assert ! Pattern.compile("\\W").matcher("екатерина").find();
	}

	public void testReplace() {
		final Mutable.Strng removed = new Mutable.Strng();
		IReplace replacer = new IReplace() {
			@Override
			public void appendReplacementTo(StringBuilder sb, Matcher match) {
				removed.value += match.group();
				sb.append(".");
			}
		};
		String text = StrUtils.replace("'Hello - world!'",
				Pattern.compile("\\W+"), replacer);
		assertEquals(".Hello.world.", text);
		assertEquals("' - !'", removed.value);
	}

	public void testSplit() {
		List<String> bits = StrUtils
				.split("E.g. tag1, \"tag 2\" tag-3,tag4   ");
		assert bits.get(0).equals("E.g.");
		assert bits.get(1).equals("tag1");
		assert bits.get(2).equals("tag 2") : bits;
		assert bits.get(3).equals("tag-3");
		assert bits.get(4).equals("tag4");
		assert bits.size() == 5;
	}

	
	public void testSplitCSVStyle() {
		List<String> bits = StrUtils
				.splitCSVStyle("E.g. tag1, \"tag 2\", tag-3,tag4,\"tag, um, 5\"   ");
		assert bits.get(0).equals("E.g. tag1");
		assert bits.get(1).equals("tag 2") : bits.get(1);
		assert bits.get(2).equals("tag-3");
		assert bits.get(3).equals("tag4");
		assert bits.get(4).equals("tag, um, 5");
		assert bits.size() == 5;
	}
	
	public void testSplitOnComma() {
		List<String> bits = StrUtils
				.splitOnComma("E.g. tag1, \"tag 2\", tag-3,tag4,\"tag, um, 5\"   ");
		assert bits.get(0).equals("E.g. tag1");
		assert bits.get(1).equals("\"tag 2\"") : ">"+bits.get(1)+"<";
		assert bits.get(2).equals("tag-3");
		assert bits.get(3).equals("tag4");
		assert bits.get(4).equals("\"tag, um, 5\"");
		assert bits.size() == 5;
	}

	
	public void testSplitWithQuotedQuotes() {
		List<String> bits = StrUtils
				.split("foo  \"\"\"Hello,\"\" she declaimed.\"  bar");
		assert bits.get(0).equals("foo");
		assert bits.get(1).equals("\"Hello,\" she declaimed.");
		assert bits.get(2).equals("bar");

		bits = StrUtils
				.split("foo  \"Hello\"  bar, \"\"\"baz\"\t\"groob\"\"\"");
		assert bits.get(0).equals("foo");
		assert bits.get(1).equals("Hello");
		assert bits.get(2).equals("bar");
		assert bits.get(3).equals("\"baz");
		assert bits.get(4).equals("groob\"");
	}

	public void testSplitBlocks() {
		{ // scratch
			String[] msgs = "hello\n#------ world".split("#----+");
			String[] msgs2 = "hello\n\n#----\n\nworld".split("#----+");
			assert msgs2[1].equals("\n\nworld");
		}
		{
			String s = "hello world\nfoo bar\n\nparagraph 2\n\n\n\n   paragraph 3";
			String[] blocks = StrUtils.splitBlocks(s);
			assert blocks.length == 3;
			assert blocks[0].equals("hello world\nfoo bar");
			assert blocks[1].equals("paragraph 2");
			assert blocks[2].equals("   paragraph 3") : blocks[2];
		}
		{
			String s = "hello world\nfoo bar";
			String[] blocks = StrUtils.splitBlocks(s);
			assert blocks.length == 1;
			assert blocks[0].equals("hello world\nfoo bar");
		}
		{
			String s = "";
			String[] blocks = StrUtils.splitBlocks(s);
			assert blocks.length == 1;
			assert blocks[0].equals("");
		}
	}

	public void testSplitFirst1() {
		String str = "A:B";
		Pair pair = StrUtils.splitFirst(str, ':');
		assert pair.first.equals("A");
		assert pair.second.equals("B");
	}

	public void testSplitFirst2() {
		String str = "A:B";
		assert StrUtils.splitFirst(str, 'x') == null;
	}

	public void testSplitFirst3() {
		String str = "A:";
		Pair pair = StrUtils.splitFirst(str, ':');
		assert pair.first.equals("A");
		assert pair.second.equals("");
	}

	public void testSplitFirst4() {
		String str = ":B";
		Pair pair = StrUtils.splitFirst(str, ':');
		assert pair.first.equals("");
		assert pair.second.equals("B");
	}

	public void testSplitFirst5() {
		String str = "A:B:C";
		Pair pair = StrUtils.splitFirst(str, ':');
		assert pair.first.equals("A");
		assert pair.second.equals("B:C");
	}

	public void testSplitLast() {
		String str = "A:B:C";
		Pair pair = StrUtils.splitLast(str, ':');
		assert pair.first.equals("A:B");
		assert pair.second.equals("C");
	}

	public void testSubstring1() {
		String str = "abcdefghij";// length=10
		assert StrUtils.substring(str, 0, 10).equals(str);
		assert StrUtils.substring(str, 0, 1).equals("a");
		assert StrUtils.substring(str, 0, 100).equals(str);
		assert StrUtils.substring(str, 0, -1).equals("abcdefghi");
		// problems (maybe) when start<end
		assert StrUtils.substring(str, 1, 0).equals("bcdefghij") : StrUtils
				.substring(str, 1, 0);
		// problems for a big negative value for end
		assert StrUtils.substring(str, 0, -100).equals("");
	}

	public void testSubstring2() {
		String str = "";// length=0
		assert StrUtils.substring(str, 0, 10).equals("");
		assert StrUtils.substring(str, 0, 0).equals("");
		// problems for a big negative value for end
		assert StrUtils.substring(str, 0, -100).equals("");
	}

	public void testToCanonical() {
		{
			assertEquals("", StrUtils.toCanonical(null));
			assertEquals("", StrUtils.toCanonical(""));
			assertEquals("", StrUtils.toCanonical("   "));
			assertEquals("fu bar", StrUtils.toCanonical(" Fu Bar\n"));
		}
		{
			assertEquals("mossoro rn brasil",
					StrUtils.toCanonical("Mossoró-RN/Brasil"));
		}
		{
			String s = StrUtils.toCanonical("	_Hello - World!!!\r\n");
			assertEquals("hello world", s);
		}
		{
			String s = StrUtils.toCanonical("çärumbä");
			assertEquals("carumba", s);
		}
		{ // dubai
			String s = StrUtils.toCanonical("دبيّ‎");
			// Hm - this changes a bit -- but possibly in a correct manner
			assertEquals("دبي", s);
		}
		{
			String s = StrUtils.toCanonical("\"I say!\" said Alice.");
			assertEquals("i say said alice", s);
		}
		if (false) { // The hearts are not letters, so they get dropped
			String s = "​♡​♡​♡";
			assert StrUtils.toCanonical(s).equals(s) : StrUtils.toCanonical(s); 
		}
	}

	public void testToCleanLinux() {
		{ // blank line
			Matcher m = StrUtils.BLANK_LINE.matcher("Hello\nWorld\n");
			assert !m.find();
			m = StrUtils.BLANK_LINE.matcher("Hello\n \nWorld\n");
			assert m.find();
			assert m.group().equals(" ") : m.group();
		}
		{
			String txt = StrUtils
					.toCleanLinux("Hello World\nYes\n   \n\twhatever");
			assertEquals("Hello World\nYes\n\n\twhatever", txt);
		}
		{ // Windows
			String txt = StrUtils
					.toCleanLinux("Hello World\r\nYes\r\n   \r\n\twhatever\r\n");
			assertEquals("Hello World\nYes\n\n\twhatever\n", txt);
		}
		{ // Mac
			String txt = StrUtils
					.toCleanLinux("Hello World\rYes\r   \r\twhatever\r");
			assertEquals("Hello World\nYes\n\n\twhatever\n", txt);
		}

	}

	public void testToNSigFigs() {
		{
			String s = StrUtils.toNSigFigs(2, 1);
			assert s.equals("2") : s;
		}
		{
			String s = StrUtils.toNSigFigs(1.002, 1);
			assert s.equals("1") : s;
		}
		{
			String s = StrUtils.toNSigFigs(1001, 1);
			assert s.equals("1000") : s;
		}
		{
			String s = StrUtils.toNSigFigs(1801, 1);
			assert s.equals("2000") : s;
		}
		{
			String s = StrUtils.toNSigFigs(-1801, 1);
			assert s.equals("-2000") : s;
		}
		{
			String s = StrUtils.toNSigFigs(0.209, 2);
			assert s.equals("0.21") : s;
		}
		{
			String s = StrUtils.toNSigFigs(-0.209, 2);
			assert s.equals("-0.21") : s;
		}
		{ // don't do the E notation
			String s = StrUtils.toNSigFigs(-0.0009881, 2);
			assert s.equals("-0.00099") : s;
		}
		{
			String s = StrUtils.toNSigFigs(-0.9881, 1);
			assert s.equals("-1") : s;
		}
		{
			String s = StrUtils.toNSigFigs(-0.0009881, 1);
			assert s.equals("-0.001") : s;
		}
	}

	public void testToTitleCase1() {
		assert StrUtils.toTitleCase("daniel").equals("Daniel");
		assert StrUtils.toTitleCase("a new string").equals("A New String");
		assert StrUtils.toTitleCase("").equals("");
		assert StrUtils.toTitleCase("Daniel").equals("Daniel");
		assert StrUtils.toTitleCase("shift's").equals("Shift's") : StrUtils
				.toTitleCase("shift's");
	}

	public void testToTitleCasePlus() {
		assertEquals(StrUtils.toTitleCasePlus("hog_wash"), "Hog Wash");
		assertEquals(StrUtils.toTitleCasePlus("daniel"), "Daniel");
		assertEquals(StrUtils.toTitleCasePlus("a new string"), "A New String");
		assertEquals(StrUtils.toTitleCasePlus(""), "");
		assertEquals(StrUtils.toTitleCasePlus("Daniel"), "Daniel");
		assertEquals(StrUtils.toTitleCasePlus("spoonMcGuffin"),
				"Spoon Mc Guffin");
	}

	public void testGetTermsBetweenChars() {
		List<String> t1 = StrUtils
				.getTermsBetweenChars(
						"\"Ben\" and \"Jane\" \"know\" each other quite well. They like to go for \"walks\" in \"the park\"",
						'"', 0);
		assert t1.contains("Ben");
		assert t1.contains("Jane");
		assert t1.contains("know");
		assert t1.contains("the park");
	}

	public void testTabulation() {
		String lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque vel velit diam, a feugiat lectus. Mauris nec ante mi. Cras odio dolor, porta vitae posuere a, rutrum a ante. Pellentesque at augue sed velit sollicitudin pulvinar. Aliquam erat volutpat. Mauris risus diam, gravida sit amet congue sed, cursus non quam. Suspendisse sed quam ante, nec venenatis neque. In hac habitasse platea dictumst. Fusce ante eros, vulputate sed hendrerit vitae, molestie eu sapien.";
		List<List<? extends Object>> data = new ArrayList<List<? extends Object>>(
				10);
		data.add(Arrays.asList("ID", "First name", "Last name", "Lorem ipsum"));
		data.add(Arrays.asList(123, "Joe", "Halliwell", lorem));
		data.add(Arrays.asList(456, "Dan", "Winterstein", lorem));
		data.add(Arrays.asList(789, "Agis", "Chartsias", lorem));
		data.add(Arrays.asList(163, "Mirren", "Fischer", lorem));
		data.add(Arrays.asList(7631, "Alex", "Nuttgens", lorem));
		String table = StrUtils.tabulate(data);
		System.out.println(table);
	}

	public void testCountSubStrings(){
		assert StrUtils.countSubStrings("Bread and butter and cheese", "and") == 2;
		assert StrUtils.countSubStrings("Bread and butter and cheese", "guacamole") == 0;
		assert StrUtils.countSubStrings("aaaabaaaa", "a") == 8;
		assert StrUtils.countSubStrings("ndnndnndnndn", "nndnn") == 2;
	}
	
}
