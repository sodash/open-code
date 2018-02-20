package com.winterwell.nlp.query;

import java.util.List;

import org.junit.Test;

import com.winterwell.nlp.query.SearchQuery;

public class SearchQueryTest {

	@Test
	public void testSimple() {
		SearchQuery sq = new SearchQuery("host:localpub.com");
		List pt = sq.getParseTree();
		System.out.println(pt);
		String host = sq.getProp("host");
		assert host.equals("localpub.com") : sq;
	}
	

	@Test
	public void testWordsKeyVal() {
		SearchQuery sq = new SearchQuery("hello world host:localpub.com");
		List pt = sq.getParseTree();
		System.out.println(pt);
		String host = sq.getProp("host");
		assert host.equals("localpub.com") : sq;
	}
	
	@Test
	public void testBadSyntax() {
		try {
			SearchQuery sq = new SearchQuery("hello OR");
			List pt = sq.getParseTree();
			assert false;
		} catch(Exception ex) {
			// ok
		}
	}

	
	@Test
	public void testKeyValKeyVal() {
		SearchQuery sq = new SearchQuery("vert:cadburys host:localpub.com");
		List pt = sq.getParseTree();
		System.out.println(pt);
		String host = sq.getProp("host");
		assert host.equals("localpub.com") : sq;
	}

}
