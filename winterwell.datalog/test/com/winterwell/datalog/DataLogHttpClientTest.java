package com.winterwell.datalog;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.utils.Dep;

public class DataLogHttpClientTest {

	@Test
	public void testGetEvents() {
		// get all
		DataLogHttpClient dlc = new DataLogHttpClient(null, new Dataspace("gl"));
		List<DataLogEvent> events = dlc.getEvents(null, 5);
		System.out.println(events);
	}

	@Test
	public void testBreakdown() {
		// get all
		DataLogHttpClient dlc = new DataLogHttpClient(new Dataspace("gl"));
		SearchQuery q = new SearchQuery("evt:spend");
		Breakdown breakdown = new Breakdown("pub", "count", "sum");
		Map<String, Double> events = dlc.getBreakdown(q, breakdown);
		System.out.println(events);
	}

	
	@Test
	public void testBreakdownCount() {
		DataLogHttpClient dlc = new DataLogHttpClient(new Dataspace("gl"));
		SearchQuery sqd = new SearchQuery("evt:donation");
		List<DataLogEvent> donEvents = dlc.getEvents(sqd, 10);
		// NB: the count field is always present, the count stats property is a count of docs
		Breakdown bd = new Breakdown("cid", "count", "count");
		Map<String, Double> dontnForAdvert = dlc.getBreakdown(sqd, bd);	
		System.out.println(dontnForAdvert);
	}
}
