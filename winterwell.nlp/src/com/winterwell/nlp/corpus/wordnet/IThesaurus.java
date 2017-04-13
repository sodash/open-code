package com.winterwell.nlp.corpus.wordnet;

import java.util.Set;

public interface IThesaurus {

	/**
	 * @param word
	 * @return synonyms for word (including the word itself). Never empty or
	 *         null.
	 */
	Set<String> getSynonyms(String word);

}
