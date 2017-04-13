//package com.winterwell.nlp.languages;
//
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.logging.Level;
//
//import com.mzsanford.cld.CompactLanguageDetector;
//import com.winterwell.depot.Depot;
//import com.winterwell.depot.Desc;
//
//import com.winterwell.maths.stats.distributions.cond.Cntxt;
//import com.winterwell.maths.stats.distributions.cond.ICondDistribution;
//import com.winterwell.maths.stats.distributions.cond.ISitnStream;
//import com.winterwell.maths.stats.distributions.cond.Sitn;
//import com.winterwell.maths.stats.distributions.cond.UnConditional;
//import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
//import com.winterwell.nlp.NLPWorkshop;
//import com.winterwell.nlp.classifier.StreamClassifier;
//import com.winterwell.nlp.corpus.IDocument;
//import com.winterwell.nlp.corpus.wikipedia.WikipediaCorpus;
//import com.winterwell.utils.Key;
//import com.winterwell.utils.containers.AbstractIterator;
//import com.winterwell.utils.log.Log;
//import com.winterwell.utils.web.WebUtils;
//
///**
// * @deprecated Use Google's code instead -- there's an open-source project
// * for this. {@link CompactLanguageDetector}
// * 
// * @testedby {@link LangClassifierTest}
// * @author daniel
// *
// */
//public class LangClassifier extends StreamClassifier<String> {
//
//	static final ISO639 iso = new ISO639();
//
//	private static Map<String, ICondDistribution<String, Cntxt>> getModels(
//			String[] languages) {
//		Map<String, ICondDistribution<String, Cntxt>> isoCode2lang = new HashMap();
//		for (String lang : languages) {
//			lang = iso.getCode(lang);
//			ObjectDistribution m = new ObjectDistribution();
//			// a moderately generous pseudo count
//			m.setPseudoCount(10);
//			ICondDistribution<String, Cntxt> cm = new UnConditional(
//					m);
//			isoCode2lang.put(lang, cm);
//		}
//		return isoCode2lang;
//	}
//
//	ISitnStream<String> tokeniser;
//
//	public LangClassifier(String... languages) {
//		super(new ShinglesStream(3, ""), getModels(languages));
//		trainWithWikipedia();
//	}
//
//	private void trainWithWikipedia() {
//		int cnt = 0;
//		Depot depot = Depot.getDefault();
//		for (String lang : getTags()) {
//			NLPWorkshop nlp = NLPWorkshop.get(lang);
//			// load if we can
//			Desc<ICondDistribution> desc = new Desc<ICondDistribution>(
//					"LangClassifier_model", ICondDistribution.class);
//			desc.setTag("winterwell.nlp/" + lang);
//			desc.put(new Key("lang"), lang);
//			ICondDistribution model = depot.get(desc);
//			if (model != null) {
//				setModel(lang, model);
//				continue;
//			}
//
//			// Train from Wikipedia
//			Log.report("nlp", "Training " + getClass().getSimpleName()
//					+ " for " + lang + " from Wikipedia", Level.FINE);
//			WikipediaCorpus corpus = new WikipediaCorpus(nlp);
//			for (IDocument doc : corpus) {
//				train1(doc, lang);
//				cnt++;
//				if (cnt % 1000 == 0) {
//					Log.i("ai.train", "	..." + cnt + " documents");
//				}
//			}
//
//			// save
//			model = getModel(lang);
//			depot.put(desc, model);
//		}
//
//		// TODO set the prior to neutral??
//	}
//
//}
//
///**
// * Shingles - a dread disease, or a set of subsequences used for simple NLP.
// * 
// * @author daniel
// */
//final class ShinglesStream implements ISitnStream<String> {
//
//	private String input;
//
//	private int len;
//
//	public ShinglesStream(int len, String input) {
//		this.len = len;
//		// remove any (sort of English) xml tags
//		this.input = WebUtils.stripTags(input);
//	}
//
//	@Override
//	public ISitnStream<String> factory(Object sourceSpecifier) {
//		return new ShinglesStream(len, sourceSpecifier.toString());
//	}
//
//	@Override
//	public String[] getContextSignature() {
//		return Cntxt.EMPTY.getSignature();
//	}
//
//	@Override
//	public Collection<Class> getFactoryTypes() {
//		return (Collection) Collections.singleton(String.class);
//	}
//
//	@Override
//	public AbstractIterator<Sitn<String>> iterator() {
//		return new AbstractIterator<Sitn<String>>() {
//			int i = 0;
//			StringBuilder shingle = new StringBuilder();
//			private boolean whitespace;
//
//			/**
//			 * create a sitn for each letter substring of length n
//			 */
//			@Override
//			protected Sitn<String> next2() throws Exception {
//				// get the next character
//				char c = 0;
//				while (true) {
//					if (i == input.length())
//						return null;
//					c = input.charAt(i);
//					i++;
//
//					// Treat non-letters as whitespace
//					if (!Character.isLetter(c)) {
//						c = ' ';
//					}
//
//					// compact whitespace
//					if (Character.isWhitespace(c)) {
//						if (whitespace) {
//							continue;
//						}
//						whitespace = true;
//						break;
//					}
//
//					whitespace = false;
//					break;
//				}
//
//				if (shingle.length() == len) {
//					shingle.deleteCharAt(0);
//				}
//
//				// lowercase
//				c = Character.toLowerCase(c);
//
//				shingle.append(c);
//				Sitn<String> sitn = new Sitn<String>(shingle.toString(),
//						Cntxt.EMPTY);
//				return sitn;
//			}
//		};
//	}
//
//}
