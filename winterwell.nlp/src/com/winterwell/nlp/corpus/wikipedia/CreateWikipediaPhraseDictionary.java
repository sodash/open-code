package com.winterwell.nlp.corpus.wikipedia;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

import com.winterwell.utils.Printer;

import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.LineReader;

/**
 * TODO a token joiner that takes a phrase dictionary, and uses it to re-combine
 * tokens?? Or should it be done inside {@link WordAndPunctuationTokeniser}??
 * 
 * Create a dictionary of wikipedia concepts
 * 
 * @author daniel
 * 
 */
public class CreateWikipediaPhraseDictionary {

	public static final String FILENAME = "multiword-wiki-articles.txt";

	public static void main(String[] args) throws IOException {
		CreateWikipediaPhraseDictionary cwd = new CreateWikipediaPhraseDictionary();
		cwd.run();
	}

	private void run() throws IOException {
		File index = NLPWorkshop.get().getFile(CreateWikipediaIndex.FILENAME);
		File out = NLPWorkshop.get()
				.getFile(CreateWikipediaPhraseDictionary.FILENAME);
		BufferedWriter writer = FileUtils.getWriter(out);
		LineReader lr = new LineReader(index);
		for (String title : lr) {
			int bi = title.indexOf('(');
			if (bi != 0) {
				title = title.substring(bi - 1);
			}
			WordAndPunctuationTokeniser ts = new WordAndPunctuationTokeniser(
					title);
			List<Tkn> tokens = Containers.getList(ts);
			if (tokens.size() < 2) {
				continue;
			}
			String s = Printer.toString(tokens, " ");
			writer.write(s);
			writer.newLine();
		}
		FileUtils.close(writer);
	}

}
