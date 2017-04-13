package com.winterwell.nlp.corpus.brown;

import static com.winterwell.utils.Printer.out;

import org.junit.Test;

import com.winterwell.nlp.corpus.IDocument;

public class BrownCorpusTest {

	@Test
	public void testDir() {
		BrownCorpus bcr = new BrownCorpus();
		System.out.println(bcr.dir);
		for (IDocument iDocument : bcr) {
			out(iDocument);
			break;
		}
	}

}
