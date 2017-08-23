package com.winterwell.nlp.corpus.wikipedia;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

import com.winterwell.maths.datastorage.HalfLifeIndex;
import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.ATask;

/**
 * FIXME the list created for English is pretty bad. Wikipedia perhaps isn't the
 * best corpus here!
 * 
 * TODO use Google's n-grams??
 * 
 * @see WikipediaCorpus
 * @testedby {@link CreateWikipediaStopwordsTest}
 * @author daniel
 * 
 */
public class CreateWikipediaStopwords extends ATask<List<String>> {

	public static final String FILENAME = "stopwords.wikipedia.txt";

	/**
	 * Make some (but NOT English cos we like the list we have)
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
//		Depot.getDefault().setErrorPolicy(KErrorPolicy.ASK);
		for (String lang : new String[] { 
//				"en"
//				"es"
//				"he", 
				"tr"
		}) {
			NLPWorkshop ws = NLPWorkshop.get(lang);
			CreateWikipediaStopwords cwd = new CreateWikipediaStopwords(ws);
			cwd.run();
		}
	}

	/**
	 * documents processed
	 */
	int cnt;

	private WikipediaCorpus corpus;

	private HalfLifeIndex<String> index;

	/**
	 * How many stopwords to find.
	 */
	private int size = 700;

	private final NLPWorkshop workshop;

	public CreateWikipediaStopwords(NLPWorkshop ws) {
		workshop = ws;
		// protect the English & Arabic stopwords list
		// (Arabic list from http://arabicstopwords.sourceforge.net/ via Dubai
		// School of Govt)
		// assert ! workshop.getLanguage().startsWith("en");
		assert ! workshop.getLanguage().startsWith("ar");
		assert ! workshop.getLanguage().startsWith("en");
	}

	@Override
	public List<String> getIntermediateOutput() {
		List<String> words = index.getSortedEntries();
		words = Containers.subList(words, 0, size);
		return words;
	}

	@Override
	public double[] getProgress() {
		return new double[] { cnt, corpus.size() };
	}

	@Override
	public List<String> run() throws IOException {
		corpus = new WikipediaCorpus(workshop);
		if ( ! corpus.getDumpFile().exists()) {
			corpus.download();
		}
		// collect frequency counts in a lossy/approximate manner
		index = new HalfLifeIndex<String>(size * 10);
		for (IDocument doc : corpus) {
			String contents = doc.getContents();
			String _contents = contents.replaceAll("\\{\\{.+?\\}\\}", "");
//			if ( ! contents.equals(_contents)) {
//				System.out.println(_contents);
//			}
			WordAndPunctuationTokeniser ts = new WordAndPunctuationTokeniser(
					_contents);
			ts.setLowerCase(true);
			ts.setSwallowPunctuation(false);
			ts.setSplitOnApostrophe(false);
			for (Tkn token : ts) {
				String txt = token.getText();
				if (txt.matches(".*\\d.*")) continue;
				if (StrUtils.isPunctuation(txt)) {
					continue;
				}
				index.indexOfWithAdd(txt);
			}
			cnt++;
			if (cnt % 1000 == 0) {
				Log.i("corpus", "	..." + cnt + " documents");
			}
		}

		// get the stopwords
		List<String> words = index.getSortedEntries();
		words = Containers.subList(words, 0, size);

		// output stopwords, over-writing if it's there!!
		File out = new File("wtf-stopwords.txt"); 				
		out.getAbsoluteFile().getParentFile().mkdirs();
		BufferedWriter writer = FileUtils.getWriter(out);
		for (String w : words) {
			writer.write(w);
			writer.write('\n');
		}
		FileUtils.close(writer);
		Log.i("stopwords", "...Created temp file "+out);
		File properFile = workshop.getFilePointer(CreateWikipediaStopwords.FILENAME);
		FileUtils.move(out, properFile);
		Log.i("stopwords", "Created file "+properFile);
		
		// done
		// ...free the memory
		corpus = null;
		index = null;
		return words;
	}

}
