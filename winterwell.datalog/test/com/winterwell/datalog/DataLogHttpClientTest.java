package com.winterwell.datalog;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class DataLogHttpClientTest {

	@Test
	public void testGetEvents() {
		// get all
		DataLogHttpClient dlc = new DataLogHttpClient(null, "gl");
		List<DataLogEvent> events = dlc.getEvents(null, 5);
		System.out.println(events);
	}

}
