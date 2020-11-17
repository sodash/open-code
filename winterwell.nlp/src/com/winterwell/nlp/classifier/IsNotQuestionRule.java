package com.winterwell.nlp.classifier;

import java.util.ArrayList;
import java.util.Collection;

import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.maths.stats.distributions.discrete.Uniform;
import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.nlp.corpus.IDocument;

/**
 * A question must have a question marker -- who/what/where/why/?
 * TODO OR it must have "verb subject", e.g. "Can I go home" (which should still have a ? but might not)
 * <p>
 * The converse is not true: having a question marker does not mean this
 * text must be a question. E.g. rhetorical questions, or using words in a non-questioning manner, "When I get home, I'll..."
 * @author daniel
 * @testedby  IsNotQuestionRuleTest}
 */
public class IsNotQuestionRule extends HardCodedRule<String> {

	static final String question = "question"; // FIXME make into a list
	
	public IsNotQuestionRule(String lang, Collection<String> tags) {
		super(tags);
		assert tags.contains(question) : tags;
		setLang(lang);
		assert lang != null;
	}
	
	@Override
	protected IFiniteDistribution<String> pClassify2(IDocument text) {
		ArrayList noq = new ArrayList(tags);
		noq.remove(question);
		Uniform<String> uni = new Uniform<String>(noq);
		return uni;
	}

	@Override
	protected boolean active(IDocument x) {
		// Not English?
		NLPWorkshop nlpw = NLPWorkshop.get(x.getLang());		
		Collection<String> qwords = nlpw.getQuestionWords();
		if (qwords.isEmpty()) return false;
		// A question must have a question marker -- e.g. who/what/why/?
		// OR it must have "verb subject", e.g. "Can I go home" (which should still have a ? but might not)
		String txt = (x.getName()+" "+x.getContents()).toLowerCase();
		for (String qw : qwords) {
			if (txt.contains(qw)) {
				return false;
			}
		}
		return true;
	}

}
