package com.winterwell.nlp.corpus.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.winterwell.nlp.corpus.ICorpus;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.corpus.brown.BrownDocument;
import com.winterwell.utils.Utils;

public class TestCorpus implements ICorpus {

	String[] CAT_SENTENCES = new String[] {
			"My/PRP$ cat/NN is/VBZ so/RB cute/JJ ./.",
			"I/PRP can/MD has/VB cheesburger/NN LOL/NN" };

	String[] CAT_WORDS = new String[] { "cute/JJ", "cat/NN", "pussy/NN",
			"kitten/NN", "sleeping/VB", "pictures/NN" };

	List<IDocument> docs = new ArrayList<IDocument>();
	// /**
	// * From the worst erotic writing award:
	// * http://www.cbc.ca/arts/books/story/2008/11/21/sex-literature.html
	// */
	String[] PORN_SENTENCES = new String[] {
			"I/PRP am/VBP here/RB to/TO fix/VB the/DT washing/JJ machine/NN ./.",
			"Do/VB it/PRP to/TO me/PRP big/JJ boy/NN !/."
	// "At last, she could no longer control the world around her, her five senses seemed to break free and she wasn't strong enough to hold on to them.",
	// "As if struck by a sacred bolt of lightning, she unleashed them, and the world, the seagulls, the taste of salt, the hard earth, the smell of the sea, the clouds, all disappeared, and in their place appeared a vast gold light, which grew and grew until it touched the most distant star in the galaxy."
	};
	String[] PORN_WORDS = new String[] { "hot/JJ", "girl/NN", "pussy/NN",
			"sex/VB", "action/NN", "pictures/NN" };

	public TestCorpus(int n) {
		for (int i = 0; i < n; i++) {
			IDocument sd = i % 2 == 0 ? newCatDoc() : newPornDoc();
			docs.add(sd);
		}
	}

	@Override
	public Iterable<? extends IDocument> getDocumentsByTitle(String title) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterable<? extends IDocument> getDocumentsUsing(String term) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<IDocument> getSample(int num) {
		return Utils.getRandomSelection(num, docs);
	}

	@Override
	public Iterator<IDocument> iterator() {
		return docs.iterator();
	}

	private IDocument newCatDoc() {
		return newDoc(CAT_WORDS, CAT_SENTENCES);
	}

	private IDocument newDoc(String[] words, String[] sentences) {
		String title = Utils.getRandomMember(Arrays.asList(words)) + " "
				+ Utils.getRandomMember(Arrays.asList(words));
		String text = Utils.getRandomMember(Arrays.asList(words));
		return new BrownDocument(title + "\n\n" + text);
	}

	private IDocument newPornDoc() {
		return newDoc(PORN_WORDS, PORN_SENTENCES);
	}

	@Override
	public int size() {
		return docs.size();
	}

}
