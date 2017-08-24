/**
 * 
 */
package com.winterwell.nlp.io;

import java.util.List;

import org.junit.Test;

import com.winterwell.nlp.dict.Dictionary;
import com.winterwell.nlp.dict.EmoticonDictionary;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser.TweetSpeak;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.KErrorPolicy;

/**
 * @author Daniel
 * 
 */
public class WordAndPunctuationTokeniserTest {

	@Test
	public void test2Dictionaries() {
		WordAndPunctuationTokeniser tk = new WordAndPunctuationTokeniser();
		tk.setSwallowPunctuation(true);
		Dictionary dict1 = new Dictionary(new ArrayMap("F.U.B.A.R.",
				"fucked up")) {
			@Override
			protected String toCanonical(String word) {
				return word.trim();
			}
		};
		Dictionary dict2 = new Dictionary(new ArrayMap("rhubarb rhubarb",
				"blah blah"));
		tk.addDictionary(dict1, false);
		tk.addDictionary(dict2, false);
		{
			WordAndPunctuationTokeniser tokens = tk.factory("hello F.U.B.A.R.");
			ATSTestUtils.assertTokenisation(tokens, "hello F.U.B.A.R.");
		}
		{
			WordAndPunctuationTokeniser tokens = tk
					.factory("rhubarb and rhubarb rhubarb");
			ATSTestUtils.assertMatches(tokens, "rhubarb", "and",
					"rhubarb rhubarb");
		}
	}

	// @Test TODO
	public void testAbbreviation() {
		{
			String input = "O.K.";
			ITokenStream t = new WordAndPunctuationTokeniser(input);
			ATSTestUtils.assertTokenisation(t, "O.K.");
		}
		{
			String input = "The U.K. rules O.K.!";
			ITokenStream t = new WordAndPunctuationTokeniser(input);
			ATSTestUtils.assertTokenisation(t, "The U.K. rules O.K. !");
		}
	}


	
	/**
	 * TODO What is the "correct"/desired behaviour?
	 */
	@Test
	public void testApostrophe() {
		{
			String input = "Don't, won't can't";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setSplitOnApostrophe(false);
			ATSTestUtils.assertTokenisation(t, "Don't , won't can't");
		}
		{
			String input = "'x'";
			ITokenStream t = new WordAndPunctuationTokeniser(input);
			ATSTestUtils.assertTokenisation(t, "' x '");
		}
		{
			String input = "monkey's bananas";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setSplitOnApostrophe(false);
			ATSTestUtils.assertTokenisation(t, "monkey's bananas");
		}
		{
			String input = "monkeys' bananas";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setSplitOnApostrophe(false);
			ATSTestUtils.assertTokenisation(t, "monkeys' bananas");
		}
		{
			String input = "'elp!";
			ITokenStream t = new WordAndPunctuationTokeniser(input);
			ATSTestUtils.assertTokenisation(t, "' elp !");
		}
		{
			String input = "You can't you won't & you don't stop";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setSwallowPunctuation(false);
			ATSTestUtils.assertTokenisation(t,
					"You can ' t you won ' t & you don ' t stop");
		}
		{ // with swallowing
			String input = "You can't you won't & you don't stop";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setSwallowPunctuation(true);
			ATSTestUtils.assertTokenisation(t,
					"You cant you wont you dont stop");
		}
	}

	@Test
	public void testDictionary() {
		WordAndPunctuationTokeniser tk = new WordAndPunctuationTokeniser();
		tk.setSwallowPunctuation(true);
		EmoticonDictionary dict = EmoticonDictionary.getDefault();
		tk.addDictionary(dict, false);
		tk.setLowerCase(true);
		{	// test factory
			WordAndPunctuationTokeniser t2 = tk.factory("hello :) %&");
			assert t2.getDictionaries().length == 1 : Printer.toString(t2.getDictionaries());
			assert t2.getDictionaries()[0] == dict : t2.getDictionaries()[0];
		}
		{ // test in use
			WordAndPunctuationTokeniser tokens = tk.factory("hello :) %&");
			ATSTestUtils.assertTokenisation(tokens, "hello :)");
		}
		{ // test in use
			WordAndPunctuationTokeniser tokens = tk
					.factory("Farewell, and adieu :) (does it work?)");
			ATSTestUtils.assertTokenisation(tokens,
					"farewell and adieu :) does it work");
		}
		
		{ // TweetSpeak
			TweetSpeak base = new WordAndPunctuationTokeniser.TweetSpeak();
			WordAndPunctuationTokeniser tokens = base
					.factory("#good :) yes");
			ATSTestUtils.assertTokenisation(tokens,
					"#good :) yes");
		}
	}

	/**
	 * What happens to non-ASCII words?
	 */
	@Test
	public void testForeignLingo() {
		WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser();
		t.setUrlsAsWords(true);
		t.setSwallowPunctuation(true);
		t.setSplitOnApostrophe(false);
		t.setLowerCase(true);

		WordAndPunctuationTokeniser streamRusky = t
				.factory("RT @metdim: Меня в сутки фолловят в среднем 10 твиплов из них 6 ботов СЕОшиников и 4 живых человека. #ru_ff живые люди follow me :)");
		String target = "rt metdim Меня в сутки фолловят в среднем 10 твиплов из них 6 ботов СЕОшиников и 4 живых человека ruff живые люди follow me"
				.toLowerCase();
		ATSTestUtils.assertTokenisation(streamRusky, target);

		// well perhaps something I could tokenise at all would be a better test
		WordAndPunctuationTokeniser stream = t
				.factory("「全世界で600万本を突破『マリオ＆ソニック AT バンクーバーオリンピック』」。皆さん、意外と雪上スポーツゲー、好きですよね。どこの国が一番、売れてるのかなぁ？");
		Printer.out(Containers.getList(stream));
		// ATSTestUtils.assertTokenisation(stream,
		// "全世界で600万本を突破 マリオ＆ソニック AT バンクーバーオリンピック 」。皆さん、意外と雪上スポーツゲー、好きですよね。どこの国が一番、売れてるのかなぁ");
	}

	@Test
	public void testHashTags() {
		{ // test with an overridden method (and hence an anonymous class)
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser() {
				@Override
				protected boolean isWordStart(char c) {
					return super.isWordStart(c) || c == '#';
				}
			};
			t.setSwallowPunctuation(true);
			WordAndPunctuationTokeniser t2 = t.factory("#a @b #c!");
			ATSTestUtils.assertTokenisation(t2, "#a b #c");
		}
	}

	/**
	 * TODO What is the "correct"/desired behaviour?
	 */
	@Test
	public void testHyphenation() {
		{
			String input = "monkey-business";
			ITokenStream t = new WordAndPunctuationTokeniser(input);
			ATSTestUtils.assertTokenisation(t, "monkey - business");
		}
		{
			String input = "monkey_business";
			ITokenStream t = new WordAndPunctuationTokeniser(input);
			ATSTestUtils.assertTokenisation(t, "monkey_business");
		}
	}

	@Test
	public void testInstantiate() {
		{
			String input = "http://whatever.com/yes";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setUrlsAsWords(true);
			t.setSwallowPunctuation(true);
			WordAndPunctuationTokeniser t2 = t.factory("http://test1.com , a");
			ATSTestUtils.assertTokenisation(t2, "http://test1.com a");
		}
		{
			String input = "!!!";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setUrlsAsWords(false);
			t.setSwallowPunctuation(false);
			WordAndPunctuationTokeniser t2 = t.factory("http://test1.com , a");
			ATSTestUtils.assertTokenisation(t2, "http :// test1 . com , a");
		}
		{ // test with an overridden method (and hence an anonymous class)
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser() {
				@Override
				protected boolean isWordStart(char c) {
					return super.isWordStart(c) || c == '#';
				}
			};
			WordAndPunctuationTokeniser t2 = t.factory("#a @b");
			ATSTestUtils.assertTokenisation(t2, "#a @ b");
		}
	}

	/**
	 * Tests an infinite loop bug from Oct 12th 2011
	 */
	@Test
	public void testLoopingBug() {
		WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser.TweetSpeak();
		t.setNormaliseToAscii(KErrorPolicy.ACCEPT);
		t.setSwallowPunctuation(true);
		t.setSplitOnApostrophe(false);
		t.setLowerCase(true);

		WordAndPunctuationTokeniser tokens = t
				.factory("Without communication there is no relationship; without respect there is no love; without trust there is no reason to continue");
		List<Tkn> toks = ATSTestUtils.getTokens(tokens);
		System.out.println(toks);
	}

	@Test
	public void testNumber() {
		{
			String input = "27.2 + 100.00 = £1,000.01";
			ITokenStream t = new WordAndPunctuationTokeniser(input);
			ATSTestUtils.assertTokenisation(t, "27.2 + 100.00 = £ 1,000.01");
		}
		{
			String input = "1";
			ITokenStream t = new WordAndPunctuationTokeniser(input);
			ATSTestUtils.assertTokenisation(t, "1");
		}
		{
			String input = "1. 2, 33!";
			ITokenStream t = new WordAndPunctuationTokeniser(input);
			ATSTestUtils.assertTokenisation(t, "1 . 2 , 33 !");
		}

	}

	@Test
	public void testSimple() {
		{
			String input = "This is a test";
			ITokenStream t = new WordAndPunctuationTokeniser(input);
			ATSTestUtils.assertTokenisation(t, "This is a test");
		}
		{
			String input = "This is   A test!";
			ITokenStream t = new WordAndPunctuationTokeniser(input);
			ATSTestUtils.assertTokenisation(t, "This is A test !");
		}
		{
			String input = "x";
			ITokenStream t = new WordAndPunctuationTokeniser(input);
			ATSTestUtils.assertTokenisation(t, "x");
		}
		{
			String input = "";
			ITokenStream t = new WordAndPunctuationTokeniser(input);
			assert ! t.iterator().hasNext();
			List<Tkn> tokens = ATSTestUtils.getTokens(t);
			assert tokens.size() == 0;
		}
		{
			String input = "   What! Some punctuation? Yes!!Hoorah";
			ITokenStream t = new WordAndPunctuationTokeniser(input);
			ATSTestUtils.assertTokenisation(t,
					"What ! Some punctuation ? Yes !! Hoorah");
		}
		{
			String input = "test with\n linebreaks";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			ATSTestUtils.assertTokenisation(t, input);
		}
	}

	@Test
	public void testSwallowPunctuation() {
		{
			String input = "This is a test.";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setSwallowPunctuation(true);
			ATSTestUtils.assertTokenisation(t, "This is a test");
		}
		{
			String input = "x";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setSwallowPunctuation(true);
			ATSTestUtils.assertTokenisation(t, "x");
		}
		{
			String input = ".";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setSwallowPunctuation(true);
			assert ! t.iterator().hasNext();
			List<Tkn> tokens = ATSTestUtils.getTokens(t);
			assert tokens.size() == 0;
		}
		{
			String input = "!!!  !";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setSwallowPunctuation(true);
			assert ! t.iterator().hasNext();
			List<Tkn> tokens = ATSTestUtils.getTokens(t);
			assert tokens.size() == 0;
		}
		{
			String input = "   What! Some punctuation? Yes!!Hoorah";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setSwallowPunctuation(true);
			ATSTestUtils.assertTokenisation(t,
					"What Some punctuation Yes Hoorah");
		}
		{
			String input = "isn't 'this' the dogs' bollocks?";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setSwallowPunctuation(true);
			ATSTestUtils.assertTokenisation(t, "isnt this the dogs bollocks");
		}
		// { // TODO protect numbers
		// String input = "1,000 + 100.01.";
		// WordAndPunctuationTokeniser t = new
		// WordAndPunctuationTokeniser(input);
		// t.setSwallowPunctuation(true);
		// ATSTestUtils.assertTokenisation(t, "1000 100.01");
		// }
	}

	@Test
	public void testUrls() {
		{
			String input = "http://whatever.com/yes";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setUrlsAsWords(true);
			ATSTestUtils.assertTokenisation(t, input);
		}
		{
			String input = "test http://whatever.com/yes no";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setUrlsAsWords(true);
			ATSTestUtils.assertTokenisation(t, input);
		}
		{
			String input = "http://foo.com/bar.js?x=1.1&y=%27x";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setUrlsAsWords(true);
			ATSTestUtils.assertTokenisation(t, input);
		}
		{
			String input = "test without\n -- any  urls";
			WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser(
					input);
			t.setUrlsAsWords(true);
			ATSTestUtils.assertTokenisation(t, input);
		}

	}
}
