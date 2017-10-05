package com.winterwell.datalog.server;

import java.io.File;

import com.winterwell.es.client.DeleteRequestBuilder;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.IESResponse;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.ConfigBuilder;

public class DeleteAll {

	public static void main(String[] args) {		
		ESConfig esconfig = new ConfigBuilder(new ESConfig())
				.set(new File("config/datalog.properties"))
				.setFromMain(args).get();
		ESHttpClient ec = new ESHttpClient(esconfig);
		DeleteRequestBuilder del = new DeleteRequestBuilder(ec);
		del.setIndex("assist");
		del.setType("experiment");
		IESResponse r = del.get();
		System.out.println(r);
		Utils.sleep(1000);
		ec.close();
		Utils.sleep(1000);
	}

}
