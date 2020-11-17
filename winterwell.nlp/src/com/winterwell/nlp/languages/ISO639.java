package com.winterwell.nlp.languages;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.StrUtils;

/**
 * ISO 2-letter <b>Language</b> Codes (ISO standard 639-1).
 * E.g. English is "en"
 * 
 * @author daniel
 * @testedby  ISO639Test}
 * @see ISO3166
 */
public final class ISO639 {
	
	/**
	 * Chinese/Japanese/Korean. They share a unicode charset -- so some systems
	 * may consider these a "language".<br> 
	 * It isn't a language, so lets give it an obviously bogus code.
	 * This will fail {@link #is2LetterCode(String)}, but will get a return from
	 * {@link #getNameForCode(String)}
	 */
	public static final String CJK_CODE = "c?";

	static Map<String, String> code2name;

	static Map<String, String> name2code;
	
	static Map<String, String> code2macrolang;
	/**
	 * name/code pairs for extra names to recognise. This covers cases where a
	 * common name is not the official English short name.
	 */
	private static final String[] SPECIAL_CASE_NAMES = new String[] {
			"Chinese/Japanese/Korean", CJK_CODE,
			"iw", "he", // Sun mis-codes Hebrew as "iw"
			"fil", "tl" // Filipino = Tagalog hack c.f. http://www.mail-archive.com/unicode@unicode.org/msg27496.html
			};

	private static String canon(String name) {
		name = name.replaceAll("\\(.+\\)", ""); // no brackets
		return StrUtils.toCanonical(name);
	}

	private boolean preferMacroLanguage;
		
	public ISO639 setPreferMacroLanguage(boolean preferMacroLang) {
		this.preferMacroLanguage = preferMacroLang;
		return this;
	}

	private static void init() {
		if (code2name != null)
			return;
		try {
			code2name = new HashMap();
			name2code = new HashMap();
			code2macrolang = new HashMap();
			// load from file
			InputStream strm = ISO639.class
					.getResourceAsStream("iso-language-codes.csv");
			Reader _reader = new InputStreamReader(strm, "UTF8");
			BufferedReader reader = new BufferedReader(_reader);
			String line = reader.readLine(); // discard the header line
			Pattern ML = Pattern.compile("Covered by macrolanguage \\[(\\w\\w)");
			while (true) {
				line = reader.readLine();
				if (line == null) {
					break;
				}
				String[] bits = line.split("\t");
				String name = bits[1].trim();
				String nativeName = bits[2].trim();
				String code = bits[3].trim().toLowerCase();
				// Include the (potentially) long version for lookup
				name2code.put(canon(name), code);
				// cleaner, simpler
				name = name.toLowerCase();
				name = name.replaceAll(",\\s*modern", ""); // no modern
				if (name.indexOf(";")!=-1) { // e.g. "Spanish; Castilian"
					name = name.substring(0, name.indexOf(";"));
				}
				code2name.put(code, StrUtils.toTitleCase(name));
				name2code.put(canon(name), code);
				// also put in the later iso639 variants as "names" (but with paranoia about not overwiting any real names)
				String iso639_2 = bits[4].trim().toLowerCase();
				if ( ! iso639_2.isEmpty() && ! name2code.containsKey(iso639_2)) {
					name2code.put(iso639_2, code);	
				}
				// can have several
				String[] nnames = nativeName.split(",\\s*");
				for (String nn : nnames) {
					name2code.put(canon(nn), code);
				}
				// part of a macrolanguage?
				if (line.contains("macrolanguage")) {
					Matcher m = ML.matcher(line);
					boolean ok = m.find();
					if (ok) {
						String macrolang = m.group(1);
						code2macrolang.put(code, macrolang);						
					}
				}
			}
			reader.close();
			// special cases
			for (int i = 0; i < SPECIAL_CASE_NAMES.length; i += 2) {
				String name = canon(SPECIAL_CASE_NAMES[i]);
				name2code.put(name, SPECIAL_CASE_NAMES[i + 1]);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Language coder. Uses a static cache, so creating these objects is cheap.
	 */
	public ISO639() {
	}

	/**
	 * 
	 * @param languageNameOrCode Must not be null
	 * @return lowercase 2 letter code, e.g. "en", or null if unknown
	 */
	public String getCode(String languageNameOrCode) {
		init();
		// Is it a valid code already?
		// Special case: zh-tw = Chinese (Taiwan), similarly zh-hans, and a few others that fit this pattern
		if (languageNameOrCode.length() > 4 && languageNameOrCode.length() < 8 && languageNameOrCode.charAt(2)=='-') {
			String cc = languageNameOrCode.substring(0, 2).toLowerCase();
			if (code2name.containsKey(cc)) {
				return getCode2_return(cc);
			}				
		}
		if (languageNameOrCode.length() == 2) {
			String cc = languageNameOrCode.toLowerCase();
			if (code2name.containsKey(cc)) {
				return getCode2_return(cc);
			}
		}
		languageNameOrCode = canon(languageNameOrCode);
		String cc = name2code.get(languageNameOrCode);
		return getCode2_return(cc);
	}

	private String getCode2_return(String cc) {
		if ( ! preferMacroLanguage) return cc;
		if (cc==null) return cc;
		String ml = code2macrolang.get(cc);
		if (ml!=null) return ml;		
		return cc;
	}

	/**
	 * @param langCode Must be a valid code
	 * @return E.g. "English"
	 */
	public String getNameForCode(String langCode) {
		init();
		// Special case: Chinese/Korean/Japanese?
		if (langCode.equals(CJK_CODE))
			return "Chinese/Korean/Japanese";
		assert langCode.length() == 2 : langCode;
		// in case it's a name, or wrong fromat
		String ccode = getCode(langCode);
		String name = code2name.get(ccode);
		return name;
	}

	/**
	 * @param code
	 * @return true if this is a valid ISO 3166-1 2-letter code
	 */
	public boolean is2LetterCode(String code) {
		if (code==null) return false;
		init();
		return code2name.containsKey(code.toLowerCase());
	}

}
