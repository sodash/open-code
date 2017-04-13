//package com.winterwell.nlp.languages;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import com.mzsanford.cld.CompactLanguageDetector;
//
//import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
//import com.winterwell.nlp.NLPWorkshop;
//import com.winterwell.nlp.classifier.ITextClassifier;
//import com.winterwell.nlp.corpus.IDocument;
//import com.winterwell.nlp.io.ITokenStream;
//import com.winterwell.nlp.io.Tkn;
//import com.winterwell.nlp.io.WordAndPunctuationTokeniser;
//import com.winterwell.utils.StrUtils;
//import com.winterwell.utils.containers.ArrayMap;
//
///**
// * @deprecated Use Google's code instead -- there's an open-source project
// * for this. {@link CompactLanguageDetector}
// * 
// * A crude thing that relies on stopword lists.
// * 
// * @author daniel
// * @testedby {@link LanguageClassifierTest}
// */
//public class LanguageClassifier implements ITextClassifier<String> {
//
//	Map<String, Set<String>> lang2stopwords = new ArrayMap();
//
//	WordAndPunctuationTokeniser tokeniser;
//
//	/**
//	 * A crude thing that relies on stopword lists.
//	 * @param languages
//	 */
//	public LanguageClassifier(String... languages) {
//		for (String lang : languages) {
//			NLPWorkshop nlp = NLPWorkshop.get(lang);
//			lang2stopwords.put(lang, nlp.getStopwords());
//		}
//		tokeniser = new WordAndPunctuationTokeniser();
//		tokeniser.setNormaliseToAscii(null); // keep the weird accents!
//		tokeniser.setSwallowPunctuation(true);
//		tokeniser.setLowerCase(true);
//	}
//
//	@Override
//	public String classify(IDocument text) {
//		return pClassify(text).getMostLikely();
//	}
//
//	@Override
//	public void finishTraining() {
//	}
//
//	@Override
//	public boolean isReady() {
//		return true;
//	}
//
//	@Override
//	public ObjectDistribution<String> pClassify(IDocument text) {
//		ObjectDistribution<String> dist = new ObjectDistribution<String>();
//		ITokenStream tks = tokeniser.factory(text.getContents());
//		List<Tkn> words = tks.toList();
//		for (String lang : lang2stopwords.keySet()) {
//			Set<String> stopwords = lang2stopwords.get(lang);
//			int cnt = 0;
//			for (Tkn tk : words) {
//				if (StrUtils.isNumber(tk.getText())) {
//					continue;
//				}
//				if (stopwords.contains(tk.getText())) {
//					cnt++;
//				}
//			}
//			dist.setProb(lang, cnt);
//		}
//		// tiny tie-breaker bias towards English
//		if (lang2stopwords.containsKey("en")) {
//			dist.addProb("en", 0.9);
//		}
//		return dist;
//	}
//
//	@Override
//	public void resetup() {
//	}
//
//	@Override
//	public void train1(IDocument x, String tag) {
//		throw new UnsupportedOperationException();
//	}
//
//}
