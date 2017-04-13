package com.winterwell.nlp.corpus.bingliu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.winterwell.nlp.NLPWorkshop;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.LineReader;
import com.winterwell.utils.log.KErrorPolicy;

import com.winterwell.depot.Depot;
import com.winterwell.utils.Printer;

/**
 * Data provided by Bing Liu from http://www.cs.uic.edu/~liub/FBS/sentiment-analysis.html
 * 
 * "License": 
; If you use this list, please cite one of the following two papers:
;
;   Minqing Hu and Bing Liu. "Mining and Summarizing Customer Reviews." 
;       Proceedings of the ACM SIGKDD International Conference on Knowledge 
;       Discovery and Data Mining (KDD-2004), Aug 22-25, 2004, Seattle, 
;       Washington, USA, 
;   Bing Liu, Minqing Hu and Junsheng Cheng. "Opinion Observer: Analyzing 
;       and Comparing Opinions on the Web." Proceedings of the 14th 
;       International World Wide Web conference (WWW-2005), May 10-14, 
;       2005, Chiba, Japan

 * There is no licensing info on the comparative data, which was freely provided online by the collector,
 * & I think can therefore be considered public domain.
 * 
 * Note: this data is not currently used anywhere in SoDash
 * @author daniel
 */
public class BingLiuData {

	public static void main(String[] args) {
		Depot.getDefault().setErrorPolicy(KErrorPolicy.ASK);
		BingLiuData bld = new BingLiuData();
		List<String> pos = bld.getPositiveWords();
		Printer.out(pos);
		List<String> neg = bld.getNegativeWords();
		Printer.out(neg);
		List<String> comp = bld.getComparativeWords();
		Printer.out(comp);
		Printer.out(bld.getComparisonData());
	}
	
	public List<String> getPositiveWords() {
		return getWords("positive");
	}
	
	File getComparisonData() {		
		File file = NLPWorkshop.get("en").getFile("bing-liu-comparison-data.txt");
		System.out.println(file);
		Depot.getDefault().flush();
		return file;
	}
	
	private List<String> getWords(String type) {
		File file = NLPWorkshop.get("en").getFile("bing-liu-"+type+"-words.txt");
//		System.out.println(file);
//		Depot.getDefault().flush();
		LineReader lr = new LineReader(file);
		// strip the preamble
		for (String string : lr) {
			if (Utils.isBlank(string)) break;
		}
		lr.setFresh(true);
		List<String> list = new ArrayList();
		for (String string : lr) {
			list.add(string.toLowerCase().trim());
		}
		FileUtils.close(lr);
		return list;
	}

	public List<String> getNegativeWords() {
		return getWords("negative");
	}
	
	List<String> getComparativeWords() {
		return getWords("comparative");
	}
}
