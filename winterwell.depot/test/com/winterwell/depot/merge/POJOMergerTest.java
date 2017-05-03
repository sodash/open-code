package com.winterwell.depot.merge;

import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.utils.Printer;
import com.winterwell.web.WebPage;

public class POJOMergerTest {

	@Test
	public void testDiff() {
		// what shall we test on? how about a WebPage?
		WebPage before = new WebPage();
		
		WebPage after = new WebPage();
		after.setTitle("After Title");
		after.append("Hello After-World :)");
		
		Merger merger = new Merger();
		Diff diff = merger.diff(before, after);
		Printer.out(diff);
	}

//	@Test
	public void testDoMergePOJO() {
		// what shall we test on? how about a WebPage?
		WebPage before = new WebPage();
		
		WebPage after = new WebPage();
		after.setTitle("After Title");
		after.append("Hello After-World :)");
		
		WebPage latest = new WebPage();
		latest.addStylesheet("mystyle.css");
		
		POJOMerger merger = new POJOMerger(new Merger());
		WebPage m = (WebPage) merger.doMerge(before, after, latest);

		assert m != null;
		assert m.getStylesheets().contains("mystyle.css");
		assert m.getTitle().equals("After Title");
	}

}
