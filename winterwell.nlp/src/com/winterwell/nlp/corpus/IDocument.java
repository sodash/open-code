package com.winterwell.nlp.corpus;

import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.nlp.languages.ISO639;
import com.winterwell.utils.IProperties;
import com.winterwell.utils.containers.Slice;

public interface IDocument {

	/**
	 * @return The {@link ISO639} 2 letter code for the text's language,
	 * or null. 
	 */
	String getLang();
	
	/**
	 * This is (ideally) a unique identifier, e.g. "elvis@twitter" Can be null
	 */
	String getAuthor();

	String getContents();

	/**
	 * @return usually null. A region of contents to focus on.
	 */
	Slice getFocalRegion();

	IProperties getMetaData();

	/**
	 * aka Subject or Title
	 * 
	 * @return Can be null
	 */
	String getName();

	/**
	 * Some corpuses come tokenised. If so, this will invoke a corpus specific
	 * tokeniser.
	 * 
	 * @throws UnsupportedOperationException
	 */
	ITokenStream getTokenStream() throws UnsupportedOperationException;

	/**
	 * Convenience for title\n\ncontents (with sensible null handling)
	 * @return
	 */
	String getTitleAndContents();
}
