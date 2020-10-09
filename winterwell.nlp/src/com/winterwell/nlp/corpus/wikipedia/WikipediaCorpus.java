package com.winterwell.nlp.corpus.wikipedia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.nlp.corpus.ICorpus;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.corpus.SimpleDocument;
import com.winterwell.utils.IFilter;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;

/**
 * @testedby  WikipediaCorpusTest}
 * @author daniel
 * 
 */
public class WikipediaCorpus implements ICorpus {

	/**
	 * s/en/language/ to get other languages
	 */
	static final String databaseDumpUrl = "http://download.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2";

	/**
	 * Ignore special pages, talk pages, user pages and help pages
	 */
	static IFilter<String> filterByTitle = new IFilter<String>() {
		@Override
		public boolean accept(String x) {
			if (x.indexOf(':') != -1)
				return false;
			return true;
		}
	};

	/**
	 * xml in a bz2 zip
	 */
	private final File dumpFile;

	/**
	 * 2-letter code
	 */
	private final String lang;

	public WikipediaCorpus(NLPWorkshop workshop) {
		lang = workshop.getLanguage().substring(0, 2);
		dumpFile = workshop.getFilePointer("wikipedia" + lang + ".xml.bz2");
	}

	/**
	 * Download a fresh version of Wikipedia! This may be slow.
	 */
	public void download() {
		String url = databaseDumpUrl.replace("en", lang);
		Log.i("corpus", "Downloading " + lang + " Wikipedia from " + url + "...");
		FakeBrowser fb = new FakeBrowser();
		fb.setMaxDownload(10000); // 10gb!
		File download = fb.getFile(url);
		FileUtils.move(download, dumpFile);
		Log.i("corpus", "...stored " + lang + " Wikipedia at " + dumpFile);
	}

	@Override
	public Iterable<? extends IDocument> getDocumentsByTitle(String title) {
		// TODO use Lucene
		throw new TodoException();
	}

	@Override
	public Iterable<? extends IDocument> getDocumentsUsing(String term) {
		// TODO use Lucene
		throw new TodoException();
	}

	public File getDumpFile() {
		return dumpFile;
	}

	InputStream getInputStream() throws IOException {
		// attempt to find the dump in local data
		if (!dumpFile.exists()) {
			File f2 = new File(FileUtils.getWinterwellDir(),
					"code/winterwell.nlp/data/corpora/wikipedia/" + lang
							+ "wiki-latest-pages-articles.xml.bz2");
			if (f2.exists()) {
				Printer.out("...Copying " + f2 + " into storage");
				FileUtils.copy(f2, dumpFile);
			} else
				throw new FileNotFoundException(dumpFile + ", " + f2);
		}
		// Get the xml file
		assert dumpFile != null && dumpFile.length() > 10000;
		// Open an xml reader on it
		FileInputStream fin = new FileInputStream(dumpFile);
		// Advice from http://www.kohsuke.org/: Jacek Bilski told me
		// that he had to read two bytes from the stream before he uses
		// CBZip2InputStream. Those two bytes ('B' and 'Z') are used by
		// the command line bzip program to mark the stream.
		int b1 = fin.read();
		int b2 = fin.read();
		BZip2CompressorInputStream in = new BZip2CompressorInputStream(fin); // was CBZip2InputStream in an earlier apache/jakarta library
		return in;
	}

	@Override
	public Collection<IDocument> getSample(int num)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<IDocument> iterator() {
		try {
			InputStream in = getInputStream();
			final BufferedReader reader = FileUtils.getReader(in);
			return new AbstractIterator<IDocument>() {
				@Override
				protected IDocument next2() throws IOException {
					return iterator2_crudeParse(reader);
				}
			};
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	protected IDocument iterator2_crudeParse(BufferedReader reader)
			throws IOException {
		while (true) {
			// tags: page, title, text
			while (true) {
				String line = reader.readLine();
				if (line == null)
					return null;
				if (line.contains("<page>")) {
					break;
				}
			}
			// collect the page
			StringBuilder page = new StringBuilder();
			while (true) {
				String line = reader.readLine();
				if (line == null)
					return null;
				page.append(line);
				if (line.contains("</page>")) {
					break;
				}
			}
			// do we have a doc?
			String p = page.toString();
			List<String> title = WebUtils.extractXmlTags("title", p, false);
			List<String> text = WebUtils.extractXmlTags("text", p, false);
			if (title.isEmpty() || text.isEmpty()) {
				continue; // try again
			}
			if (!filterByTitle.accept(title.get(0))) {
				continue; // try again
			}
			assert title.size() == 1 : title;
			assert text.size() == 1 : text;
			String ttl = WebUtils2.htmlDecode(title.get(0));
			String txt = WebUtils2.htmlDecode(text.get(0));
			return new SimpleDocument(ttl, txt, null);
		}
	}

	public void processArticle(String title, String text) {
		Printer.out("\n## " + title + "\n" + StrUtils.ellipsize(text, 280));
	}

	/**
	 * HACK: Guess that it's large
	 */
	@Override
	public int size() {
		return 10000000; // 10 million? What is en.wikipedia these days?
	}

}

// Example article:
// <page>
// <title>AccessibleComputing</title>
// <id>10</id>
// <redirect />
// <revision>
// <id>133452289</id>
// <timestamp>2007-05-25T17:12:12Z</timestamp>
// <contributor>
// <username>Gurch</username>
// <id>241822</id>
// </contributor>
// <minor />
// <comment>Revert edit(s) by [[Special:Contributions/Ngaiklin|Ngaiklin]] to
// last version by [[Special:Contributions/Rory096|Rory096]]</comment>
// <text xml:space="preserve">#REDIRECT [[Computer accessibility]] {{R from
// CamelCase}}</text>
// </revision>
// </page>
