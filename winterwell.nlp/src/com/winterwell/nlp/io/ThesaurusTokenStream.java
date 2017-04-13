package com.winterwell.nlp.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import com.winterwell.nlp.corpus.wordnet.IThesaurus;
import com.winterwell.utils.Key;
import com.winterwell.utils.containers.AbstractIterator;

/**
 * Pass words through a thesaurus filter, replacing words with a "canonical"
 * choice of synonym (the alphabetical first).
 * 
 * @author daniel
 */
public class ThesaurusTokenStream extends ATokenStream {

	private final IThesaurus thesaurus;
	
	public ThesaurusTokenStream(IThesaurus thesaurus, ITokenStream base) {
		super(base);
		this.thesaurus = thesaurus;
		// Is class enough?
		// Should we make IThesuarus implement IHasDesc?
//		if (thesaurus instanceof IHasDesc) {
//			
//		}
		desc.put(new Key("thesaurus"), thesaurus.getClass());
	}

	@Override
	public ITokenStream factory(String input) {
		return new ThesaurusTokenStream(thesaurus, base.factory(input));
	}

	@Override
	protected Tkn processFromBase(Tkn w, AbstractIterator<Tkn> bit) {	
		Set<String> syns = thesaurus.getSynonyms(w.getText());
		if (syns.size() == 1)
			return w;
		// replace word with the alphabetical first synonym
		ArrayList<String> list = new ArrayList<String>(syns);
		Collections.sort(list);
		w.setText(list.get(0));
		return w;
	}

}
