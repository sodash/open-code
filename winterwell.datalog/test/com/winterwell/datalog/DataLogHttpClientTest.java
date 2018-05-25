package com.winterwell.datalog;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class DataLogHttpClientTest {

	@Test
	public void testGetEvents() {
		DataLogHttpClient dlc = new DataLogHttpClient("gl");
		List<DataLogEvent> events = dlc.getEvents(null, 5);
		System.out.println(events);
	}

}
