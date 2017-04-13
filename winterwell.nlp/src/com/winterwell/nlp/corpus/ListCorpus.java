package com.winterwell.nlp.corpus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.winterwell.nlp.io.ATokenStream;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser;
import com.winterwell.utils.Utils;

/**
 * Simple list of IDocuments.
 * 
 * @author miles
 * 
 */
public class ListCorpus implements ICorpus {

	private List<IDocument> store;

	public ListCorpus(List<IDocument> corpus) {
		store = corpus;
	}

	/**
	 * Constructor intended to make it as simple as possible to construct
	 * corpora for testing. Call as follows:
	 * 
	 * ListCorpus("title1", "Text 1", "Author 1", "title2", "Text2",
	 * "Author 2",...);
	 */
	public ListCorpus(String... titleTextAuthorTriples) {
		assert titleTextAuthorTriples.length % 3 == 0;
		store = new ArrayList<IDocument>(titleTextAuthorTriples.length / 3);
		for (int i = 0; i < titleTextAuthorTriples.length; i += 3) {
			String title = titleTextAuthorTriples[i];
			String text = titleTextAuthorTriples[i + 1];
			String author = titleTextAuthorTriples[i + 2];
			store.add(new SimpleDocument(title, text, author));
		}

	}

	@Override
	public Iterable<? extends IDocument> getDocumentsByTitle(String title) {
		List<IDocument> desired = new ArrayList<IDocument>();
		for (IDocument document : store) {
			if (document.getName().equals(title)) {
				desired.add(document);
			}
		}
		return desired;
	}

	/**
	 * Walks the documents, tokenizing each on whitespace and matching the term
	 * specified. Does no caching. Should be horribly slow if used more than
	 * once.
	 */
	@Override
	public Iterable<? extends IDocument> getDocumentsUsing(String term) {
		List<IDocument> desired = new ArrayList<IDocument>();
		for (IDocument document : store) {
			ATokenStream tokenStream = new WordAndPunctuationTokeniser(
					document.getContents());
			for (Tkn token : tokenStream) {
				if (token.getText().equals(term)) {
					desired.add(document);
					break;
				}	
			}			
		}
		return desired;
	}

	@Override
	public Collection<IDocument> getSample(int num) {
		return Utils.getRandomSelection(num, store);
	}

	@Override
	public Iterator<IDocument> iterator() {
		// TODO Auto-generated method stub
		return store.iterator();
	}

	@Override
	public int size() {
		return store.size();
	}

}
