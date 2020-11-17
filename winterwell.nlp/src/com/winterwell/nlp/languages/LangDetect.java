package com.winterwell.nlp.languages;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mzsanford.cld.CompactLanguageDetector;
import com.mzsanford.cld.LanguageDetectionCandidate;
import com.mzsanford.cld.LanguageDetectionResult;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.nlp.classifier.ITextClassifier;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.log.Log;

/**
 * Wrapper for {@link CompactLanguageDetector}
 * 
 * (previously this class wrapped the langdetect project: http://code.google.com/p/language-detection/
 * but that was surprisingly rubbish)
 * 
 * @author daniel
 * @testedby  LangDetectTest} 
 */
public class LangDetect implements ITextClassifier<String> {
	
	static final CompactLanguageDetector cld = init();

	static ISO639 iso = new ISO639();
	
	/**
	Pro-English bias (if in doubt, say English).
	This helps avoid the many-languages noisy output we see from short tweets.
	
	TODO have a per-project bias, e.g. boost spanish in LatAm??
	 */
	private static final Map<String,Double> zBIAS = new ArrayMap(
			iso.getCode("english"), 0.25,
			iso.getCode("spanish"), 0.1);

	
	/**
	 * This avoids an exception during bootup, which can floor servers.
	 * But we don't like it!
	 * @return
	 */
	private static CompactLanguageDetector init() {
		try {
			return new CompactLanguageDetector();
		} catch(Throwable ex) {
			ex.printStackTrace();
			Log.e("lang", ex);
			return null;
		}
	}

	@Override
	public String classify(IDocument text) {
		return classify(text.getContents());
	}

	public String classify(String text) {
		if (text == null) return null;
		IFiniteDistribution<String> od = pClassify(text);
		if (od.size()==0) return null;
		String best = od.getMostLikely();
		return best;
//		LanguageDetectionResult r = cld.detect(text);
//		if (r==null) return null;
//		Locale loc = r.getProbableLocale(); // Perversely, this seems to NOT be the same as highest-scoring-candidate
//		if (loc==null) return null;
//		String code = iso.getCode(loc.getLanguage());
//		return code;
	}

	private boolean enough(String txt) {
		// TODO remove @name before scoring??
		if (txt.length() > 3) return true;
		// Not ascii?
		String lang = new CharsetClassifier().getLanguage(txt);
		if (lang!=null && ! lang.equals("en")) return true;
		return false;
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
		return pClassify(text.getContents());
	}
	
	/**
	 * @return Can be empty, never null.
	 */
	public IFiniteDistribution<String> pClassify(String txt) {
		if ( ! enough(txt)) {
			return new ObjectDistribution();
		}
		if (cld==null) {
			Log.e("LangDetect", "null language detector");
			return new ObjectDistribution();
		}
		LanguageDetectionResult r = cld.detect(txt);
		if (r==null) return new ObjectDistribution();
		List<LanguageDetectionCandidate> cs;
		try {
			cs = r.getCandidates();
		} catch(NullPointerException ex) {
			Log.v("LangDetect", "NPE for "+txt+" "+r);
			return new ObjectDistribution();
		}		
		ObjectDistribution<String> od = new ObjectDistribution<String>();
		for (LanguageDetectionCandidate c : cs) {
			Locale locale = c.getLocale();
			String lang = locale.getLanguage();
			lang = iso.getCode(lang);
			if (lang==null) {
				// Mis-code for Indonesia?
				if ("in".equals(locale.getLanguage())) {
					lang = "id";
				} else {
	//				// Filipino / Tagalog -- handled inside ISO639
					Log.v("LangDetect", "Unrecognised locale: "+locale+" for "+txt);
					continue;
				}
			}			
			// 1 language could  in principle be entered a few times under different locales
			// FIXME score or normalized score?? What is the normalisation against??
			od.addProb(lang, c.getScore());
		}
		
		// Pro-English bias (if in doubt, say English)
		// -- this helps avoid the many-languages output we see from short tweets.
		// NB: I *think* the CLD scores are [0,100]. But let's normalise to be safe.
		od.normalise();
		for(String lang : zBIAS.keySet()) {
			od.addProb(lang, zBIAS.get(lang));
		}
		od.normalise();		
		return od;		
	}
	
	@Override
	public void resetup() {		
	}

	@Override
	public void train1(IDocument x, String tag, double weight) {		
	}
		
	
}
