package com.winterwell.nlp.corpus;

import java.util.Collection;

public interface ICorpus extends Iterable<IDocument> {

	/**
	 * Retrieve documents with the given title
	 * 
	 * @param title
	 */
	Iterable<? extends IDocument> getDocumentsByTitle(String title);

	/**
	 * Retrieve documents that use the given term
	 * 
	 * @param term
	 */
	Iterable<? extends IDocument> getDocumentsUsing(String term);

	/**
	 * @param num
	 * @return a random sample from the corpus. This need not be shuffled.
	 */
	Collection<IDocument> getSample(int num);

	/**
	 * @return number of documents
	 */
	int size();

}
