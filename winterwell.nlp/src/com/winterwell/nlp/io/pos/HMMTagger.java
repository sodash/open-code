package com.winterwell.nlp.io.pos;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.maths.hmm.FlexiHMM;
import com.winterwell.nlp.io.ATokenStream;
import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.utils.Key;
import com.winterwell.utils.containers.AbstractIterator;

/**
 * Use a Hidden Markov Model to apply tags
 * 
 * @author daniel
 * @testedby {@link HMMTaggerTest}
 */
public class HMMTagger<X> extends ATokenStream {

	int bi;
	List<Tkn> buffer;

	final private FlexiHMM<Tkn, X> dist;

	final Key<X> key;

	public HMMTagger(ITokenStream base, Key<X> key, FlexiHMM<Tkn, X> dist) {
		super(base);
		this.key = key;
		this.dist = dist;
	}

	@Override
	public ITokenStream factory(String input) {
		ITokenStream newBase = base.factory(input);
		return new HMMTagger<X>(newBase, key, dist);
	}

	public FlexiHMM<Tkn, X> getModel() {
		return dist;
	}

	@Override
	public AbstractIterator<Tkn> iterator() {
		return new AbstractIterator<Tkn>() {
			@Override
			protected Tkn next2() throws Exception {
				return next3(); // TODO better!
			}
		};
	}
	
	protected Tkn next3() {
		if (buffer != null) {
			if (bi == buffer.size())
				return null;
			Tkn n = buffer.get(bi);
			bi++;
			return n;
		}
		// Viterbi the whole damn thing (TODO rolling viterbi for handling
		// larger docs)
		next3_fillBuffer();
		next3_viterbi();
		// ready
		return next3();
	}

	private void next3_fillBuffer() {
		buffer = new ArrayList<Tkn>();
		for (Tkn tok : base) {
			buffer.add(tok);
		}
	}

	private void next3_viterbi() {
		List<X> hidden = dist.viterbi(buffer);
		for (int i = 0; i < buffer.size(); i++) {
			Tkn tok = buffer.get(i);
			X h = hidden.get(i);
			tok.put(key, h);
		}
	}

}
