package com.winterwell.datalog.server;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import com.winterwell.datalog.DataLogConfig;
import com.winterwell.utils.Dep;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.log.Log;
import com.winterwell.web.FakeBrowser;

import ua_parser.Client;
import ua_parser.Parser;

public class LgServletTest {


	@Test
	public void testUTM() {
		Dep.setIfAbsent(DataLogConfig.class, new DataLogConfig());
		{
			Map<String, Object> params = new ArrayMap();		
			String ref = "https://www.drynites.co.uk/?utm_source=mysauce&utm_medium=clairvoyant&utm_campaign=freedom";
			LgServlet.readGoogleAnalyticsTokens(ref, params);
			assert params.get("source").equals("mysauce");
			assert params.get("medium").equals("clairvoyant");
			assert params.get("campaign").equals("freedom");
		}
		{	// malformed
			Map<String, Object> params = new ArrayMap();		
			String ref = "https://www.drynites.co.uk/?utm_source=&utm_campaign";
			LgServlet.readGoogleAnalyticsTokens(ref, params);
			LgServlet.readGoogleAnalyticsTokens("", params);
		}
	}
	
	@Test
	public void testHttp() {
		try {
			DataLogServer dls = new DataLogServer();
			dls.main(null);
		} catch(Throwable ex) {
			Log.d("", ex);
		}
		
		FakeBrowser fb = new FakeBrowser();
		fb.setDebug(true);
		String url = "http://locallg.good-loop.com/lg?t=testevent&d=gl&p=%7B%22pub%22%3A%22www.good-loop.com%22%2C%22bid%22%3A%22bid_ycnlix161420c7431%22%2C%22vert%22%3A%22vert_ikbqiuqf%22%2C%22campaign%22%3A%22Lifecake%22%2C%22variant%22%3A%7B%22adsecs%22%3A15%2C%22banner%22%3A%22default%22%2C%22unitSlug%22%3A%22lifecake%22%7D%2C%22slot%22%3A%22glad0%22%2C%22format%22%3A%22mediumrectangle%22%7D&r=&s=";
		Object ok = fb.getPage(url);
		
	}

	@Test
	public void testParser() throws IOException {
		Parser p = LgServlet.uaParser();
		Client c = p.parse("Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
		assert "Chrome 67".equals(c.userAgent.family+" "+c.userAgent.major);
	}
}
