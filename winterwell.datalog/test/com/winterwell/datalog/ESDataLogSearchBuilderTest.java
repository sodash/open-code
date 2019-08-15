package com.winterwell.datalog;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
				new ArrayMap("stats", new ArrayMap("field", "evt"))
				);
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
					"{terms={field=frog, missing=unset}, aggs={by_carrot={terms={field=carrot, missing=unset}, aggs={by_iron={terms={field=iron, missing=unset}}}}}}") : s;
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
