package com.winterwell.depot;

import org.junit.Test;

import com.winterwell.depot.INotOverwritable.OverWriteException;
import com.winterwell.utils.Utils;

public class INotOverwritableTest {

	@Test
	public void testSmoke() {
		Depot depot = Depot.getDefault();
		Desc desc = new Desc("test_"+Utils.getRandomString(4), MySafeObject.class);
		desc.setTag("test");
		MySafeObject artifact = new MySafeObject("A");
		
		depot.put(desc, artifact);
		
		MySafeObject artifact2 = new MySafeObject("B");
		
		try {
			depot.put(desc, artifact2);
			assert false;
		} catch(OverWriteException ex) {
			// good
		}
		MySafeObject old = depot.putIfAbsent(desc, artifact2);
		assert old == artifact : old;
		
		// you can over-write yourself
		depot.put(desc, artifact);
		
		// explicit remove
		depot.remove(desc);
		
		// now we can add
		depot.put(desc, artifact2);
	}
	
	@Test
	public void testSmokeWithFlush() {
		Depot depot = Depot.getDefault();
		Desc desc = new Desc("test_"+Utils.getRandomString(4), MySafeObject.class);
		desc.setTag("test");
		MySafeObject artifact = new MySafeObject("A");
		
		depot.put(desc, artifact);
		Utils.sleep(100);
		depot.flush();
		Utils.sleep(100);
		
		MySafeObject artifact2 = new MySafeObject("B");
		
		try {
			depot.put(desc, artifact2);
			assert false;
		} catch(OverWriteException ex) {
			// good
		}
		MySafeObject old = depot.putIfAbsent(desc, artifact2);
		assert old == artifact : old;
		
		// you can over-write yourself
		depot.put(desc, artifact);
		
		// explicit remove
		depot.remove(desc);
		depot.flush();
		Utils.sleep(100);
		
		// now we can add
		depot.put(desc, artifact2);
	}

	class MySafeObject implements INotOverwritable {

		private String name;

		public MySafeObject(String string) {
			this.name = string;
		}

		@Override
		public String toString() {
			return "MySafeObject[" + name + "]";
		}
		
	}
}
