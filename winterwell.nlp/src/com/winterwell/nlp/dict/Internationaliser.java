/**
 * 
 */
package com.winterwell.nlp.dict;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.IReplace;
import com.winterwell.utils.Mutable;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.log.Log;

import com.winterwell.nlp.languages.ISO639;

/**

Use tr("text") to translate.

Applies the rules:

"Hello {Joe} you have 5 message(s), latest from alice@twitter</a>"
-> "Hello {} you have N message(s), latest from XID" is used as the key in the lookup table.
-> "Hello Joe you have 5 messages, latest from alice@twitter" in (English) display.

NOTE: This IS case-sensitive.

@testedby {@link InternationaliserTest}
 */
public class Internationaliser extends Dictionary {
	
	public Internationaliser(Map<String, String> word2meaning) {
		super(word2meaning);
	}
		
	
	/**
	 * 
	 * @param target Language-code, e.g. "es"
	 * @param file
	 * @param separator e.g. '\t' (which is more convenient that ,)
	 */
	public Internationaliser(String target, File file, char separator) {
		super(file, separator);
		this.target = new ISO639().getCode(target);
		assert this.target != null;
	}

	public Internationaliser(String lang) {
		super(new ArrayMap());
		this.target = lang;
		assert this.target != null;
	}
	
	public Internationaliser(String lang, File file) {
		this(lang, file, '\t');
	}


	@Override
	protected String toCanonical(String word) {
		return canon(word, new ArrayList());
	}
	
	@Override
	protected String toCanonicalMeaning(String word) {
		return canon(word, new ArrayList());
	}

	/**
	 * Target language (for logging)
	 */
	String target;

	public String tr(String en) {
		ArrayList<String> vars = new ArrayList(4);
		String enc = canon(en, vars);
		String m = dict.get(enc);
		if (m==null || m.isEmpty()) {
			// Log it so we can pick them out for translation
			Log.i("TRANS2"+target, en);
			return en;
		}
		// uncanonicalise!
		String m2 = uncanon(m, vars);
		return m2;
	}


	/**
	 * @param m
	 * @param vars
	 * @return m as we want to display it
	 */
	private String uncanon(String m, final ArrayList<String> vars) {		
		if (vars.isEmpty()) return m;
		
		// TODO Handle plurals (& even gender) via extra translations
		String[] bits = m.split(DUPE_SEPARATOR);
		m = bits[0];
				
		Pattern p = Pattern.compile("␚\\d+");
		final Mutable.Int i = new Mutable.Int();
		String m2 = StrUtils.replace(m, p, new IReplace() {			
			@Override
			public void appendReplacementTo(StringBuilder sb, Matcher match) {
				String vi = vars.get(i.value); // get i from the match??
				// remove {}s from final form
				if (vi.startsWith("{") && vi.endsWith("}")) vi = vi.substring(1, vi.length()-1);
				sb.append(vi);
				i.value++;
			}
		});
		return m2;
	}


	/**
	 * TODO This assumes that the variables will stay in the same order after translation!
	 * 
	 * @param en
	 * @param vars
	 * @return
	 */
	private String canon(String en, final List<String> vars) {
		final Mutable.Int i = new Mutable.Int();
		// Numbers, stuff in {}s, and emails/XIds
		Pattern p = Pattern.compile("\\d+|\\{.*?\\}|(\\b\\S+@(\\w+|soda\\.sh))");
		String enc = StrUtils.replace(en, p, new IReplace() {			
			@Override
			public void appendReplacementTo(StringBuilder sb, Matcher match) {
				sb.append("␚"+i);
				i.value++;
				vars.add(match.group());
			}
		});		
		return enc;
	}


	/**
	 * Remove {}s, as if using an English-to-English translation.
	 * @param english, e.g. "Hello {Alice}"
	 * @return english, e.g. "Hello Alice"
	 */
	public static String clean(String english) {
		if (english==null) return null;
		String cleaned = english.replaceAll("\\{(.*?)\\}", "$1");
		return cleaned;
	}
	
	
	
	
}
