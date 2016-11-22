/**
 * 
 */
package com.winterwell.depot;

import org.junit.Test;

import com.winterwell.utils.time.TUnit;

/**
 * @author daniel
 *
 */
public class RemoteServerHttpStoreTest {

	@Test
	public void testGet() {
		DepotConfig config = new DepotConfig();				
		
		// Use SoDash settings
		config.httpPort = 8101;
		config.https = true;
		config.httpName = "shardwarden@soda.sh";
		config.httpPassword = "syncP1ec3";
		
		RemoteServerHttpStore store = new RemoteServerHttpStore(config);
		
		Desc<String> desc = new Desc<String>("RemoteServerHttpStoreTest", String.class);
		desc.setTag("test");
		desc.setServer(Desc.CENTRAL_SERVER); //"local.soda.sh");
		// Force a refresh
		desc.setMaxAge(TUnit.SECOND.dt);
		
//		Depot.getDefault().put(desc, "Hello There!");
//		Depot.getDefault().flush();
//		Depot.getDefault().close();
		
		String test1 = store.get(desc);
		
		System.out.println(test1);
	}

}
