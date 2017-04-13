package com.winterwell.nlp.corpus.gutenberg;

import org.junit.Test;

import com.winterwell.utils.Printer;

import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.utils.StrUtils;

public class GutenbergCorpusTest {

	@Test
	public void testLear() {
		GutenbergCorpus corpus = new GutenbergCorpus();
		for (IDocument doc : corpus) {
			String author = doc.getAuthor();
			String name = doc.getName();
			Printer.out(name + " by " + author);
			String text = doc.getContents();
			int i = text.toLowerCase().indexOf("gutenberg");
			if (i != -1) {
				Printer.out(StrUtils.substring(text, i - 50, i + 50));
			}
			assert !text.toLowerCase().contains("gutenberg");
		}
	}
}
