package com.winterwell.web.app;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.winterwell.data.AThing;
import com.winterwell.es.ESKeyword;
import com.winterwell.es.ESNoIndex;
import com.winterwell.es.ESType;
import com.winterwell.es.client.query.BoolQueryBuilder;
import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.utils.containers.Containers;

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

	@Test
	public void testESMapping() {
		Map mapping = new ESType().object().property("foo", ESType.keyword);
		ESType est = AppUtils.estypeForClass(TestThing.class, mapping);		
		System.out.println(est);
		Map props = (Map) est.get("properties");
		Map c1 = (Map) props.get("child");
		Map c2 = (Map) props.get("child2");
		assert Containers.same(c1, c2);
	}
	
}

class TestThing extends AThing {
	
	String texty;
	
	@ESKeyword
	String keyy;
	
	String foo;
	
	double d;
	
	Integer i;
	
	TestThing child;
	
	TestThing child2;
	
	@ESNoIndex
	TestThing ignored;
}