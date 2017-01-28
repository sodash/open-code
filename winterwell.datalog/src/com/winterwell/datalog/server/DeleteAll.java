package com.winterwell.datalog.server;

import java.io.File;

import com.winterwell.es.ESUtils;
import com.winterwell.es.client.DeleteRequestBuilder;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.IESResponse;
import com.winterwell.utils.Utils;

public class DeleteAll {

	public static void main(String[] args) {
		DataLogServer.initES();
		ESHttpClient ec = new ESHttpClient(DataLogServer.esconfig);
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
