package com.winterwell.web.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.winterwell.data.AThing;
import com.winterwell.data.KStatus;
import com.winterwell.es.ESKeyword;
import com.winterwell.es.ESNoIndex;
import com.winterwell.es.ESType;
import com.winterwell.es.IESRouter;
import com.winterwell.es.StdESRouter;
import com.winterwell.es.XIdTypeAdapter;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.KRefresh;
import com.winterwell.es.client.query.BoolQueryBuilder;
import com.winterwell.gson.Gson;
import com.winterwell.gson.GsonBuilder;
import com.winterwell.gson.KLoopPolicy;
import com.winterwell.gson.StandardAdapters;
import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.web.ajax.JThing;
import com.winterwell.web.data.XId;

public class AppUtilsTest {

	@Test
	public void testMakeESFilterFromSearchQuery_regex() {
		String q = "user:/.+@trk/"; // filter anon trk users
		SearchQuery sq = new SearchQuery(q);
		List ptre = sq.getParseTree();
		BoolQueryBuilder esf = AppUtils.makeESFilterFromSearchQuery(sq, null, null);
		Map esmap = (Map) esf.getUnderlyingMap().get("bool");
		System.out.println(esmap);
		System.out.println(esf);
		assert esmap.toString().equals("{must=[{regexp={user={value=.+@trk}}}]}") : esmap.toString();
	}
	
	@Test
	public void testMakeESFilterFromSearchQuery_dueBefore() {
		String q = "due:before:2020-01-01";
		SearchQuery sq = new SearchQuery(q);
		List ptre = sq.getParseTree();
		assert ptre.toString().equals("[and, {due={before=2020-01-01}}]") : ptre;
		BoolQueryBuilder esf = AppUtils.makeESFilterFromSearchQuery(sq, null, null);
		Map esmap = (Map) esf.getUnderlyingMap().get("bool");
		System.out.println(esmap);
		System.out.println(esf);
		assert esmap.toString().equals("TODO");
	}
	
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
		assert props.containsKey("@class");
		Object c1c = SimpleJson.get(c1, "properties", "@class");
		assert c1c != null : c1;
	}
	
	@Test
	public void testESMapping_inUse() {
		IESRouter router = Dep.setIfAbsent(IESRouter.class, new StdESRouter());
		Dep.setIfAbsent(ESConfig.class, new ESConfig());
		Dep.setIfAbsent(ESHttpClient.class, new ESHttpClient());
		
		Gson gson = new GsonBuilder()
		.setLenientReader(true)
		.registerTypeAdapter(Time.class, new StandardAdapters.TimeTypeAdapter())
		.registerTypeAdapter(XId.class, new XIdTypeAdapter())
		.registerTypeAdapter(long.class, new StandardAdapters.LenientLongAdapter(0l))
		.serializeSpecialFloatingPointValues()		
		.setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
		.setLoopPolicy(KLoopPolicy.QUIET_NULL)
		.create();
		Dep.setIfAbsent(Gson.class, gson);
		
		KStatus[] statuses = new KStatus[] {KStatus.PUBLISHED};
		Class[] dbclasses = new Class[] {TestThing.class};
		AppUtils.initESIndices(statuses, dbclasses);
		AppUtils.initESMappings(statuses, dbclasses, new ArrayMap());
		Utils.sleep(500);
		
		TestThing item = new TestThing();
		item.id = "mytestid";
		item.i = 7;
		item.child = new TestThing();
		item.child.setName("kiddy");
		item.ids = new ArrayList();
		item.ids.add("id1");
		item.ids.add("id2");
		
		JThing pubd = AppUtils.doPublish(item, KRefresh.TRUE, false);
		System.out.println(pubd);
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
	
	@ESKeyword
	List<String> ids;
}