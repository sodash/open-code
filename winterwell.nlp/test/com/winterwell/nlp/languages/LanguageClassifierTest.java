//package com.winterwell.nlp.languages;
//
//import org.junit.Test;
//
//import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
//import com.winterwell.nlp.corpus.SimpleDocument;
//import com.winterwell.utils.Printer;
//
//public class LanguageClassifierTest {
//
//	@Test
//	public void testFrench() {
//		// this should be easy!
//		LanguageClassifier lc = new LanguageClassifier("en", "fr");
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
//		LanguageClassifier lc = new LanguageClassifier("en", "he");
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
//}
