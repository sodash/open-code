package com.goodloop.chat.web;

import java.util.Arrays;

import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.WebEx;

/**
 * Call the Hugging Face Inference API
 * @author daniel
 *
 */
public class HuggingFaceAPIClient {

	public HuggingFaceAPIClient(String apiToken) {
		this.apiToken = apiToken;
	}
	
	String apiToken;
	
//	sentence-transformers/paraphrase-MiniLM-L6-v2
//	modelosanseviero/full-sentence-distillroberta2
	
	String repo;
	String modelName;

	private Dt wait;
	
	public HuggingFaceAPIClient setRepo(String repo) {
		this.repo = repo;
		return this;
	}
	public HuggingFaceAPIClient setModelName(String modelName) {
		this.modelName = modelName;
		return this;
	}
	
	/**
	 * 
	 * @param input
	 * @return
	 * @throws WebEx.E50X "please wait while we load your model"
	 */
	public Object run(String input) throws WebEx.E50X {		
		String API_URL = "https://api-inference.huggingface.co/models/"+repo+"/"+modelName;
		FakeBrowser fb = new FakeBrowser();
		fb.setRequestHeader("Authorization", "Bearer "+apiToken);
		ArrayMap req = new ArrayMap(
			"inputs", input 
		);
		String json = WebUtils2.generateJSON(req);
		
		Time to = wait==null? null : new Time().plus(wait);
		String resp = null;
		// wait and loop if needed
		while(true) {
			try {
				resp = fb.postJsonBody(API_URL, json);
				break;
			} catch(WebEx.E50X waitex) {
				if (to==null || to.isBefore(new Time())) {
					throw waitex;
				}
				Utils.sleep(5000);
			}
		}
		
		Object jobj = WebUtils2.parseJSON(resp);
		return jobj;
	}
	public void setWait(Dt dt) {
		this.wait = dt;
	}
	
}
