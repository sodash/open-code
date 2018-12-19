/**
 * 
 */
package com.winterwell.depot;

import java.io.File;
import java.util.Set;

import org.junit.Test;

/**
 * @author daniel
 *
 */
public class FileStoreTest {

	@SuppressWarnings("rawtypes")
	@Test
	public void testLoadKeys() {
		File testDir = new File("test/temp").getAbsoluteFile();
		FileStore fs = new FileStore(new DepotConfig().setDir(testDir));
		
		Desc<String> desc1 = new Desc("Test1", String.class);
		desc1.setTag("test");
		desc1.put("a", 1);
		fs.put(desc1, "artifact1");
		
		Desc<String> desc2 = new Desc("Test2", String.class);
		desc2.setTag("test");
		desc2.put("a", 1);
		desc2.put("b", 2);
		fs.put(desc2, "artifact2");		
		
		Set<Desc> keys1 = fs.loadKeys(desc1);
		Set<Desc> keys2 = fs.loadKeys(desc2);
		
		Desc<String> pdesc = new Desc(null, String.class);
		pdesc.setTag("test");
		pdesc.put("a", 1);
		Set<Desc> keys3 = fs.loadKeys(pdesc);
		
		assert keys1.contains(desc1) : keys1;
		assert ! keys1.contains(desc2);
		
		assert keys2.contains(desc2);
		assert ! keys2.contains(desc1);
		
		assert keys3.contains(desc1);
		assert keys3.contains(desc2);
	}

}
