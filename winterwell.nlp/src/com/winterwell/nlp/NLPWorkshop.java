package com.winterwell.nlp;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.winterwell.depot.Depot;
import com.winterwell.depot.Desc;
import com.winterwell.depot.MetaData;
import com.winterwell.maths.datastorage.HalfLifeMap;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.corpus.brown.BrownCorpus;
import com.winterwell.nlp.dict.Dictionary;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser;
//import com.winterwell.nlp.similarity.TFIDFCosineSimilarity;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Key;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Cache;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;

/**
 * Provide access to NLP stuff - such as dictionaries - in a context sensitive
 * way.
 * 
 * @see CacheFile
 * @author daniel
 * @testedby  NLPWorkshopTest}
 */
public class NLPWorkshop {

	private static final Depot depot = Depot.getDefault();
	static final String STOPWORDS_FILENAME = "stopwords.txt";
	
	/**
	 * ISO639 2-letter code, lowercase
	 */
	public static final Key<String> LANG = new Key("lang");
	
//	private static TFIDFCosineSimilarity tcs;
	/**
	 * Not used! Purpose unknown.
	 */
	String context = null;

	private Set<String> dict;

	final String language;

	private Set<String> stopwords;
	private String[] langs;

	/**
	 * The default setup: English!
	 */
	private NLPWorkshop() {
		this("en");
	}

	/**
	 * NB: Just constructing these is cheap -- but they do cache stopwords (when accessed).
	 *  
	 * @param language
	 */
	private NLPWorkshop(String language) {
		this.language = language;
		assert language != null;
	}
	
	public NLPWorkshop(String[] langs) {
		this.langs = langs;
		language = null;
	}

	/**
	 * 
	 * @param lang Can be null (defaults to "en")
	 * @return
	 */
	public static NLPWorkshop get(String lang) {
		if (lang==null) lang = "en";
		assert lang.length() == 2 : lang;
		NLPWorkshop w = lang2workshop.get(lang);
		if (w==null) {
			w = new NLPWorkshop(lang);
			lang2workshop.put(lang, w);
		}
		return w;
	}
	

	public static NLPWorkshop get(String[] langs) {
		NLPWorkshop w = new NLPWorkshop(langs);
		return w;
	}
	
	static final Cache<String,NLPWorkshop> lang2workshop = new Cache<String, NLPWorkshop>(5);
	
	/**
	 * Miscellaneous cache -- e.g. question words, edited stopwords.
	 */
	public final Cache<String, Object> cache = new Cache(20);


	Desc<File> getConfig(String filename) {
		Desc<File> ad = getNLPFileArtifact(filename);
		ad.setServer(Desc.CENTRAL_SERVER);
//		ad.put(new Key("file"), filename);
		return ad;
	}

	/**
	 * @testedby  NLPWorkshopTest#getDictionary()}
	 * @return
	 */
	public Set<String> getDictionary() {
		if (dict != null)
			return dict;
		File f;
		if (language == Locale.ENGLISH.getLanguage()) {
			// assume British english and we're on Ubuntu
			f = new File("/usr/share/dict/british-english");
		} else
			throw new TodoException();
		if (!f.exists())
			throw new WrappedException(new FileNotFoundException(f
					+ "does not exist. Please install the wbritish package."));
		String s = FileUtils.read(f);
		String[] words = s.split("\\s+");
		dict = Collections.unmodifiableSet(new HashSet<String>(Arrays
				.asList(words)));
		return dict;
	}
	
	/**
	 * For English: returns the Moby POS dictionary, which has lots of phrases as well as words.
	 * @return
	 */
	public Dictionary getPOSDictionary() {
		if ("en".equals(language)) {
			// The mobyposi dictionary, from datastore
			File f = getFile("mobyposi-dictionary.csv");
			Dictionary d = new Dictionary(f, '×');
			// TODO map this to use Brown-style annotations, and store that somewhere
			return d;
		}
		return null;
	}

	/**
	 * Use the depot and context to get a file. This may trigger it being
	 * fetched.
	 * 
	 * @param filename
	 *            This should not usually include any directories
	 * @return Warning: can return null if depot fails
	 * 
	 * Note: Use Depot.getDefault().setErrorPolicy(KErrorPolicy.ASK) to choose and put the file.
	 */
	public File getFile(String filename) {
		assert filename!=null;
		if (langs!=null) {
			// FIXME loop and join
		}
		Desc<File> ad = getConfig(filename);		
		File f = depot.get(ad);
		if (f==null) {
			Log.w("NLP", "No file from depot! filename: "+filename+" lang:"+language);
			return null;
		}
//		assert f.isFile() : f; good idea but risky
		return f;
	}

	/**
	 * Use the depot and context to get a file which may or may not be empty.
	 * This will try Depot.get() so it can do a remote fetch.
	 * 
	 * @param filename
	 *            This should not usually include any directories
	 * @return a File; never null
	 */
	public File getFilePointer(String filename) {
		Desc<File> ad = getConfig(filename);
		try {
			File f = depot.get(ad);
			if (f!=null) return f;
		} catch (Exception e) {
		}
		MetaData f = depot.getMetaData(ad);
		return f.getFile();
	}

	/**
	 * lowercase iso code for the language
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * @param name
	 *            This should include file suffixes such as .txt
	 * @return a blankish artifact description for the workshop's group,
	 *         language and context.
	 */
	Desc<File> getNLPFileArtifact(String name) {
		Desc<File> ad = new Desc<File>(name, File.class);
		ad.setTag("winterwell.nlp");
		if (language!=null) {
			ad.put(LANG, language);
		} else {
			ad.put(LANG, StrUtils.join(langs,","));
		}
		ad.put(new Key("context"), context);
		return ad;
	}

	/**
	 * Stopwords for this language. The stopwords are loaded once then cached
	 * for efficiency.
	 * 
	 * The list for English includes stemmed versions!?
	 * 
	 * @return Cannot be edited!
	 * ??Should this use {@link Dictionary}??
	 */
	public Set<String> getStopwords() {
		if (stopwords != null) return stopwords;
		// load once and cache
		File file = getFile(STOPWORDS_FILENAME);
		if (file==null || ! file.exists() || ! file.isFile()) {
			Log.w("nlp", "Can't find good stopwords at "+file);
			// Try the short version
			file = getFile("stopwords-short.txt");
			if (file==null || ! file.exists()) {
				// Fail!
				Log.e("nlp", "Can't find any stopwords for "+language);
				return new ArraySet(0);
			}
			Log.i("nlp", "Using short stopwords list for "+language);
		}
		String txt = FileUtils.read(file);
		// wrap in a hashset for fast contains()
		List<String> words = Arrays.asList(StrUtils.splitLines(txt));
		HashSet<String> _stopwords = new HashSet<String>(words);
		// Add in canonicalised forms
		// -- this is likely to be irrelevant for English, where
		// lower-casing the stream
		// will handle things. But it's important for Arabic
		for (String word : words) {
			_stopwords.add(StrUtils.toCanonical(word));
		}
		stopwords = Collections.unmodifiableSet(_stopwords);
		return stopwords;
	}
	

//	@Deprecated
//	// let's move this to using the depot
//	public TFIDFCosineSimilarity getTFIDFCosineSimilarity() {
//		String wordFrequencies = "bnc_simple.al.gz";
//		// depot.cache(getConfig(wordFrequencies), new
//		// File("data/bnc_simple.al.gz"));
//		if (tcs == null) {
//			BufferedReader frequencyData = null;
//			try {
//				File wordFrequenciesFile = getFile(wordFrequencies);
//				frequencyData = FileUtils.getReader(new GZIPInputStream(
//						new FileInputStream(wordFrequenciesFile)));
//			} catch (IOException e) {
//				throw new WrappedException(e);
//			}
//			DefaultTokenStream dts = new DefaultTokenStream("");
//			tcs = new TFIDFCosineSimilarity(frequencyData, dts);
//		}
//		return tcs;
//	}

	/**
	 * SWALLOWS Exceptions.
	 * @return
	 */
	public Collection<String> getQuestionWords() {
		String ckey = "question-words";
		Collection<String> qwords = (Collection<String>) cache.get(ckey);
		if (qwords!=null) return qwords;
		try {
			File f = getFile("bootstrap-question-words.txt");
			if (f!=null && f.exists()) {
				qwords = FileUtils.readList(f);
				cache.put(ckey, qwords);
				return qwords;
			} else if ("en".equals(language)) {
				Log.e("nlp.question-words", new FailureException("No question-words?! "+f));	
			}
		} catch(Throwable ex) {
			Log.e("nlp", ex);
		}	
		return Collections.EMPTY_SET;
	}

	/**
	 * @deprecated Default to English!
	 * @return
	 */
	public static NLPWorkshop get() {
		return get("en");
	}

	/**
	 * @return
	 */
	public static NLPWorkshop getCombined() {
		return get(new String[]{"en", "es", "de", "fr"});
	}

	public IFiniteDistribution<String> getWordFrequency() {
		assert "en".equals(language) : language;
		Desc<IFiniteDistribution> desc = new Desc("wordFreq_Brown", IFiniteDistribution.class);
		desc.setTag("winterwell.nlp");
		desc.put(LANG, language);
		IFiniteDistribution<String> distro = Depot.getDefault().get(desc);
		if (distro!=null) return distro;
		ObjectDistribution _distro = new ObjectDistribution(new HalfLifeMap(100000), false);
		BrownCorpus bc = new BrownCorpus();
		WordAndPunctuationTokeniser tokeniser = new WordAndPunctuationTokeniser().setLowerCase(true);
		tokeniser.setSwallowPunctuation(true);
		
		for(IDocument doc : bc) {
//			Log.d("nlp.wordfreq", "..."+doc+"...");
			WordAndPunctuationTokeniser tokens = tokeniser.factory(doc.getContents());
			for (Tkn tkn : tokens) {
				_distro.train1(tkn.getText());
			}
		}
		Depot.getDefault().put(desc, _distro);
		return _distro;
	}

}
