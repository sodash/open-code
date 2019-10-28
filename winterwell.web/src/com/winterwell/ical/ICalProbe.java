package com.winterwell.ical;


import java.util.List;

import com.winterwell.web.FakeBrowser;

/**
 * TODO see #6463
 * 
 * 
 * @author Daniel
 * @testedby ICalProbeTest
 */
public class ICalProbe {
	
	private static final long serialVersionUID = 1L;
	private static final String LOGTAG = "ical";
	private String url;
	public String calName;
	
	protected List<ICalEvent> run() throws Exception {
		FakeBrowser fb = new FakeBrowser();
		String ical = fb.getPage(url);
		ICalReader r = new ICalReader(ical);
		List<ICalEvent> events = r.getEvents();
		calName = r.getCalendarName();
		return events;		
	}


}
