//package com.winterwell.nlp.languages;
//
//import java.util.List;
//
//import org.junit.Test;
//
//import com.winterwell.maths.stats.distributions.cond.ISitnStream;
//import com.winterwell.maths.stats.distributions.cond.Sitn;
//import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
//import com.winterwell.nlp.corpus.SimpleDocument;
//import com.winterwell.utils.Printer;
//import com.winterwell.utils.containers.Containers;
//
//public class LangClassifierTest {
//
//	@Test
//	public void testFrench() {
//		// this should be easy!
//		LangClassifier lc = new LangClassifier("en", "fr");
//		{
//			IFiniteDistribution<String> d = lc.pClassify(new SimpleDocument(
//					"hello to the world"));
//			assert d.getMostLikely().equals("en");
//		}
//		{
//			IFiniteDistribution<String> d = lc.pClassify(new SimpleDocument(
//					"bonjour a le monde"));
//			assert d.getMostLikely().equals("fr");
//		}
//		{
//			IFiniteDistribution<String> d = lc.pClassify(new SimpleDocument(
//					"chat"));
//			Printer.out(d);
//		}
//	}
//
//	@Test
//	public void testHebrew() {
//		// this should be easy!
//		LangClassifier lc = new LangClassifier("en", "he");
//		{
//			IFiniteDistribution<String> d = lc.pClassify(new SimpleDocument(
//					"hello to the world"));
//			assert d.getMostLikely().equals("en");
//		}
//		{
//			IFiniteDistribution<String> d = lc.pClassify(new SimpleDocument(
//					"של"));
//			assert d.getMostLikely().equals("he");
//		}
//	}
//
//	@Test
//	public void testShingling() {
//		ShinglesStream shingliser = new ShinglesStream(3, "");
//		{
//			for (char c : new char[] { '0', 'a', '!', '<', '.', ' ', '@', 'ש',
//					'ر', 'ó', 'ç', 'ä' }) {
//				System.out.println(c + ": " + Character.isLetter(c));
//			}
//		}
//		{
//			ISitnStream<String> shingles = shingliser.factory("Hello World!");
//			List<Sitn<String>> list = Containers.getList(shingles);
//			System.out.println(list);
//		}
//		{
//			ISitnStream<String> shingles = shingliser
//					.factory("<p>Hello World!!   Woot!</p>\r\n");
//			List<Sitn<String>> list = Containers.getList(shingles);
//			System.out.println(list);
//		}
//		{
//			ISitnStream<String> shingles = shingliser.factory("don't.a<br>b");
//			List<Sitn<String>> list = Containers.getList(shingles);
//			System.out.println(list);
//		}
//	}
//
//}
