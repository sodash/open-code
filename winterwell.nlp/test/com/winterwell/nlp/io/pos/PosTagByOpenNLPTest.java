package com.winterwell.nlp.io.pos;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.winterwell.depot.Depot;

import com.winterwell.nlp.io.DumbTokenStream;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.KErrorPolicy;

/**
 * [The, boy, saw, the, man, on, the, hill, with, a, telescope, .]
DT NN VBD DT NN IN DT NN IN DT NN . 
[No, good, will, come, of, it, .]
DT NN MD VB IN PRP . 
[The, cat, sat, on, the, mat, .]
DT NN VBD IN DT NN . 
 * @author daniel
 *
 */
public class PosTagByOpenNLPTest {

	@Test
	public void testGetPossibleTags() {
		PosTagByOpenNLP tagd = new PosTagByOpenNLP(new DumbTokenStream("Hello world :)"));
		
		for(String w_tags : new String[]{			
			"the DT",
			"a DT",
			"cat NN",
			"jump NN,VB",
			"strange JJ",			
			"green JJ,NN"
		}) {
			String word = w_tags.split(" ")[0];
			String[] tags = w_tags.split(" ")[1].split(",");
			List<String> ptags = PosTagByOpenNLP.getPossibleTags(word);
			assert Containers.same(ptags, Arrays.asList(tags)) : w_tags+" v "+ptags;
		}
	}
	
	@Test
	public void testSimple() {
		Depot.getDefault().setErrorPolicy(KErrorPolicy.ASK);
		
		WordAndPunctuationTokeniser base = new WordAndPunctuationTokeniser(
				"The cat sat on the mat.");
		PosTagByOpenNLP tagd = new PosTagByOpenNLP(base);
		List<Tkn> tagged = tagd.toList();
		System.out.println(tagged);
		String tags = "";
		for (Tkn tkn : tagged) {
			tags += tkn.getPOS()+" ";
		}
		System.out.println(tags);
		assert tags.equals("DT NN VBD IN DT NN . ") : tags;
	}

	
	@Test
	public void test2() {
		WordAndPunctuationTokeniser base = new WordAndPunctuationTokeniser(
				"The boy saw the man on the hill with a telescope.");
		PosTagByOpenNLP tagd = new PosTagByOpenNLP(base);
		List<Tkn> tagged = tagd.toList();
		System.out.println(tagged);
		String tags = "";
		for (Tkn tkn : tagged) {
			tags += tkn.getPOS()+" ";
		}
		System.out.println(tags);
	}
	

	@Test
	public void testNoGood() {
		WordAndPunctuationTokeniser base = new WordAndPunctuationTokeniser(
				"No good will come of it.");
		PosTagByOpenNLP tagd = new PosTagByOpenNLP(base);
		List<Tkn> tagged = tagd.toList();
		System.out.println(tagged);
		String tags = "";
		for (Tkn tkn : tagged) {
			tags += tkn.getPOS()+" ";
		}
		System.out.println(tags);
	}

}
