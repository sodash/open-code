package com.winterwell.nlp.io;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.nlp.dict.Dictionary;
import com.winterwell.nlp.dict.EmoticonDictionary;
import com.winterwell.nlp.io.FilteredTokenStream.KInOut;
import com.winterwell.utils.log.KErrorPolicy;

public class FilteredTokenStreamTest {

	@Test
	public void testLoopingBug() {
		// long/infinite loop on FilteredTokenStream <-
		// WordAndPunctuationTokeniser
		// [urlsAsWords=true, swallowPunctuation=true, splitOnApostrophe=false,
		// lowerCase=true]
		// Without communication there is no relationship; without respect there
		// is no love; without trust there is no reason to continue
		// get words and urls, lose punctuation
		// - except for # and @ which are special and can start words
		WordAndPunctuationTokeniser t = new WordAndPunctuationTokeniser.TweetSpeak();
		t.setNormaliseToAscii(KErrorPolicy.ACCEPT);
		t.setSwallowPunctuation(true);
		t.setSplitOnApostrophe(false);
		t.setLowerCase(true);

		// keep in emoticons and have phrases as single-tokens
		// Also keeps in ?s for detecting questions, !!s for stronger emotion,
		// and !?, ?! etc
		// TODO more phrases eg "how much"
		Dictionary dict = new Dictionary(
				NLPWorkshop.get().getFile("phrases.txt"), '\t');
		t.addDictionary(dict, false);
		t.addDictionary(EmoticonDictionary.getDefault(), false); // Not for testing -
															// makes sentiment
															// too easy
		ITokenStream _tokeniser = t;

		// TODO handle "not good" ?? leave not in? or have not-good as one
		// token?
		// How about "It's not David Cameron's fault"??
		// Note if so: "not a good idea" should be "not-good", "idea" rather
		// than "not-a" "good" "idea"
		// what about other -ives: never ??map all negatives to "neg"
		// should we apply not to all adjectives (but fault in the eg above is a
		// noun) hm?

		// filter out stopwords
		// String txt = FileUtils.read(new
		// NLPWorkshop().getFile("stopwords.txt"));
		// HashSet<String> stopwords = new
		// HashSet<String>(Arrays.asList(StringUtils.splitLines(txt)));
		Set<String> stopwords = new HashSet<String>(
				NLPWorkshop.get().getStopwords());
		// Let question words and +ive/-ive words through
		stopwords.remove("what");
		stopwords.remove("how");
		stopwords.remove("where");
		stopwords.remove("when");
		stopwords.remove("why");
		stopwords.remove("?");

		stopwords.remove("yes");
		stopwords.remove("yeah");
		stopwords.remove("no");
		stopwords.remove("not");
		stopwords.remove("never");
		_tokeniser = new FilteredTokenStream(stopwords, KInOut.EXCLUDE_THESE,
				_tokeniser);

		// break up urls into host, path (doesn't cope with bitly)
		_tokeniser = new UrlBreaker(_tokeniser);

		// stem (does not stem #tags @mentions or links)
		StemmerFilter t3 = new StemmerFilter(_tokeniser);
		// t3.getStemmer().setDictionaryStems(true); // not needed - there is a
		// different tokeniiser for display
		_tokeniser = t3;
		ITokenStream tokeniser = _tokeniser;

		tokeniser = tokeniser
				.factory("Without communication there is no relationship; without respect there is no love; without trust there is no reason to continue");
		List<Tkn> tokens = ATSTestUtils.getTokens(tokeniser);
		System.out.println(tokens);
	}

	@Test
	public void testStopwords() {
		{
			WordAndPunctuationTokeniser wpt = new WordAndPunctuationTokeniser();
			List<String> stopwords = Arrays.asList("hello");
			FilteredTokenStream filter = new FilteredTokenStream(stopwords,
					KInOut.EXCLUDE_THESE, wpt);
			ITokenStream tokens = filter.factory("hello world");
			Iterator<Tkn> it = tokens.iterator();
			Tkn token1 = it.next();
			assert token1.getText().equals("world") : token1;
			assert ! it.hasNext() : tokens;
		}
		{
			WordAndPunctuationTokeniser wpt = new WordAndPunctuationTokeniser();
			List<String> stopwords = Arrays.asList("hello");
			FilteredTokenStream filter = new FilteredTokenStream(stopwords,
					KInOut.EXCLUDE_THESE, wpt);
			ITokenStream tokens = filter.factory("");
			assert ! tokens.iterator().hasNext() : tokens;
		}
	}
}
