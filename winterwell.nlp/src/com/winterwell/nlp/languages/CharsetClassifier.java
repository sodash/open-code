package com.winterwell.nlp.languages;

import java.util.HashMap;
import java.util.Map;

import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.nlp.classifier.ITextClassifier;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.IntRange;

/**
 * Identify the language by the charset used. This is a quick and effective way
 * of picking out e.g. arabic, or hebrew. But less good for European languages.
 * <p>
 * There are lots of reasons why this will make mistakes. But it has it's place.
 * 
 * WARNING: if using with web pages, you should ALWAYS strip tags first.
 * 
 * @warning Assumes ASCII = English!
 * 
 * 
 * @author daniel
 * @testedby  CharsetClassifierTest}
 */
public class CharsetClassifier implements ITextClassifier<String> {

	static final Map<String, String> charsetInfo = new ArrayMap(
			// European
			"English", 	"41 - 7A", // BOGUS: assume ascii = English!!!
			"Arabic",	"0600 - 06FF", // Warning: urdu uses arabic!
			"Hebrew",	"0590 - 05FF",
			// "Ethipoic", "1200 - 137F", // dead language
			// "Balinese", "1B00 - 1B7F", // this minority Indonesian language
			// does not have an ISO 639-1 code :(
			ISO639.CJK_CODE, "4E00 - 9FCF", 
			"Korean (hangul)", "AC00 - D7AF",
			"Japanese (katakana)", 	"30A0 - 30FF", 
			"Japanese (hiragana)",	"3040 - 309F", 
			"Javanese", "A980 - A9DF", 
			"Gujarati",	"0A80 - 0AFF", 
			"Tagalog", "1700 - 171F", 
			"Thai", "0E00 - 0E7F",
			"Greek", "0370 - 03FF", 
			"Russian", "0400 - 04FF" // assume Cyrillic = Russian!
	);

	/**
	 * range2ISOcode. charsetInfo is "loaded" into here
	 */
	final Map<IntRange, String> charsetInfo2 = new HashMap();

	public CharsetClassifier() {
		ISO639 isoLang = new ISO639();
		// "load" the data
		for (String key : charsetInfo.keySet()) {
			String v = charsetInfo.get(key);
			// use ISO639 to convert the name into a code
			String k = key == ISO639.CJK_CODE ? ISO639.CJK_CODE : isoLang
					.getCode(key);
			if (k == null) {
				System.out.println(key + "!!!");
				continue;
			}
			String[] bits = v.split("\\s*-\\s*");
			int a = Integer.parseInt(bits[0], 16);
			int b = Integer.parseInt(bits[1], 16);
			IntRange r = new IntRange(a, b);
			charsetInfo2.put(r, k);
		}
	}

	/**
	 * @warning Assumes ASCII = English!
	 * @param text
	 * @return the best-guess language
	 */
	@Override
	public String classify(IDocument text) {
		IFiniteDistribution<String> dist = pClassify(text);
		if (dist.size() == 0)
			// assume English
			return "en";
		return dist.getMostLikely();
	}

	@Override
	public void finishTraining() {
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public IFiniteDistribution<String> pClassify(IDocument text) {
		String s = text.getContents();
		return pClassify2(s);
	}

	private IFiniteDistribution<String> pClassify2(String s) {		
		ObjectDistribution<String> dist = new ObjectDistribution<String>();
		// which char sets?		
		// Note: don't bother with the whole doc
		for (int i = 0, n= Math.min(s.length(), 1000); i<n; i++) {
			int c = s.codePointAt(i);
			if (!Character.isLetterOrDigit(c)) {
				continue;
			}
			for (Map.Entry<IntRange, String> e : charsetInfo2.entrySet()) {
				IntRange r = e.getKey();
				if (!r.contains(c)) {
					continue;
				}
				dist.count(e.getValue());
				break;
			}
		}
		return dist;
	}

	@Override
	public void resetup() {
	}

	@Override
	public void train1(IDocument x, String tag, double weight) {
	}

	public String getLanguage(String text) {
		if (text==null||text.isEmpty()) {
			return null;
		}
		IFiniteDistribution<String> dist = pClassify2(text);
		return dist.size()==0? null : dist.getMostLikely();
	}

}
