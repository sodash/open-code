//package com.winterwell.depot;
//
//import java.util.logging.Level;
//
//import org.junit.Test;
//
//import com.winterwell.depot.Depot;
//import com.winterwell.depot.Desc;
//import com.winterwell.depot.PAH;
//import com.winterwell.depot.PersistentArtifact;
//
//import com.winterwell.utils.Key;
//import com.winterwell.utils.ReflectionUtils;
//import com.winterwell.utils.StrUtils;
//import com.winterwell.utils.Utils;
//import com.winterwell.utils.containers.Pair;
//import com.winterwell.utils.log.Log;
//import com.winterwell.utils.web.XStreamUtils;
//
///**
// * @tested {@link PersistentArtifact} and {@link PAH}
// * @author daniel
// *
// */
//public class PersistentArtifactTest {
//
//	@Test
//	public void testEquals() {
//		Depot depot = Depot.getDefault();
//		{ // two wrappings of the same thing are equals()
//			Object artifact = "hello";
//			Desc desc = new Desc("test1", Object.class);
//			desc.setTag("test");
//			desc.bind(artifact);			
//			depot.put(desc, artifact);
//
//			Object w = PersistentArtifact.wrap(artifact,
//					new Class[] { CharSequence.class });
//			// not equals() to the original 'cos "hello".equals(wrap) would
//			// never work
//			assert !w.equals(artifact);
//			assert !artifact.equals(w);
//
//			String xml = XStreamUtils.serialiseToXml(w);
//			Object w2 = XStreamUtils.serialiseFromXml(xml);
//			// this will be the artifact
//			Object w3 = depot.get(desc);
//
//			assert PersistentArtifact.isWrapped(w2);
//			assert !PersistentArtifact.isWrapped(w3);
//
//			assert artifact.equals(w3);
//			assert w != w2;
//			assert w.equals(w2);
//
//			assert !w.equals(artifact);
//			assert !w2.equals(artifact);
//			assert !w.equals(w3);
//			assert w3.equals(artifact);
//			assert !artifact.equals(w);
//		}
//	}
//
//	@Test
//	public void testWrap() {
//		Depot depot = Depot.getDefault();
//		Object artifact = "hello";
//		Desc desc = new Desc("test1", Object.class);
//		desc.setTag("test");
//		desc.bind(artifact);
//		
//		depot.put(desc, artifact);
//		Object w = PersistentArtifact.wrap(artifact,
//				new Class[] { CharSequence.class });
//		String xml = XStreamUtils.serialiseToXml(w);
//		System.out.println(xml);
//		assert !xml.contains("hello");
//
//		Object w2 = XStreamUtils.serialiseFromXml(xml);
//		String s = w2.toString();
//		assert s.equals("hello");
//	}
//	
//	
//	@Test
//	public void testForMemoryLeak() { 
//		Log.setDefaultLevel(Level.OFF);
//		Depot depot = Depot.getDefault();
//		long first=0;
//		System.out.println(StrUtils.toNSigFigs(ReflectionUtils.getUsedMemory(), 3));
//		
//		for(int i=0; i<100; i++) {			
//			for(int j=0; j<10000; j++) {
//
//				// 20k worth of data
//				String b = Utils.getRandomString(10)+StrUtils.repeat('b', 10000);
//				Desc descB = new Desc("b"+Utils.getRandomString(6), String.class);
//
//				// 10k worth of props
//				for(int k=0; k<100; k++) {
//					String prop = Utils.getRandomString(10)+StrUtils.repeat('x', 100);
//					descB.put(new Key("propb"+k), prop);
//				}
//				
//				descB.setTag("test");
//				depot.put(descB, b);
//				
//				CharSequence wrappedB = PersistentArtifact.wrap(b, CharSequence.class);
//				Pair artifact = new Pair(
//						Utils.getRandomString(10)+StrUtils.repeat('a', 10000),
//						wrappedB
//						);
//				
//				Desc desc = new Desc(Utils.getRandomString(6), Pair.class);
//				// 10k worth of props
//				for(int k=0; k<100; k++) {
//					String prop = Utils.getRandomString(10)+StrUtils.repeat('x', 100);
//					descB.put(new Key("propb"+k), prop);
//				}
//				desc.setTag("test");
//
//				depot.put(desc, artifact);
//				String xml = XStreamUtils.serialiseToXml(artifact);
//				
//				// hopefully force use of wrap/unwrap
//				Object a2 = depot.get(desc);
//				Object a3 = XStreamUtils.serialiseFromXml(xml);
//				Desc<Object> desc2 = Desc.descCache.getDescription(a3);
//				assert a2.equals(a3);
//				assert a2.equals(artifact);
//				assert desc2.equals(desc);
//			}			
//			
//			long mem = ReflectionUtils.getUsedMemory();
//			if (i==1) first = mem;
//			System.out.println(StrUtils.toNSigFigs(mem/(1024*1024), 3)+"mb\t"+PersistentArtifact.getKnownArtifactsSize());
//		}	
//		long mem = ReflectionUtils.getUsedMemory();
//		assert mem / first < 5: mem+" vs "+first; 
//	}
//	
//
//}
