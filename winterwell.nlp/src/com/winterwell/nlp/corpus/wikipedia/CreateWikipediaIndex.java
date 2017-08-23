package com.winterwell.nlp.corpus.wikipedia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.web.WebUtils;

/**
 * Just the titles, one per line, with no fancy processing, no ma'am.
 * 
 * @author daniel
 * 
 */
public class CreateWikipediaIndex {

	public static final String FILENAME = "wikipedia-titles.txt";
	private String lang;

	public CreateWikipediaIndex(String lang) {
		this.lang = lang;
	}

	public static void main(String[] args) throws Exception {
		String lang = "en";
		CreateWikipediaIndex cwi = new CreateWikipediaIndex(lang);
		cwi.run();
	}

	private int i;
	private BufferedWriter writer;

	protected void addEntry(String title) {
		if (!WikipediaCorpus.filterByTitle.accept(title))
			return;
		try {
			writer.append(title);
			writer.newLine();
			if (i % 100 == 0) {
				Printer.out(i + ":\t" + title);
			}
			i++;
		} catch (IOException e) {
			throw Utils.runtime(e);
		}
	}

	private void run() throws Exception {
		File file = NLPWorkshop.get(lang).getFilePointer(FILENAME);
		writer = FileUtils.getWriter(file);
		WikipediaCorpus cwd = new WikipediaCorpus(NLPWorkshop.get(lang));
		InputStream in = cwd.getInputStream();
		BufferedReader reader = FileUtils.getReader(in);
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			List<String> title = WebUtils.extractXmlTags("title", line, false);
			if (title.isEmpty()) {
				continue;
			}
			addEntry(title.get(0));
		}
		FileUtils.close(writer);
	}
}
