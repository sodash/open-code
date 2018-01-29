package com.winterwell.datalog.server;

import static org.junit.Assert.*;

import org.junit.Test;

import com.winterwell.web.FakeBrowser;

public class LgServletTest {

	@Test
	public void testHttp() {
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(true);
		String url = "http://locallg.good-loop.com/lg?t=testevent&d=gl&p=%7B%22pub%22%3A%22www.good-loop.com%22%2C%22bid%22%3A%22bid_ycnlix161420c7431%22%2C%22vert%22%3A%22vert_ikbqiuqf%22%2C%22campaign%22%3A%22Lifecake%22%2C%22variant%22%3A%7B%22adsecs%22%3A15%2C%22banner%22%3A%22default%22%2C%22unitSlug%22%3A%22lifecake%22%7D%2C%22slot%22%3A%22glad0%22%2C%22format%22%3A%22mediumrectangle%22%7D&r=&s=";
		Object ok = fb.getPage(url);
		
	}

}
