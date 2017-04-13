package com.winterwell.depot;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.winterwell.depot.DepotTest.IInner;
import com.winterwell.depot.ModularXML;
import com.winterwell.utils.Key;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.ATask;
import com.winterwell.utils.threads.TaskRunner;

/**
 * 
 * @author joe
 * 
 */
public class DepotTest {

	public interface IInner {
		Desc<IInner> getDesc();
	}	
	
	@Test
	public void testRemove() {
		Depot depot = Depot.getDefault();
		
		List<String> list1 = Arrays.asList("a","b");
		
		Desc desc = new Desc("temp", List.class);
		desc.setTag("test");
		
		depot.put(desc, list1);		
		
		assert depot.contains(desc);
		assert desc.getBoundValue() == list1;
		assert depot.get(desc) == list1;
		
//		boolean del = 
		depot.remove(desc);
						
		assert desc.getBoundValue() == null;
		assert ! depot.contains(desc) : depot.get(desc);
		assert depot.get(desc) == null : depot.get(desc);
		assert Desc.descCache.getArtifact(desc) == null;
	}

//	@Test This is really a test of remote fetching, not core depot code
	public void testMediumSizeFile() throws InterruptedException {
		Depot depot = Depot.getDefault();
		Desc<File> desc = new Desc("TwitterSampler.geo.csv", File.class);
		desc.setTag("corpus");
		desc.setServer(Desc.ANY_SERVER);
		desc.put(new Key("mon"), "Mar_12");
		
		MetaData md = depot.getMetaData(desc);
//		FileUtils.delete(md.getFile()); // Let it stay for speed reasons -- though it means the test will succeed easily 2nd time round
//		assert ! md.getFile().exists() : md.getFile();
		
		File file = depot.get(desc);
		assert file.exists() : md.getFile();
	}
	
	@Test
	public void testSymLink() throws InterruptedException {
		String artifact = "bar"+Utils.getRandomString(4);
		Desc desc = new Desc("foo", String.class);		
		depot.put(desc, artifact);
		
		Desc desc2 = new Desc("fooref", String.class);
		desc2.setSymLink(true);
		depot.putSymLink(desc2, desc);
		
		Object a1 = depot.get(desc);
		Object a2 = depot.get(desc2);
		
		assert a2==a1 : a2+" v "+a1;
		
		depot.flush();
		Utils.sleep(100);
		
		Object a1b = depot.get(desc);
		Object a2b = depot.get(desc2);
		
		assert a2b==a1b : a2b+" v "+a1b;
		
		depot.remove(desc2);
		
		Object a1c = depot.get(desc);
		Object a2c = depot.get(desc2);
		assert a2c==null;
		assert a1c!=null;
		
		depot.flush();
		Utils.sleep(100);
		a1c = depot.get(desc);
		a2c = depot.get(desc2);
		assert a2c==null;
		assert a1c!=null;
		
		depot.putSymLink(desc2, desc);
		
		Object a1d = depot.get(desc);
		Object a2d = depot.get(desc2);
		assert a2d==a1d : a2d+" v "+a1d;
	}
	
	
	@Test
	public void testSymLinkWithFile() throws Exception {
		File artifact = File.createTempFile("testDepot", ".txt");
		FileUtils.write(artifact, "testSymLinkWithFile()");
		
		Desc desc = new Desc("testSymLinkWithFile", File.class);		
		depot.put(desc, artifact);
		
		Desc desc2 = new Desc("fileRef", File.class);
		desc2.setSymLink(true);
		depot.putSymLink(desc2, desc);
		
		Object a1 = depot.get(desc);
		Object a2 = depot.get(desc2);
		
		assert a2==a1 : a2+" v "+a1;
		
		System.out.println(a2);
		
		depot.flush();
		desc = new Desc(desc);
		desc2 = new Desc(desc2);
		IDescCache dc = Desc.getDescCache();
		dc.unbind(null, desc);
		dc.unbind(null, desc2);
		Utils.sleep(100);
		
		Object a1b = depot.get(desc);
		Object a2b = depot.get(desc2);
		
		assert a2b==a1b : a1b+" v "+a2b; // This is a genuine fail :(
		// Depot doesn't recognise that a2b is the xml from a Desc
		
		depot.remove(desc2);
		
		Object a1c = depot.get(desc);
		Object a2c = depot.get(desc2);
		assert a2c==null;
		assert a1c!=null;
		
		depot.flush();
		Utils.sleep(100);
		a1c = depot.get(desc);
		a2c = depot.get(desc2);
		assert a2c==null;
		assert a1c!=null;
		
		depot.putSymLink(desc2, desc);
		
		Object a1d = depot.get(desc);
		Object a2d = depot.get(desc2);
		assert a2d==a1d : a2d+" v "+a1d;
	}
	
	@Test
	public void testContains() throws InterruptedException {
		String artifact = "bar";
		Desc desc = new Desc("foo"+Utils.getRandomString(4), String.class);
		desc.setServer(Desc.LOCAL_SERVER);		
		Depot.locker.lock(desc);
		
		desc.bind(artifact);
		depot.remove(desc);
		
		Utils.sleep(100);
		
		assert ! depot.contains(desc);
	
		depot.put(desc, artifact);
		
		assert depot.contains(desc);
		Utils.sleep(200);		
		assert depot.contains(desc);
		Depot.locker.unlock(desc);
	}
	
	/**
	 * Files are handled a bit differently (no xstream)
	 */
	@Test
	public void testWithFile() {
		// TODO !
	}

	@Test
	public void testBasic() {
		Desc<String> desc = new Desc("testing-consistency.xml", String.class);
		desc.setTag("testgroup");
		desc.setName("MyName");
		desc.put(new Key("foo"), "bar");
		desc.put(new Key("p2"), "v2");

		String preciousArtifact = "Hello world";

		depot.put(desc, preciousArtifact);
		depot.flush();
		String pa2 = depot.get(desc);
		
		assert preciousArtifact.equals(pa2) : pa2;
		
		MetaData md = depot.getMetaData(desc);
		assert md != null;
		System.out.println(md.getFile());
		assert md.getFile().exists() : md.getFile();
		
		depot.remove(desc);
		depot.flush();
		
		assert ! md.getFile().exists();
		MetaData md2 = depot.getMetaData(desc);
		assert md2 != null : md2;
		assert ! md2.file.exists();
	}
	
	
	@Test
	public void testPutIfAbsent() {
		String salt = Utils.getRandomString(4);
		Desc<String> desc = new Desc("testPutIfAbsent-"+salt, String.class);
		desc.setTag("test");
		desc.put(new Key("test"), "PutIfAbsent");
		
		String preciousArtifact = "Hello world";

		String old = depot.putIfAbsent(desc, preciousArtifact);
		assert old == null : old;
		
		old = depot.putIfAbsent(desc, preciousArtifact+" AGAIN");
		assert old != null;
		assert old.equals(preciousArtifact) : old;
		
		// After a flush...
		depot.flush();
		String pa2 = depot.get(desc);		
		assert preciousArtifact.equals(pa2) : pa2;

		old = depot.putIfAbsent(desc, preciousArtifact+" AGAIN");
		assert old != null;
		assert old.equals(preciousArtifact) : old;
		
		// Now we should be able to re-put
		depot.remove(desc);

		old = depot.putIfAbsent(desc, "Goodbye");
		assert old == null : old;

		// clean up
		depot.remove(desc);
		depot.flush();
	}


	public DepotTest() {
		this(Depot.getDefault());
	}
	
	protected DepotTest(Depot depot) {
		this.depot = depot;
	}

	Depot depot;
	
	@Test
	public void testResolution() {
		// If two persisted objects (a,b) that reference a third (c) are loaded
		// should a.c == b.c (strong equality?)
		// This would be quite a strong guarantee, but it appears to be what
		// MRDM is looking for

		Inner c = new Inner("C!");
		Outer a = new Outer("a", c);
		Outer b = new Outer("b", c);
		assert a.c.equals(b.c);

		depot.put(c.getDesc(), c);
		depot.put(a.getDesc(), a);
		depot.put(b.getDesc(), b);
		depot.flush();
		
		// The meat of the test
		Outer a2 = depot.get(a.getDesc());
		Outer b2 = depot.get(b.getDesc());
		assert a2.c != null && b2.c != null;
		assert a2.c.equals(b2.c);
//		assert a2.c == b2.c;
//		IInner c1 = PersistentArtifact.unwrap(a2.c);
//		IInner c2 = PersistentArtifact.unwrap(b2.c);
//		assert c1 == c2 : c1 + " vs " + c2;
	}
	
	// 20/6/2012 - This fails sometimes!
	@Test
	public void testConcurrentAccess() {
		int numWorkers = 200;
		int numThreads = 50;
		final int numInners = 5;
		final int numOuters = 10;
		
		TaskRunner seige = new TaskRunner(numThreads);
		
		final List<Inner> inners = new ArrayList<Inner>();
		final String salt = Utils.getRandomString(4);
		
		// Reset the inners
		for (int i = 0; i < numInners; i++) {
			Inner inner = new Inner(salt+"-inner-" + i);
			depot.put(inner.getDesc(), inner);
			inners.add(inner);
		}
		// reset the outers
		for (int i = 0; i < numOuters; i++) {
			Outer o = new Outer(salt+"-outer-" + (i % numOuters), null);
			depot.remove(o.getDesc());
		}
		depot.flush();
		
		for (int i = 0; i < numWorkers; i++) {
			final int id = i;
			seige.submit(new ATask<String>() {

				@Override
				protected String run() throws Exception {
					Inner i = new Inner(salt+"-inner-" + (id % numInners)); // Would random be better? Want to testing different "stripes"
					Inner i2 = (Inner) depot.putIfAbsent(i.getDesc(), i);
					assert i2 != null;
					assert i2 != i; // Already created!
					assert inners.contains(i2);
					
					Outer o = new Outer(salt+"outer-" + (id % numOuters), i2);
					Outer old = depot.putIfAbsent(o.getDesc(), o);
					
					// Will this make any difference? Meh.
					Desc d2 = new Desc(true, o.getDesc().getId(), Outer.class);
					i = null; i2 = null; o = null;				
					Utils.sleep(Utils.getRandom().nextInt(800) + 200);
					
					Outer o2 = (Outer) depot.get(d2);
					assert o2 != null;
					((Inner) o2.c).count();
					
					System.err.println(o2.c+" "+((Inner)o2.c).count);
					depot.put(o2.getDesc(), o2);
					return null;
				}
			});
		}
		

		int quicktotal = 0;
		for (int i = 0; i < numInners; i++) {
			Inner inner = (Inner) depot.get(new Inner(salt+"-inner-" + i).getDesc());
			System.out.println(i + ":" + inner.count);
			quicktotal += inner.count;
		}
		System.out.println(quicktotal);
		
		// Wait for everything to complete
		while (seige.getTodo().size() > 0) {
			// Sleep
			Utils.sleep(1000);
		}
		
		depot.flush();
		
		int total = 0;
		for (int i = 0; i < numInners; i++) {
			Inner inner = (Inner) depot.get(new Inner(salt+"-inner-" + i).getDesc());
			System.out.println(i + ":" + inner.count);
			total += inner.count;
		}
		
		assert total == numWorkers : total+"!="+numWorkers;
	}
	
	
	

	/**
	 * Test one of DescCache's guarantees: an equals Desc will
	 * get the same object.
	 */
	@Test
	public void testEqualsDescs() {
		String artifact = "Hello";
		
		Desc desc1 = new Desc("testEqualsDescs", String.class);
		desc1.setTag("test");
		
		depot.put(desc1, artifact);
		
		assert desc1.getBoundValue() == artifact;
		Object a1 = depot.get(desc1);
		assert a1 == artifact;
		
		depot.flush();
		Utils.sleep(100);
		
		// a spearate but equals Desc
		Desc desc2 = new Desc("testEqualsDescs", String.class);
		desc2.setTag("test");
		assert desc2 != desc1;
		assert desc2.equals(desc1);
		assert desc2.getBoundValue() == null;
		
		Object a2 = depot.get(desc2);
		assert a2 != null;
		assert a2.equals(artifact);
		assert a2 == artifact : a2;
	}

}




//@ModularXML
final class Inner implements IInner, IHasDesc, com.winterwell.depot.ModularXML {
	String message;
	int count;
	String misc;
	AtomicInteger v = new AtomicInteger();
	
	public IHasDesc[] getModules() { return null; };
	
	Inner(String msg) {
		Log.d("DepotTest", "Creating inner " + msg);
		this.message = msg;
		assert message != null;
		assert message != null;
		assert msg != null;
	}

	@Override
	public Desc<IInner> getDesc() {
		assert message != null : this;
		Desc<IInner> d = new Desc<IInner>("Inner-"+message,
				Inner.class);
		d.setTag("test");
		return d;
	}
	
	public synchronized void count() {
		count++;
	}

	@Override
	public String toString() {
		return "Inner[message=" + message + ", count=" + count + ", misc="
				+ misc + "]";
	}
	
}

final class Outer implements IHasDesc {
	IInner c;
	String name;
	String misc;
	
	Outer(String name, Inner inner) {
		this.name = name;
		
		// Including the following lines make concurrency test fail
		// This is not particularly surprising. But nothing should really be doing
		// the following sort of thing.
		//Desc d = inner.getDesc();
		//d.bind(inner);
		
		this.c = inner; //PersistentArtifact.wrap(inner, IInner.class);
	}

	@Override
	public IHasDesc[] getModules() {
		return new IHasDesc[]{(IHasDesc)c};
	}
	
	public Desc<Outer> getDesc() {
		Desc<Outer> d = new Desc<Outer>("Outer:" + name, Outer.class);
		d.setTag("test");
		// d.addDependency(c.getDesc());
		return d;
	}
}