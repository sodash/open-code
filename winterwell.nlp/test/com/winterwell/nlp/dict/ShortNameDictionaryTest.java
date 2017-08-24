package com.winterwell.nlp.dict;

import com.winterwell.depot.Depot;
import com.winterwell.utils.Printer;
import com.winterwell.utils.log.KErrorPolicy;

import junit.framework.TestCase;

public class ShortNameDictionaryTest extends TestCase {

	public void testSimple() {
		Depot.getDefault().setErrorPolicy(KErrorPolicy.ASK);
		
		ShortNameDictionary dict = new ShortNameDictionary();
		Printer.out(dict.getFile());
		assert dict.contains("bob");
		assert dict.contains("Bob");
		String meaning = dict.getMeaning("bob");
		assert meaning.equals("robert");
		
		{
			String no = dict.getMeaning("Sam");
			assert no=="";
			String[] yes = dict.getMeanings("Sam");
			assert yes.length > 1;
		}
		
		Depot.getDefault().flush();
	}
	
}
