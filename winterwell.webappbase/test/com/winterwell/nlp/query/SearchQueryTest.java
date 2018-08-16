package com.winterwell.nlp.query;

import java.util.List;

import org.junit.Test;

public class SearchQueryTest {

	@Test
	public void testQuotedKeyVal() {
		SearchQuery sq = new SearchQuery("campaign:\"Villa Plus\"");
		List pt = sq.getParseTree();
		System.out.println(pt);
		String host = sq.getProp("campaign");
		assert host.equals("Villa Plus") : sq;
	}
	
	@Test
	public void testSimpleQuotedTerm() {
		SearchQuery sq = new SearchQuery("\"hello world\"");
		List pt = sq.getParseTree();
		System.out.println(pt);				
		assert pt.toString().equals("[\", hello world]") : pt;
		assert pt.get(1).equals("hello world");
		assert pt.get(0) == SearchQuery.KEYWORD_QUOTED;
	}
	
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
