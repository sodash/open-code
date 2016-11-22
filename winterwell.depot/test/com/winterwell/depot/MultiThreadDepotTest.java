package com.winterwell.depot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.winterwell.utils.Key;
import com.winterwell.utils.Utils;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

/**
 * TODO 
 */
public class MultiThreadDepotTest {

	/**
	 * Thread 1 puts, & waits at different stages (requires breakpoints & manual runs)/<br>
	 * Thread 2 gets overlapping & puts overlapping.
	<p>
	Test this:
		
	0. No breaks.
	Result (Aug 21): success
	1. Break with the item in the write-behind Q. (actually, no break required)
	Result (Aug 21)git pull: success
	2. Break with the item just removed from the Q. (WriteBehind.writeOld() line 161)
	Result (Aug 21): Success. DescCache passes Alice's artifact to Bob.
	3. Break during write. (FileStore.put() lines 127,132)
	Result (Aug 21): Success.
	
	WITH DescCache nobbled...
	0. success
	1. success
	2. Fail! Bob's get fails
	3. Success?!
	 */
	@Test
	public void test2Threads() throws InterruptedException {
		final Depot depot = Depot.getDefault();
		final String salt = Utils.getRandomString(4);		
		Thread t1 = new Thread("Alice") {		
			public void run() {
				Desc d = new Desc("2threads"+salt, String.class);
				d.setTag("test");
				depot.put(d, "Alice's Artifact "+salt);
//				// Nobble DescCache ??
//				DescCache.desc2bound.clear();
//				DescCache.sharedObject2Desc.clear();
			}
		};
		Thread t2 = new Thread("Bob") {
			public void run() {
				Desc d = new Desc("2threads"+salt, String.class);
				// PUT A BREAKPOINT HERE -- then let Thread 2 go when thread 1 is "in position"
				d.setTag("test");
				Object a = depot.get(d);
				if (a==null) a = "Bob's Artifact "+salt;
				depot.put(d, a);
			}
		};
		
		// Ready? Go!
		t1.start();
		Utils.sleep(100);
		t2.start();
		
		t1.join();
		t2.join();
		
		Desc d = new Desc("2threads"+salt, String.class);
		d.setTag("test");
		Object a = depot.get(d);
		assert a.equals("Alice's Artifact "+salt) : a;
	}
	

	/**
	 * Lots of threads, lots of artifacts, gets & puts.
	 * Takes a few minutes to run.
	 */
//	@Test // slow
	public void testStress() throws InterruptedException {
//		Log.setMinLevel(Level.ALL);
		final Depot depot = Depot.getDefault();
		((SlowStorage)depot.getBase()).setDelay(new Dt(5, TUnit.SECOND));
		final ExecutorService exe = Executors.newFixedThreadPool(100);
		final Map<String,String> id2art = new ConcurrentHashMap();
		final String salt = Utils.getRandomString(5);
		final AtomicInteger error = new AtomicInteger();
		for(int i=0; i<100000; i++) {
			final int fi = i;
			final int j = i % 100;
			exe.submit(new Runnable() {		
				public void run() {			
					// slow up sometimes to create space for WriteBehind
					if (Utils.getRandomChoice(0.1)) Utils.sleep(1000);
					if (fi % 1000 == 0) System.out.println("	Get/Put "+fi);
					Desc d = new Desc("stress"+j, String.class);
					d.setTag("test");				
					d.put(new Key("salt"), salt);
					synchronized (id2art) {
						Object is = depot.get(d);
						String shouldBe = id2art.get(d.getId());
						if ( ! Utils.equals(is, shouldBe)) {
							System.out.println("ERROR "+error.incrementAndGet()+" "+is+" vs "+shouldBe);
							exe.shutdown();
						}
					}					
					
					String a = ""+j+Utils.getRandomString(1000);
					synchronized (id2art) {
						depot.put(d, a);	
						id2art.put(d.getId(), a);
					}					
				}
			});
		}
		exe.shutdown();
		exe.awaitTermination(100, TimeUnit.SECONDS);
		depot.flush();
		Utils.sleep(100);
		assert error.get() == 0 : error;
	}

	/**
	 * This fails sometimes, but the failures look bogus
	 * @throws InterruptedException
	 */
//	@Test // it's slow
	public void testStressWithModularArtifacts() throws InterruptedException {
//		lots of threads, lots of artifacts, gets & puts
		final Depot depot = Depot.getDefault();
		((SlowStorage)depot.getBase()).setDelay(new Dt(5, TUnit.SECOND));
		final ExecutorService exe = Executors.newFixedThreadPool(100);
		final Map<String,String> id2art = new ConcurrentHashMap();
		final String salt = Utils.getRandomString(6);
		final AtomicInteger error = new AtomicInteger();
		final List<String> errors = Collections.synchronizedList(new ArrayList());
		
		for(int i=0; i<100000; i++) {
			final int fi = i;
			// 100 outers
			final int j = i % 100;
			exe.submit(new Runnable() {		
				public void run() {
					// slow up sometimes to create space for WriteBehind
					if (Utils.getRandomChoice(0.1)) Utils.sleep(500);
					if (fi % 1000 == 0) System.out.println("	Get/Put "+fi);
					// 10 inners
					Inner in = new Inner((j%10)+"-"+salt);
					in.getDesc().put(new Key("salt"), salt);
					String ind = in.getDesc().getId();
					// a new outer
					Outer outer = new Outer("O"+j+"-"+salt, in);
					
					Desc<Outer> d = outer.getDesc();	
					d.put(new Key("salt"), salt);
					
					synchronized (id2art) {
						Outer got = depot.get(d);
//						assert ((Inner)got.c).message != null : got.c.getDesc();
						Object is = got==null? null : got.misc+" "+got.c;
						String shouldBe = id2art.get(d.getId());
						if ( ! Utils.equals(is, shouldBe)) {
							System.out.println("ERROR "+error.incrementAndGet()+" "+is+" vs "+shouldBe);
							errors.add(is+" vs "+shouldBe+" for "+got+" inner.v="+((Inner)got.c).v+" inner.desc="+got.c.getDesc());
							exe.shutdownNow();
						}
					}
					
					in.misc = Utils.getRandomString(10);
					outer.misc = Utils.getRandomString(10);					
					synchronized (id2art) {
						((Inner)outer.c).v.incrementAndGet();
						depot.put(d, outer);
						String as = outer.misc+" "+outer.c;						
						id2art.put(d.getId(), as);
					}					
				}
			});
		}
		exe.shutdown();
		exe.awaitTermination(100, TimeUnit.SECONDS);
		depot.flush();
		Utils.sleep(100);
		assert error.get() == 0 : error+" "+errors;
	}
	
}


