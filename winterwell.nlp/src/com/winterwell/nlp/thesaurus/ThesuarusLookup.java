package com.winterwell.nlp.thesaurus;

import com.winterwell.json.JSONObject;

public class ThesuarusLookup {

	private JSONObject base;
	private String word;

	ThesuarusLookup(String word, JSONObject jobj) {
		this.word = word;
		this.base = jobj;
	}

	@Override
	public String toString() {	
		return "ThesuarusLookup["+word+"->"+base+"]";
	}
	
}
