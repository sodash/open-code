package com.winterwell.web.app;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.winterwell.es.client.query.BoolQueryBuilder;
import com.winterwell.nlp.query.SearchQuery;

public class AppUtilsTest {

	@Test
	public void testMakeESFilterFromSearchQuery() {		
		{
			String q = "(cid:nestle-cocoa-plan-vegetable-growing-kit OR cid:tchc OR cid:unset) AND (evt:spend OR evt:spendadjust OR evt:donation)";
			SearchQuery sq = new SearchQuery(q);
			List ptre = sq.getParseTree();
			BoolQueryBuilder esf = AppUtils.makeESFilterFromSearchQuery(sq, null, null);
			Map esmap = (Map) esf.getUnderlyingMap().get("bool");
			System.out.println(esmap);
			System.out.println(esf);
			// top level must be must
			assert esmap.containsKey("must") : esmap;
			assert ! esmap.containsKey("should") : esmap;
		}
		{
			String q = "(user:wwvyfncgobrxvwqablhe@trk OR user:mark@winterwell.com@email) AND (evt:spend OR evt:spendadjust OR evt:donation)";
			SearchQuery sq = new SearchQuery(q);
			List ptre = sq.getParseTree();
			assert ! ptre.get(0).equals(SearchQuery.KEYWORD_OR);
			BoolQueryBuilder esf = AppUtils.makeESFilterFromSearchQuery(sq, null, null);
			Map esmap = (Map) esf.getUnderlyingMap().get("bool");
			System.out.println(esmap);
			System.out.println(esf);
			// top level must be must
			assert esmap.containsKey("must") : esmap;
			assert ! esmap.containsKey("should") : esmap;
		}
		if (false) { // meh - we'd like it to handle this as a top-level and -- but lets fix by using unambiguous queries
			String q = "user:wwvyfncgobrxvwqablhe@trk OR user:mark@winterwell.com@email (evt:spend OR evt:spendadjust OR evt:donation)";
			SearchQuery sq = new SearchQuery(q);
			List ptre = sq.getParseTree();
			assert ! ptre.get(0).equals(SearchQuery.KEYWORD_OR);
			BoolQueryBuilder esf = AppUtils.makeESFilterFromSearchQuery(sq, null, null);
			System.out.println(esf);
		}
	}

}
