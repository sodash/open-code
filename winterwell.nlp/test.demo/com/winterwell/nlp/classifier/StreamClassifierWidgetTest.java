package com.winterwell.nlp.classifier;

import java.util.Map;

import org.junit.Test;

import com.winterwell.maths.chart.RenderWithFlot;
import com.winterwell.maths.stats.distributions.cond.Cntxt;
import com.winterwell.maths.stats.distributions.cond.ICondDistribution;
import com.winterwell.maths.stats.distributions.cond.ISitnStream;
import com.winterwell.maths.stats.distributions.cond.UnConditional;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.nlp.corpus.SimpleDocument;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.nlp.io.Token2UnConditionalSitnStream;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.web.WebPage;

public class StreamClassifierWidgetTest {

	@Test
	public void testSimple() {
		WordAndPunctuationTokeniser tok = new WordAndPunctuationTokeniser();
		tok.setLowerCase(true);
		ISitnStream<Tkn> tokeniser = new Token2UnConditionalSitnStream(tok);
		
		ObjectDistribution catModel = new ObjectDistribution();
		catModel.setPseudoCount(1);
		ObjectDistribution pornModel = new ObjectDistribution();
		pornModel.setPseudoCount(1);
		
		Map<String, ICondDistribution<Tkn, Cntxt>> models = new ArrayMap(
				"cats", new UnConditional(catModel), 
				"porn", new UnConditional(pornModel));
		
		StreamClassifier<Tkn> c = new StreamClassifier<Tkn>(tokeniser, models);
		c.resetup();

		SimpleDocument doc1 = new SimpleDocument("cat being cute");
		SimpleDocument doc2 = new SimpleDocument("cute girl being naughty");

		c.train1(doc1, "cats");
		c.train1(doc1, "cats");
		c.train1(doc1, "cats");
		c.train1(doc2, "porn");
		c.train1(doc2, "porn");
		c.train1(doc2, "porn");
		c.finishTraining();

		SimpleDocument doc = new SimpleDocument("cat is cute");

		StreamClassifierWidget w = new StreamClassifierWidget(c, doc);

		WebPage page = new WebPage();
		page.addScript("jquery");
		page.append(RenderWithFlot.DEPENDENCIES);		
		w.appendHtmlTo(page);
		WebUtils.display(page.toString());
	}

}
