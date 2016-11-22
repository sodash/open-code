package com.winterwell.depot;

import java.util.logging.Level;

import org.junit.Test;

import com.winterwell.utils.Key;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.log.Log;

public class DescTest {

//	@Test slow
	public void testForMemoryLeak() { 
		long first=0;
		System.out.println(StrUtils.toNSigFigs(ReflectionUtils.getUsedMemory(), 3));
		
		Log.setMinLevel(Level.INFO);
		
		Depot depot = Depot.getDefault(); // init
		
		for(int i=0; i<500; i++) {			
			for(int j=0; j<10000; j++) {
				// Had to downgrade the 10k to <1k to appease an assert
				// ??10k worth of data
				String artifact = Utils.getRandomString(10)+StrUtils.repeat('x', 200);
				// ??10k worth of prop
				String prop = Utils.getRandomString(4)+StrUtils.repeat('x', 200);
				
				Desc desc = new Desc(Utils.getRandomString(4), String.class);
				desc.setTag("test");
				desc.put(new Key("bigprop"), prop);
//				TODO PersistentArtifact.wrap(artifact, intrfc)
				Depot.getDefault().put(desc, artifact);
			}			
			System.gc();
			Utils.sleep(1000);
			
			long mem = ReflectionUtils.getUsedMemory();
			if (i==1) first = mem;
			
			SlowStorage laters = (SlowStorage) depot.getBase();
			System.out.println(StrUtils.toNSigFigs(mem/(1024*1024), 3)+"mb\t"
					+"DescCache.sharedObject2Desc: "+DescCache.sharedObject2Desc.size()+"\t"
					+"DescCache.desc2bound: "+DescCache.desc2bound.size()+"\t"
					+"StoreLater.queue: "+laters.getQ().size());
		}	
		long mem = ReflectionUtils.getUsedMemory();
		assert mem / first < 5: mem+" vs "+first; 
	}
	
	
	@Test
	public void testGetId() {
		Desc desc = new Desc("test1", Pair.class);
		desc.setTag("test-project");
		desc.put(new Key("foo"), "bar");
		System.out.println(desc.getId());

		String id = desc.getId();

		Desc copy = new Desc(desc);

		assert copy.getId().equals(id);

		assert id.contains("test1") && id.startsWith("test-project/") : id;
	}

	@Test
	public void testHashCode() {
		Desc desc = new Desc("test1", Pair.class);
		desc.setTag("test-project");
		desc.put(new Key("foo"), "bar");

		Desc copy = new Desc(desc);

		assert desc.hashCode() == copy.hashCode();
		
		Desc desc2 = new Desc("test1", Pair.class);
		desc2.setTag("test-project");
		desc2.put(new Key("foo"), "bar");
		
		assert desc.hashCode() == desc2.hashCode();
		assert desc.equals(desc2);
		assert desc.equals(copy);
	}

}
