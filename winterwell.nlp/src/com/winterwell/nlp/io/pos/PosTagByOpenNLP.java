package com.winterwell.nlp.io.pos;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.nlp.io.ATokenStream;
import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.utils.Key;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.containers.ArraySet;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.Sequence;

/**
 * 
 * @testedby {@link PosTagByOpenNLPTest}
 * @author daniel
 *
 */
public class PosTagByOpenNLP extends ATokenStream {
	
	boolean overwrite;
	
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
		if (overwrite) {
			desc.put(new Key("ovr"), overwrite);
		}
	}
		
	private static volatile POSTaggerME tagger;
	
	/**
	 * One time call only. Automatically called when using POSTagger for the first time.
	 * @throws Exception 
	 */
	public synchronized static POSTaggerME init() {
		if (tagger!=null) return tagger;
		try {
			NLPWorkshop w = NLPWorkshop.get();
			File modelFile = w.getFile("en-pos-maxent.bin");
			FileInputStream modelIn = new FileInputStream(modelFile);
			POSModel model = new POSModel(modelIn);
			POSTaggerME postaggerme = new POSTaggerME(model);
			modelIn.close();
			tagger = postaggerme;
			// not thread safe :(
			Utils.sleep(100);
			return postaggerme;
		} catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}
	
//	/**
//	 * Probability distribution for part-of-speech tag
//	 */
//	public static final Key<String> PPOS = new Key<String>(StatsUtils.WART_PROB+"Token.pos");

	@Override
	public AbstractIterator<Tkn> iterator() {
		// read in all
		List<Tkn> tokens = base.toList();
		List<String> words = new ArrayList();
		for (Tkn tkn : tokens) {
			words.add(tkn.getText());
		}
		// TODO probability distros
		// tag
		String[] tags = tag(words.toArray(new String[0]));
		// add them in
		for (int i = 0; i < tags.length; i++) {
			Tkn tkn = tokens.get(i);
			if ( ! overwrite && tkn.containsKey(Tkn.POS)) continue;
			String tagi = tags[i];
			tkn.setPOS(tagi);			
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

	public PosTagByOpenNLP(ITokenStream base) {
		super(base);
	}
	
	@Override
	public ITokenStream factory(String input) {
		PosTagByOpenNLP clone = new PosTagByOpenNLP(base.factory(input));
		clone.setOverwrite(overwrite);
		return clone;
	}	
	
	/**
	 * Tags given string
	 * @param sentence : Array of string of English words that wants to be tagged
	 * @return array of string, the tag label of the input for each word
	 */
	public synchronized static String[] tag(String[] sentence) {		
		init();		
		String tags[] = tagger.tag(sentence);		
		return tags;
	}
	
	/**
	 * Return all possible tags of a given word. 
	 * @param words
	 * @return pos tags
	 */
	public static synchronized List<String> getPossibleTags(String w) {
		init();
		ArrayList<String> word = new ArrayList<String>();
		word.add(w);		
		Sequence[] topSequences = tagger.topKSequences(word);
		if (topSequences.length==0) {
			return Collections.EMPTY_LIST;
		}
		ArraySet<String> res = new ArraySet<String>();		
		for (Sequence seq : topSequences) {			
			double[] probs = seq.getProbs();
			assert probs.length == 1;
			if (probs[0] < 0.05) continue; // TODO normalise into distros
			List<String> tags = seq.getOutcomes();
			assert tags.size() == 1;
			res.addAll(tags);
		}
		if (res.isEmpty()) {
			res.addAll(topSequences[0].getOutcomes());
		}
		return res.asList();
	}

}
