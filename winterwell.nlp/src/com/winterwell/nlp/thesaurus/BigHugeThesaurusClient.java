package com.winterwell.nlp.thesaurus;

import com.winterwell.json.JSONObject;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.KErrorPolicy;
import com.winterwell.web.FakeBrowser;

/**
 * 
 * License: 
 * You may use the service for any legal and non-evil purpose* so long as you link to this site in your website or application credits as follows: 
 * Thesaurus service provided by words.bighugelabs.com
 * @author daniel
 *
 */
public class BigHugeThesaurusClient {

	// API details:
	// u: info@winterwell.com
	// p: Boog1eWoog1e	
	//You can access your admin page here: http://words.bighugelabs.com/admin/16f53732df3b21487f3da8b21095fc92
	String API_KEY = "16f53732df3b21487f3da8b21095fc92";
	
	
	public ThesuarusLookup get(String word) {
		assert ! Utils.isBlank(word);
		word = StrUtils.normalise(word, KErrorPolicy.ACCEPT);
		FakeBrowser fb = new FakeBrowser();
		String json = fb.getPage("http://words.bighugelabs.com/api/2/"+API_KEY+"/"+word+"/json");
		JSONObject jobj = new JSONObject(json);
		return new ThesuarusLookup(word, jobj);
	}
	
}
