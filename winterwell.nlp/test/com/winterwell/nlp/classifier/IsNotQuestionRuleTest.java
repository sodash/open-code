package com.winterwell.nlp.classifier;

import java.util.Arrays;

import org.junit.Test;

import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.nlp.corpus.SimpleDocument;

public class IsNotQuestionRuleTest {

	@Test
	public void testActive() {		
		IsNotQuestionRule rule = new IsNotQuestionRule("en", Arrays.asList("question", "insult"));
		SimpleDocument d1 = new SimpleDocument("Why is the sky blue?");
		SimpleDocument d2 = new SimpleDocument("Can I kick it?");
		SimpleDocument d3 = new SimpleDocument("Yo mamma's so fat, I swerved to avoid her in the street & I ran out of petrol!");
		assert ! rule.active(d1);
		assert ! rule.active(d2);
		assert rule.active(d3);
	}

	@Test
	public void testPClassify() {
		IsNotQuestionRule rule = new IsNotQuestionRule("en", Arrays.asList("question", "insult", "praise"));
		SimpleDocument d1 = new SimpleDocument("Why is the sky blue?");
		SimpleDocument d2 = new SimpleDocument("Can I kick it?");
		SimpleDocument d3 = new SimpleDocument("Yo mamma's so fat, I swerved to avoid her in the street & I ran out of petrol!");
		
		IFiniteDistribution<String> pTag1 = rule.pClassify(d1);
		IFiniteDistribution<String> pTag2 = rule.pClassify(d2);
		IFiniteDistribution<String> pTag3 = rule.pClassify(d3);
		System.out.println(pTag1);
		System.out.println(pTag3);
		assert pTag1==null || pTag1.prob("question") == 1.0/3;
		assert pTag3.prob("question") == 0;
		assert pTag3.prob("insult") == 1.0/2;
	}
	

	@Test
	public void testPClassifySoftened() {
		IsNotQuestionRule rule = new IsNotQuestionRule("en", Arrays.asList("question", "insult", "praise"));
				
		SimpleDocument d1 = new SimpleDocument("Why is the sky blue?");
		SimpleDocument d2 = new SimpleDocument("Can I kick it?");
		SimpleDocument d3 = new SimpleDocument("Yo mamma's so fat, I swerved to avoid her in the street & I ran out of petrol!");
		
		rule.train1(d3, "insult");
		{
			IFiniteDistribution<String> pTag1 = rule.pClassify(d1);
			IFiniteDistribution<String> pTag2 = rule.pClassify(d2);
			IFiniteDistribution<String> pTag3 = rule.pClassify(d3);
			System.out.println(pTag1);
			System.out.println(pTag3);
			assert pTag1==null || pTag1.prob("question") == 1.0/3;
			assert pTag3.prob("question") == 0;
			assert pTag3.prob("insult") == 1.0/2;
		}
		
		rule.train1(d3, "question");
		{
			IFiniteDistribution<String> pTag1 = rule.pClassify(d1);
			IFiniteDistribution<String> pTag2 = rule.pClassify(d2);
			IFiniteDistribution<String> pTag3 = rule.pClassify(d3);
			System.out.println(pTag1);
			System.out.println(pTag3);
			assert pTag1==null || pTag1.prob("question") == 1.0/3;
			assert pTag3.prob("question") > 0 && pTag3.prob("question") < 1.0/3 : pTag3;			
			assert pTag3.prob("insult") < 1.0/2 && pTag3.prob("insult") > 1.0/3;
		}
	}

}
