package com.winterwell.nlp.dict;

import junit.framework.TestCase;
import com.winterwell.utils.containers.ListMap;
import com.winterwell.utils.log.KErrorPolicy;

import com.winterwell.depot.Depot;
import com.winterwell.utils.Printer;

public class EmoticonDictionaryTest extends TestCase {

	public void testSimple() {
		Depot.getDefault().setErrorPolicy(KErrorPolicy.ASK);
		
		EmoticonDictionary dict = new EmoticonDictionary();
		Printer.out(dict.getFile());
		assert dict.contains(":)");
		String meaning = dict.getMeaning(":)");
		assert meaning.equals("happy");
	}
	
	public void testSad() {
		Depot.getDefault().setErrorPolicy(KErrorPolicy.ASK);
		
		EmoticonDictionary dict = new EmoticonDictionary();
		ListMap<String,String> meaning2words = new ListMap(); 
		for (String w : dict) {
			meaning2words.add(dict.getMeaning(w), w);
		}
		System.out.println(meaning2words.keySet());
		System.out.println(meaning2words.get("sad"));
		System.out.println(meaning2words.get("crying"));
	}


	public void testPosNeg() {
		Depot.getDefault().setErrorPolicy(KErrorPolicy.ASK);		
		EmoticonDictionary dict = new EmoticonDictionary();
		Printer.out(dict.getFile());
		ListMap m2t = new ListMap();
		for (String string : dict) {
			String m = dict.getMeaning(string);
			m2t.add(m, string);
		}
		System.out.println(m2t);
	}
}
