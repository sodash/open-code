package com.winterwell.depot;

import java.util.Map;

import org.junit.Test;

import com.winterwell.depot.merge.Merger;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.gson.FlexiGson;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;

public class ESStoreTest {

	
	@Test
	public void testSimple() {
		Dep.setIfAbsent(FlexiGson.class, new FlexiGson());
		Dep.setIfAbsent(Merger.class, new Merger());
		Dep.setIfAbsent(ESConfig.class, new ESConfig());
		ESConfig esconfig = Dep.get(ESConfig.class);
		if ( ! Dep.has(ESHttpClient.class)) Dep.setSupplier(ESHttpClient.class, false, ESHttpClient::new);
		Dep.setIfAbsent(DepotConfig.class, new DepotConfig());

		ArrayMap artifact = new ArrayMap("a", "apple", "b", "bee");
		Desc desc = new Desc("test-simple", Map.class);
		ESDepotStore store = new ESDepotStore();
		store.init();
		store.put(desc, artifact);
		Utils.sleep(1500);
		
		Object got = store.get(desc);
		assert Utils.equals(artifact, got) : got;
	}
	
}
