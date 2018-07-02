package com.winterwell.depot.merge;

import java.util.Map;

import org.junit.Test;

import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.web.WebPage;

public class MergerTest {

	
	@Test
	public void testDoMergePOJO() {
		Merger merger = new Merger();
		POJOMerger pmerger = new POJOMerger(merger);
		merger.addMerge(WebPage.class, pmerger);
		
		// what shall we test on? how about a WebPage?
		WebPage before = new WebPage();
		
		WebPage after = new WebPage();
		after.setTitle("After Title");
		after.append("Hello After-World :)");
		
		WebPage latest = new WebPage();
		latest.addStylesheet("mystyle.css");
		
		WebPage m = (WebPage) merger.doMerge(before, after, latest);

		assert m != null;
		assert m.getStylesheets().contains("mystyle.css") : m.getStylesheets();
		assert m.getTitle().equals("After Title");
	}
	
	@Test
	public void testDoMergeCustom() {
		// what shall we test on? how about a WebPage?
		WebPage before = new WebPage();
		
		WebPage after = new WebPage();
		after.setTitle("After Title");
		after.append("Hello After-World :)");
		
		WebPage latest = new WebPage();
		latest.addStylesheet("mystyle.css");
		
		Merger merger = new Merger();
		IMerger noop = new UseLatestMerger();
		merger.addMerge(StringBuilder.class, noop);
		merger.addMerge(String.class, noop);
		merger.addMerge(WebPage.class, new POJOMerger(merger));
		
		WebPage m = (WebPage) merger.doMerge(before, after, latest);

		assert m != null;
		assert m.getStylesheets().contains("mystyle.css") : m.getStylesheets();
		assert m.getTitle().equals("After Title");
	}


	@Test
	public void testDoMergeMap() {
		// what shall we test on? how about a WebPage?
		Map before = new ArrayMap("before", 1);
		
		ArrayMap after = new ArrayMap();
		after.put("title", "After Title");
		after.put("body", new ArrayMap("text", "Hello After-World :)"));
		
		ArrayMap latest = new ArrayMap();
		latest.put("css", "mystyle.css");
		latest.put("before", "not");
		
		Merger merger = new Merger();
		ArrayMap m = (ArrayMap) merger.doMerge(before, after, latest);

		assert m != null;
		System.out.println(m);
	}

}
