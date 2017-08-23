package com.winterwell.nlp.vectornlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.LineReader;
import com.winterwell.utils.log.Log;
import com.winterwell.web.FakeBrowser;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Vector;

/**
 * Use the GLOVE pre-computed word vectors.
 * Warning: Make ONE instance and cache it globally! This is a big object and slow to load.
 * @author daniel
 * @testedby {@link GloveWordVectorsTest}
 */
public class GloveWordVectors {
	private static final String LOGTAG = "glove";
	private File file;
	private KGloveSource src;
	private Set<String> dictionary;
	

	public static enum KGloveSource {
		/**
		 * 400k vocab
		 */
		Wikipedia("glove.6B.zip", "glove.6B.300d.txt"), 
		CommonCrawl("glove.42B.300d.zip", "glove.42B.300d.txt"), 
		/**
		 * 1.2M vocab
		 */
		Twitter("glove.twitter.27B.zip", "glove.twitter.27B.200d.txt");
		
		public final String filename;
		public final String entry;
		private KGloveSource(String filename, String entry) {
			this.filename = filename;
			this.entry = entry;
		}
	}
	
	public GloveWordVectors() {
		this(KGloveSource.Wikipedia);		
	}
	
	public GloveWordVectors(KGloveSource src) {
		this(getFile(src));
		this.src = src;
		dictionary = NLPWorkshop.get("en").getDictionary();
	}
	
	private static File getFile(KGloveSource src) {
		// gzip entry? (this will probably fail, with a failed scp)
		File gzipd = NLPWorkshop.get("en").getFile(src.entry+".gz");
		if (gzipd!=null && gzipd.exists()) return gzipd;
		
		NLPWorkshop.get("en").getFile(src.filename);
		File zip = NLPWorkshop.get("en").getFile(src.filename);
		if (zip!=null && zip.exists()) return zip;
		return null;
	}

	public GloveWordVectors(File file) {
		this.file = file;
	}
	
	final Map<String,Vector> word2vec = new HashMap();
	
	/**
	 * Download a fresh version of Wikipedia! This may be slow.
	 * @param file2 
	 */
	public void download(KGloveSource src) {
		File localFile = NLPWorkshop.get("en").getFilePointer(src.filename);
		String url = "http://nlp.stanford.edu/data/"+src.filename;
		Log.i(LOGTAG, "Downloading " + src + " vectors from " + url + " to "+localFile+" ...");
		FakeBrowser fb = new FakeBrowser();
		fb.setMaxDownload(10000); // 10gb!
		File download = fb.getFile(url);		
		FileUtils.move(download, localFile);
		Log.i("corpus", "...stored "+localFile);
	}

	
	public GloveWordVectors init() {
		if ( ! word2vec.isEmpty()) return this;
		synchronized (this) {
			ZipFile zipf = null;
			try {
				if ( ! word2vec.isEmpty()) return this;
				Log.d("glove", "Loading word vectors from "+file+" ...");
				if (file==null || ! file.exists()) {
					// download it
					download(src);				
				}
				BufferedReader reader;
				if (file.getName().endsWith(".gz")) {
					reader = FileUtils.getGZIPReader(file);
				} else if (file.getName().endsWith(".zip")) {
					zipf = new ZipFile(file);
					ZipEntry entry = zipf.getEntry(src.entry);
					if (entry==null) {
						throw new FileNotFoundException(src.entry+" in "+zipf);
					}
					InputStream in = zipf.getInputStream(entry);
					reader = FileUtils.getReader(in);
				} else {
					reader = FileUtils.getReader(file);
				}
				// read in the data: each line is a word
				LineReader lr = new LineReader(reader);
				int cnt=0;
				for (String input : lr) {
					cnt++;
					if (cnt % 100000 == 0) Log.d("glove", "...loaded "+cnt+" "+(cnt/4000)+"% kept "+word2vec.size()+" word vectors...");
					String[] data = input.split(" ");
					String word = data[0];	
					// dictionary filter, cos Glove does have a lot of dud-words in it
					if (dictionary!=null) {
						if ( ! dictionary.contains(word)) {
							continue;
						}
					}
					Vector sv = new DenseVector(data.length);
					for (int i=1;i<data.length;i++) {				
						Double v = Double.valueOf(data[i]);
						sv.set(i-1, v);
					}			
					word2vec.put(canon(word),sv);					
				}
				lr.close();
				Log.d("glove", "...loaded "+word2vec.size()+" word vectors.");
				return this;
			} catch(Exception ex) {
				throw Utils.runtime(ex);
			} finally {
				FileUtils.close(zipf);
			}
		}
	}

	private String canon(String word) {
		return StrUtils.toCanonical(word);
	}

	/**
	 * Does a defensive copy
	 * @param string
	 * @return
	 */
	public Vector getVector(String string) {
		init();
		Vector v = word2vec.get(canon(string));
		// safety copy - protect the store from edits
		return v==null? null : new DenseVector(v);
	}

	public Set<String> getWords() {
		init();
		return word2vec.keySet();
	}
	
}
