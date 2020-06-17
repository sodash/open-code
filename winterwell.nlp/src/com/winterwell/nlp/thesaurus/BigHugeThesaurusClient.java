package com.winterwell.nlp.thesaurus;

import com.winterwell.json.JSONObject;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.KErrorPolicy;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.LoginDetails;
import com.winterwell.web.app.Logins;

/**
 * 
 * License: 
 * You may use the service for any legal and non-evil purpose* so long as you link to this site in your website or application credits as follows: 
 * Thesaurus service provided by words.bighugelabs.com
 * @author daniel
 *
 */
public class BigHugeThesaurusClient {

	
	public ThesuarusLookup get(String word) {		
		LoginDetails ld = Logins.get("words.bighugelabs.com");
		
		assert ! Utils.isBlank(word);
		word = StrUtils.normalise(word, KErrorPolicy.ACCEPT);
		FakeBrowser fb = new FakeBrowser();
		String json = fb.getPage("http://words.bighugelabs.com/api/2/"+ld.apiKey+"/"+word+"/json");
		JSONObject jobj = new JSONObject(json);
		return new ThesuarusLookup(word, jobj);
	}
	
}
