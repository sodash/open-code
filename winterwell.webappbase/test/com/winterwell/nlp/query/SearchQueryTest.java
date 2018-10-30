package com.winterwell.nlp.query;

import java.util.List;

import org.junit.Test;
/**
 * NB in webappbase project cos so is searchquery
 * @author mark
 *
 */
public class SearchQueryTest {

	@Test
	public void testKeyNull() {
		{
			SearchQuery sq = new SearchQuery("alice foo:null");
			List pt = sq.getParseTree();
			System.out.println(pt);
			assert pt.toString().equals("[and, alice]") : sq;
			assert sq.matches("Hello alice :)");
		}
		{
			SearchQuery sq = new SearchQuery("foo:null alice");
			List pt = sq.getParseTree();
			System.out.println(pt);
			assert pt.toString().equals("[and, alice]") : sq;
			assert sq.matches("Hello alice :)");
		}

		{
			SearchQuery sq = new SearchQuery("name:null foo:null");
			List pt = sq.getParseTree();
			System.out.println(pt);
			assert pt.toString().equals("[and]") : pt;
			assert sq.matches("Hello alice :)");
		}
	}

	
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
	
	@Test
	public void testCombineWithAND() {
		String baseq = "user:ww@trk OR user:mark@winterwell.com@email";
		String extraq = "evt:spend OR evt:spendadjust OR evt:donation";
		SearchQuery base = new SearchQuery(baseq);
		SearchQuery extra = new SearchQuery(base, extraq);
		assert extra.getRaw().equals("(user:ww@trk OR user:mark@winterwell.com@email) AND (evt:spend OR evt:spendadjust OR evt:donation)");
	}
	
//	@Test
//	public void testBrackets() {
//		String baseq = "user:wwvyfncgobrxvwqablhe@trk";
//		SearchQuery sq = new SearchQuery("blah");
//		sq.bracket()
//	}

}
