package com.winterwell.depot;

import org.junit.Test;

import com.winterwell.utils.Utils;

public class DescCacheTest {

	
	/**
	 * Test one of DescCache's guarantees: an equals Desc will
	 * get the same object.
	 */
	@Test
	public void testGetArtifact() {
		DescCache dc = (DescCache) Desc.descCache;
		if (dc==null) dc = new DescCache();
		String artifact = "Hello";
		
		Desc desc1 = new Desc("DescCache.testGetArtifact", String.class);
		desc1.setTag("test");
		Depot.locker.lock(desc1);
		try {
			dc.bind(artifact, desc1);
			
			assert desc1.getBoundValue() == artifact;
			
			Object a1 = dc.getArtifact(desc1);
			assert a1 == artifact;
			
			// a spearate but equals Desc
			Desc desc2 = new Desc("DescCache.testGetArtifact", String.class);
			desc2.setTag("test");
			assert desc2 != desc1 && desc2.equals(desc1);
			
			Object a2 = dc.getArtifact(desc2);
			assert a2 != null;
			assert a2 == artifact;
		} finally {
			Depot.locker.unlock(desc1);
		}
	}

	/** with sym links, desc -> artifact becomes many to 1 */
	@Test
	public void testSymLinks() {		
		Depot depot = Depot.getDefault();
		DescCache dc = (DescCache) Desc.descCache;
		if (dc==null) dc = new DescCache();
		String artifact = "Hello "+Utils.getRandomString(4);
		
		Desc desc1 = new Desc("DescCacheTest.original", String.class);
		desc1.setTag("test");
		Depot.locker.lock(desc1);
		try {
			dc.bind(artifact, desc1);
					
			assert desc1.getBoundValue() == artifact;
			
			Object a1 = dc.getArtifact(desc1);
			assert a1 == artifact;
	
			// a sym link
			Desc desc2 = new Desc("DescCacheTest.link", String.class);
			desc2.setTag("test");
			desc2.setSymLink(true);
			assert desc2 != desc1 && ! desc2.equals(desc1);
			
			depot.put(desc2, desc1);
			
			{	// get
				assert depot.get(desc2) == artifact : depot.get(desc2);
				assert depot.get(desc1) == artifact;
			}
			{	// desc-cache
				Object a1b = dc.getArtifact(desc1);
				assert a1b == artifact : a1b;
				Object a2 = dc.getArtifact(desc2);
				Object a2b = a2;
				if (a2 instanceof Desc) {
					a2b = dc.getArtifact((Desc) a2);
				}
				// TODO cache Desc -> Desc symlink bindings
//				assert a2b == artifact : a2b;
			}
	
			// flush
			depot.flush();
			Utils.sleep(100);
			
			{	// get
				assert depot.get(desc2) == artifact;
				assert depot.get(desc1) == artifact;
			}
			{	// desc-cache
				Object a1b = dc.getArtifact(desc1);
				assert a1b == artifact : a1b;
				Object a2 = dc.getArtifact(desc2);
				Object a2b = a2;
				if (a2 instanceof Desc) {
					a2b = dc.getArtifact((Desc) a2);
				}
				// TODO cache Desc -> Desc symlink bindings
//				assert a2b == artifact : a2b;
			}
		} finally {
			Depot.locker.unlock(desc1);
		}
	}
}
