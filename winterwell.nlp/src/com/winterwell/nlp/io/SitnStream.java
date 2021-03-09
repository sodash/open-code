package com.winterwell.nlp.io;

/**
 * 
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.IFactory;
import com.winterwell.maths.stats.distributions.cond.Cntxt;
import com.winterwell.maths.stats.distributions.cond.ISitnStream;
import com.winterwell.maths.stats.distributions.cond.Sitn;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.corpus.SimpleDocument;
import com.winterwell.nlp.io.SitnStream.IFeature;
import com.winterwell.nlp.io.pos.PosTagByFastTag;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.containers.Containers;

/**
 * Slice a document into Sitn -- a series of words, each with a context.
 * @author daniel
 *
 */
public class SitnStream implements ISitnStream<Tkn> {

	@Override
	public String toString() {
		return "SitnStream [sig=" + Arrays.toString(sig) + ", source=" + source
				+ "]";
	}

	final ITokenStream words;
	final String[] sig;
	final IDocument source;
	
	/**
	 * Equivalent to {@link #SitnStream(IDocument, ITokenStream, String[], IFeature[])},
	 * except the features are setup via {@link #getFeature(String)}, and
	 * rely on either standard names (e.g. "w-1") or over-riding getFeature().
	 * @param source Can be null
	 * @param words
	 * @param sig
	 */
	public SitnStream(IDocument source, ITokenStream words, String[] sig) {
		assert sig != null;
		this.words = words;
		this.sig = sig;
		this.source = source;
		features = new IFeature[sig.length];
		for (int i = 0; i < sig.length; i++) {
			features[i] = getFeature(sig[i]);
		}
	}
	
	SitnStream(IDocument source, ITokenStream words, String[] sig, IFeature[] features) {
		this.words = words;
		this.sig = sig;
		this.source = source;
		this.features = features;		
	}

	protected IFeature getFeature(String ftr) {
		assert ftr != null;
		IFeature f = featureFromName.factory(ftr);
		if (f!=null) return f;		
		throw new TodoException(ftr);
	}
	
	IFactory<String, IFeature> featureFromName = new CommonFeatureFactory();

	public void setFeatureFromNameFactory(IFactory<String, IFeature> featureFromName) {
		this.featureFromName = featureFromName;
	}
	
	@Override
	public SitnStream factory(Object sourceSpecifier) {
		// input: IDocument, String or ITokenStream
		// new words!
		ITokenStream words2 = null;
		if (sourceSpecifier instanceof ITokenStream) {
			words2 = (ITokenStream) sourceSpecifier;
		} else {
			String text = null;
			if (sourceSpecifier instanceof IDocument) {
				IDocument doc = (IDocument) sourceSpecifier;
				text = doc.getTitleAndContents();
			} else {
				text = (String) sourceSpecifier;
				sourceSpecifier = new SimpleDocument(text);
			}
			words2 = words.factory(text);			
		}
		// same sig & features
		return new SitnStream((IDocument) sourceSpecifier, words2, sig, features);
	}

	@Override
	public String[] getContextSignature() {
		return sig;
	}

	@Override
	public Collection<Class> getFactoryTypes() {
		return Arrays.asList(
			(Class)IDocument.class,
			String.class		
		);
	}

	@Override
	public AbstractIterator<Sitn<Tkn>> iterator() {
		return new SitnStreamIt(this);
	}

	IFeature[] features;
	
	/**
	 * A function for getting a piece of information e.g. "previous word"
	 * for building Sitns. Often created by name using {@link CommonFeatureFactory}
	 * @author daniel
	 *
	 */
	public static interface IFeature {
		/**
		 * 
		 * @param source
		 * @param words
		 * @param i Position in the word list
		 * @return the value of this feature for word i
		 */
		Object extract(IDocument source, List<Tkn> words, int i);		
	}

	@Override
	public Map<String, Object> getFixedFeatures() {
		return null;
	}

	/**
	 * @return outcomes (e.g. words) separated by spaces.
	 */
	public String getText() {
		StringBuilder sb = new StringBuilder();
		for(Sitn sitn : this) {
			sb.append(sitn.outcome.toString());
			sb.append(" ");
		}
		return sb.toString();
	}
}

class SitnStreamIt extends AbstractIterator<Sitn<Tkn>> {

	private final SitnStream stream;
	private final List<Tkn> words;
	int i;

	public SitnStreamIt(SitnStream sitnStream) {
		this.stream = sitnStream;
		words = Containers.getList(stream.words);
		// Simplify the tokens TODO is this needed?
		for (int j = 0; j < words.size(); j++) {
			Tkn tkn = words.get(j);
			// Strip the Tkn down to just text & POS
			Tkn tkn2 = new Tkn(tkn.getText(), tkn.start, tkn.end);
			String pos = tkn.getPOS();
			tkn2.put(Tkn.POS, pos);
			words.set(j, tkn2);
		}
	}

	@Override
	protected Sitn<Tkn> next2() throws Exception {
		if (i==words.size()) return null;
		Tkn outcome = words.get(i);				
		Object[] bits = new Object[stream.sig.length];
		for (int j = 0; j < bits.length; j++) {
			Object fj = stream.features[j].extract(stream.source, words, i);
			bits[j] = fj;
		}
		Cntxt context = new Cntxt(true, stream.source, stream.sig, bits);
		i++;
		return new Sitn<Tkn>(outcome, context);
	}
	
}

/**
 * Just a lookup -- Assumes that POS tagging has already been run!
 * @see PosTagByFastTag
 * @see PosTagByOpenNLP
 */
final class FtrPOS implements IFeature {
	@Override
	public Object extract(IDocument source, List<Tkn> words, int i) {
		Tkn w = words.get(i);
		String pos = w.getPOS();
		return pos;
	}	
}

final class FtrLang implements IFeature {

	@Override
	public Object extract(IDocument source, List<Tkn> words, int i) {
		return source.getLang();
	}
	
}

final class FtrPrevWord implements IFeature {	
	private int n;

	public FtrPrevWord(int n) {
		this.n = n;
		assert n > 0 : n;
	}

	@Override
	public Tkn extract(IDocument source, List<Tkn> words, int i) {
		int pi = i - n;
		if (pi<0) return Tkn.START_TOKEN; // Use a special (rare) "start of sentence" token
		return words.get(pi);
	}	
}
final class FtrNextWord implements IFeature {	
	private int n;

	public FtrNextWord(int n) {
		this.n = n;
		assert n > 0;
	}

	@Override
	public Tkn extract(IDocument source, List<Tkn> words, int i) {
		int pi = i + n;
		if (pi >= words.size()) return Tkn.END_TOKEN;
		return words.get(pi);
	}	
}