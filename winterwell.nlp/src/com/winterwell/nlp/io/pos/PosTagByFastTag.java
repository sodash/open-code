package com.winterwell.nlp.io.pos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.knowledgebooks.nlp.fasttag.FastTag;
import com.winterwell.nlp.io.ATokenStream;
import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.utils.Key;
import com.winterwell.utils.containers.AbstractIterator;


/**
 * Add POS tags into a stream. Uses Mark Watson's FastTag BrillTagger,
 * which is a simple rules based (a lexicon of common words + 8 rules) tagger.
 * @author daniel
 * @testedby {@link PosTagByFastTagTest}
 */
public class PosTagByFastTag extends ATokenStream {

	public PosTagByFastTag(ITokenStream base) {
		super(base);
	}
	
	static FastTag fastTag = new FastTag();	
	
	boolean overwrite;
	
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
		if (overwrite) {
			desc.put(new Key("ovr"), overwrite);
		}
	}
	
	@Override
	public AbstractIterator<Tkn> iterator() {
		// read in all
		List<Tkn> tokens = base.toList();
		List<String> words = new ArrayList();
		for (Tkn tkn : tokens) {
			words.add(tkn.getText());
		}
		// tag
		List<String> tags = fastTag.tag(words);
		// add them in
		for (int i = 0; i < tags.size(); i++) {
			Tkn tkn = tokens.get(i);
			if ( ! overwrite && tkn.containsKey(Tkn.POS)) continue;
			tkn.setPOS(tags.get(i));
		}			
		// done
		final Iterator<Tkn> it = tokens.iterator();
		return new AbstractIterator<Tkn>() {
			@Override
			protected Tkn next2() throws Exception {
				return it.hasNext()? it.next() : null;
			}
		};
	}
	
	@Override
	public ITokenStream factory(String input) {
		PosTagByFastTag clone = new PosTagByFastTag(base.factory(input));
		clone.setOverwrite(overwrite);
		return clone;
	}
}
