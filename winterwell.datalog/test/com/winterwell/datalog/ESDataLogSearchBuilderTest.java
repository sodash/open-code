package com.winterwell.datalog;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;
import org.junit.Test;

import com.winterwell.datalog.server.DataServletTest;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.agg.Aggregation;
import com.winterwell.es.client.query.BoolQueryBuilder;
import com.winterwell.es.client.query.ESQueryBuilders;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.SysOutCollectorStream;

/**
 * See also {@link DataServletTest}
 * @author daniel
 *
 */
public class ESDataLogSearchBuilderTest {

	@Test
	public void testSumAll() {
		ESHttpClient esc = new ESHttpClient(new ESConfig());
		ESDataLogSearchBuilder esdsb = new ESDataLogSearchBuilder(esc, new Dataspace("test"));
		
		List<String> breakdown = Arrays.asList("{\"dntn\":\"sum\"}");
		esdsb.setBreakdown(breakdown);
		
		List<Aggregation> aggs = esdsb.prepareSearch2_aggregations();
		String s = Printer.toString(aggs, "\n");
		assert aggs.size() == 2;
		Aggregation a0 = aggs.get(0);
		Printer.out(a0.toJson2());
		assert ! s.contains("by_");
	}

	
	@Test
	public void testClean() {
		{	// no breakdown
			String json = "{'all':{'min':1.0,'avg':1.0,'max':1.0,'count':25.0,'sum':25.0},'no0_0':{'doc_count':25.0,'count':{'min':1.0,'avg':1.0,'max':1.0,'count':25.0,'sum':25.0}},'examples':[{'_index':'datalog.testspace_aug19','_type':'evt','_source':{'evt':['unittest'],'count':1.0,'host':'localhost','time':'2019-08-15T13:54:21Z','user':'DataServletTest@bot','props':[{'v':'init','k':'stage'}]},'_id':'testspace_unittest_fb628568b1cd43e736cbd9041a74f3a6_771','_score':1.0},{'_index':'datalog.testspace_aug19','_type':'evt','_source':{'evt':['unittest'],'count':1.0,'host':'localhost','time':'2019-08-16T10:23:10Z','user':'DataServletTest@bot','props':[{'v':'init','k':'stage'}]},'_id':'testspace_unittest_fb628568b1cd43e736cbd9041a74f3a6_507','_score':1.0},{'_index':'datalog.testspace_aug19','_type':'evt','_source':{'evt':['unittest'],'count':1.0,'host':'localhost','time':'2019-08-16T10:27:20Z','user':'DataServletTest@bot','props':[{'v':'init','k':'stage'}]},'_id':'testspace_unittest_fb628568b1cd43e736cbd9041a74f3a6_99','_score':1.0},{'_index':'datalog.testspace_aug19','_type':'evt','_source':{'evt':['unittest'],'count':1.0,'host':'localhost','time':'2019-08-16T10:42:36Z','user':'DataServletTest@bot','props':[{'v':'init','k':'stage'}]},'_id':'testspace_unittest_fb628568b1cd43e736cbd9041a74f3a6_836','_score':1.0},{'_index':'datalog.testspace_aug19','_type':'evt','_source':{'evt':['unittest'],'count':1.0,'host':'localhost','time':'2019-08-16T10:43:31Z','user':'DataServletTest@bot','props':[{'v':'init','k':'stage'}]},'_id':'testspace_unittest_fb628568b1cd43e736cbd9041a74f3a6_827','_score':1.0},{'_index':'datalog.testspace_aug19','_type':'evt','_source':{'evt':['unittest'],'count':1.0,'host':'localhost','time':'2019-08-16T11:16:33Z','user':'DataServletTest@bot','props':[{'v':'init','k':'stage'}]},'_id':'testspace_unittest_fb628568b1cd43e736cbd9041a74f3a6_370','_score':1.0},{'_index':'datalog.testspace_aug19','_type':'evt','_source':{'evt':['unittest'],'count':1.0,'host':'localhost','time':'2019-08-16T11:33:51Z','user':'DataServletTest@bot','props':[{'v':'init','k':'stage'}]},'_id':'testspace_unittest_fb628568b1cd43e736cbd9041a74f3a6_615','_score':1.0},{'_index':'datalog.testspace_aug19','_type':'evt','_source':{'evt':['unittest'],'count':1.0,'host':'localhost','time':'2019-08-15T13:49:32Z','user':'DataServletTest@bot','props':[{'v':'init','k':'stage'}]},'_id':'testspace_unittest_fb628568b1cd43e736cbd9041a74f3a6_316','_score':1.0},{'_index':'datalog.testspace_aug19','_type':'evt','_source':{'evt':['unittest'],'count':1.0,'host':'localhost','time':'2019-08-15T16:12:35Z','user':'DataServletTest@bot','props':[{'v':'init','k':'stage'}]},'_id':'testspace_unittest_fb628568b1cd43e736cbd9041a74f3a6_253','_score':1.0},{'_index':'datalog.testspace_aug19','_type':'evt','_source':{'evt':['unittest'],'count':1.0,'host':'localhost','time':'2019-08-16T11:21:01Z','user':'DataServletTest@bot','props':[{'v':'init','k':'stage'}]},'_id':'testspace_unittest_fb628568b1cd43e736cbd9041a74f3a6_374','_score':1.0}]}".replace('\'', '"');
			ESHttpClient esc = new ESHttpClient(new ESConfig());
			ESDataLogSearchBuilder esdsb = new ESDataLogSearchBuilder(esc, new Dataspace("test"));
			Map aggregations = (Map) JSON.parse(json);
			Map clean = esdsb.cleanJson(aggregations);
			String cj = JSON.toString(clean);
			assert ! cj.contains("no0") : cj;
		}
		{
			String json = "{'buckets':[{'no0_0':{'doc_count':20.0,'count':{'min':1.0,'avg':1.0,'max':1.0,'count':20.0,'sum':20.0}},'doc_count':20.0,'key':'unittest'}]}".replace('\'', '"');
			ESHttpClient esc = new ESHttpClient(new ESConfig());
			ESDataLogSearchBuilder esdsb = new ESDataLogSearchBuilder(esc, new Dataspace("test"));
			Map aggregations = (Map) JSON.parse(json);
			Map clean = esdsb.cleanJson(aggregations);
			String cj = JSON.toString(clean);
			assert ! cj.contains("no0") : cj;
		}
		{
			String json = "{'evt':{'min':1.0,'avg':1.0,'max':1.0,'count':20.0,'sum':20.0},'by_evt':{'doc_count_error_upper_bound':0.0,'sum_other_doc_count':0.0,'buckets':[{'no0_0':{'doc_count':20.0,'count':{'min':1.0,'avg':1.0,'max':1.0,'count':20.0,'sum':20.0}},'doc_count':20.0,'key':'unittest'}]}}".replace('\'', '"');
			ESHttpClient esc = new ESHttpClient(new ESConfig());
			ESDataLogSearchBuilder esdsb = new ESDataLogSearchBuilder(esc, new Dataspace("test"));
			Map aggregations = (Map) JSON.parse(json);
			Map clean = esdsb.cleanJson(aggregations);
			String cj = JSON.toString(clean);
			assert ! cj.contains("no0") : cj;
		}
	
	}
	
	@Test
	public void testSetBreakdownSimple() {
		ESHttpClient esc = new ESHttpClient(new ESConfig());
		ESDataLogSearchBuilder esdsb = new ESDataLogSearchBuilder(esc, new Dataspace("test"));
		
		List<String> breakdown = Arrays.asList("evt");
		esdsb.setBreakdown(breakdown);
		
		List<Aggregation> aggs = esdsb.prepareSearch2_aggregations();
		String s = Printer.toString(aggs, "\n");
		Printer.out("evt:	"+s);
		assert aggs.size() == 2;
		Aggregation a0 = aggs.get(0);
		Printer.out(a0.toJson2());
		assert Containers.same(a0.toJson2(), 
				new ArrayMap("terms", new ArrayMap("field", "evt", "missing", "unset"))
				);
		Aggregation a1 = aggs.get(1);
		Printer.out(a1.toJson2());
		assert Containers.same(a1.toJson2(), 
				new ArrayMap("stats", new ArrayMap("field", "count"))
				);
	}

	@Test
	public void testSetBreakdownOverlap() {
		ESHttpClient esc = new ESHttpClient(new ESConfig());
		ESDataLogSearchBuilder esdsb = new ESDataLogSearchBuilder(esc, new Dataspace("test"));
		
		List<String> breakdown = Arrays.asList("evt/host", "evt/user");
		esdsb.setBreakdown(breakdown);

		// TODO it would be nice if the parser was smart enough to merge these into a tree. Oh well.
		
		List<Aggregation> aggs = esdsb.prepareSearch2_aggregations();
		String s = Printer.toString(aggs, "\n");
		Printer.out("evt:	"+s);
		ArrayList<Object> names = Containers.apply(aggs,  agg -> agg.name);
		assert names.size() == new HashSet(names).size() : names;
		assert aggs.size() == 3;
	}


	@Test
	public void testSetBreakdownAByB() {
		ESHttpClient esc = new ESHttpClient(new ESConfig());
		ESDataLogSearchBuilder esdsb = new ESDataLogSearchBuilder(esc, new Dataspace("test"));
		
		{
			List<String> breakdown = Arrays.asList("evt/time");
			esdsb.setBreakdown(breakdown);
			List<Aggregation> aggs = esdsb.prepareSearch2_aggregations();
			Map s = aggs.get(0).toJson2();
			Printer.out("evt/time:	"+s);
			assert s.toString().equals(
					"{terms={field=evt, missing=unset}, aggs={by_time={date_histogram={field=time, interval=day}}}}") : s;
		}
		{	
			List<String> breakdown = Arrays.asList("frog/carrot/iron");
			esdsb.setBreakdown(breakdown);
			List<Aggregation> aggs = esdsb.prepareSearch2_aggregations();
			Map s = aggs.get(0).toJson2();
			Printer.out("\n"+breakdown+":	"+s);
			assert s.toString().equals(
					"{terms={field=frog, missing=unset}, aggs={by_carrot_iron={terms={field=carrot, missing=unset}, aggs={by_iron={terms={field=iron, missing=unset}}}}}}") : s;
		}
		{	
			List<String> breakdown = Arrays.asList("animal/vegetable {\"mycount\": \"avg\"}");
			esdsb.setBreakdown(breakdown);
			List<Aggregation> aggs = esdsb.prepareSearch2_aggregations();
			Map s = aggs.get(0).toJson2();
			Printer.out("\n"+breakdown+":	"+s);
		}
	}
}
