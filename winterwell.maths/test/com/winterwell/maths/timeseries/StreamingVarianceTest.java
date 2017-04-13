//package com.winterwell.maths.timeseries;
//
//import junit.framework.TestCase;
//import com.winterwell.maths.stats.distributions.d1.Constant1D;
//import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
//import com.winterwell.utils.time.Dt;
//import com.winterwell.utils.time.TUnit;
//
//public class StreamingVarianceTest extends TestCase {
//
//	public void testBasic1D() {
//		RandomDataStream base = new RandomDataStream(new Constant1D(2), null, new Dt(1, TUnit.MINUTE));
//		base.setLabel("A");
//		StreamingVariance var = new StreamingVariance(base);
//		for(int i=0; i<10; i++) {
//			Datum d = var.next();
//			assert d.x() == 0 : d.x();
//			assert d.getLabel().equals("A");
//		}
//	}
//	
//	public void testSmall() {
//		PipedDataStream base = new PipedDataStream(1);
//		StreamingVariance var = new StreamingVariance(base);
//		base.add(new Datum(1));
//		Datum v1 = var.next();
//		base.add(new Datum(-1));
//		Datum v2 = var.next();
//		assert v1.x() == 0;
//		assert v2.x() == 1;
//	}
//	
//	public void test1D() {
//		ADataStream base = new RandomDataStream(new Gaussian1D(5,2), null, new Dt(1, TUnit.MINUTE));
//		StreamingVariance var = new StreamingVariance(base);
//		// burn in
//		for(int i=0; i<10000; i++) {
//			var.next();
//		}
//		for(int i=0; i<10; i++) {
//			Datum d = var.next();
//			assert Math.abs(d.x() - 2) < 0.1 : d;
//		}
//	}
//	
//	
//	public void testLossy1D() {
//		{
//			ADataStream base = new RandomDataStream(new Constant1D(5), null, new Dt(1, TUnit.MINUTE));
//			StreamingVariance var = new StreamingVariance(base);
//			var.setLossFactor(0.1);
//			// burn in
//			for(int i=0; i<10000; i++) {
//				var.next();
//			}
//			for(int i=0; i<10; i++) {
//				Datum d = var.next();
//				assert Math.abs(d.x()) < 0.1 : d;
//			}
//		}
//		{
//			ADataStream base = new RandomDataStream(new Gaussian1D(5,2), null, new Dt(1, TUnit.MINUTE));
//			StreamingVariance var = new StreamingVariance(base);
//			var.setLossFactor(0.01);
//			// burn in
//			for(int i=0; i<10000; i++) {
//				var.next();
//			}
//			for(int i=0; i<10; i++) {
//				Datum d = var.next();
//				assert Math.abs(d.x() - 2) < 0.25 : d;
//			}			
//		}
//	}
// }
