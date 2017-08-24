package com.winterwell.nlp.io;

import java.util.List;

import com.winterwell.maths.IFactory;
import com.winterwell.maths.stats.distributions.cond.Sitn;
import com.winterwell.nlp.io.SitnStream.IFeature;
import com.winterwell.utils.containers.Containers;

/**
 * Used with {@link SitnStream}
 * @author daniel
 *
 */
class CommonFeatureFactory implements IFactory<String, IFeature> {

	@Override
	public IFeature factory(String ftr) {
		// common cases
		// ...previous word?
		if (ftr.startsWith("w-")) {
			int n = Integer.valueOf(ftr.substring(2));
			return new FtrPrevWord(n);
		}
		// ...next word?
		if (ftr.startsWith("w+")) {
			int n = Integer.valueOf(ftr.substring(2));
			return new FtrNextWord(n);
		}
		if (ftr.equals(Tkn.POS.name)) {			
			return new FtrPOS();
		}
		// ...language?
		if ("lang".equals(ftr)) {
			return new FtrLang();
		}
		return null;
	}

	/**
	 * Convenience method, usinf SitnStream to extract a set of features over a list of tokens
	 * @param sig
	 * @param words
	 * @return
	 * @throws Exception
	 */
	public static List<Sitn<Tkn>> getFeatures(String[] sig, ITokenStream words) {
		SitnStream ss = new SitnStream(null, words, sig);
		List<Sitn<Tkn>> list = Containers.getList(ss);
		return list;
	}
	
	@Override
	public boolean isFactory() {
		return true;
	}

}