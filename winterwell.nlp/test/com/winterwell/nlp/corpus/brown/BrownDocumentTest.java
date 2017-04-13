package com.winterwell.nlp.corpus.brown;

import org.junit.Test;

import com.winterwell.nlp.io.ListTokenStream;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.utils.Utils;

public class BrownDocumentTest {

	@Test
	public void testGetText() {
		BrownCorpus bc = new BrownCorpus();
		BrownDocument doc = (BrownDocument) bc.iterator().next();

		String text = doc.getContents();
		assert text != null;
	}

	@Test
	public void testPOSTagging() {
		BrownCorpus bc = new BrownCorpus();
		BrownDocument doc = (BrownDocument) bc.iterator().next();

		ListTokenStream text = (ListTokenStream) doc.getTokenStream();
		assert text != null;
		Tkn token = text.getList().get(0);
		String pos = token.get(Tkn.POS);
		assert !Utils.isBlank(pos);
	}
}
