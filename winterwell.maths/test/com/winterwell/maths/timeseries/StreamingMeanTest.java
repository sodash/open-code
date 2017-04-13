//package com.winterwell.maths.timeseries;
//
//import junit.framework.TestCase;
//import no.uib.cipr.matrix.Vector;
//import com.winterwell.maths.stats.distributions.d1.Constant1D;
//import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
//import com.winterwell.maths.vector.VectorUtils;
//import com.winterwell.maths.vector.XYZ;
//import com.winterwell.utils.Mutable;
//import com.winterwell.utils.time.Dt;
//import com.winterwell.utils.time.TUnit;
//import com.winterwell.utils.time.Time;
//
//public class StreamingMeanTest extends TestCase {
//
//	/**
//	 * From ITI cow work
//	 */
//	public void testResidualDataStream_gravity() {
//		// Some test data
//		final Time t = new Time();
//		final Mutable.Int i = new Mutable.Int(); 
//		ADataStream fakeRaw = new ADataStream(3) {
//			@Override
//			protected Datum next2() {
//				double x = i.value % 2==0? 1 : -1;
//				double y = i.value % 2==0? 1+0.3 : -1+0.3;
//				double z = 0.4;
//				i.value++;
//				return new Datum(t, new double[]{x,y,z}, "A");
//			}			
//		};
//		IDataStream gravity = new StreamingMean(fakeRaw);
//		
//		// burn in
//		for(int j=0; j<1000; j++) gravity.next();
//		
//		// test
//		Vector g = gravity.next();
//			
//		assert VectorUtils.dist(g, new XYZ(0, 0.3, 0.4)) < 0.1;
//				
//	}
//	
//
//	public void testSmall() {
//		PipedDataStream base = new PipedDataStream(1);
//		StreamingMean var = new StreamingMean(base);
//		base.add(new Datum(1));
//		Datum v1 = var.next();
//		base.add(new Datum(-1));
//		Datum v2 = var.next();
//		assert v1.x() == 1 : v1;
//		assert v2.x() == 0;
//	}
//	
//	public void testBasic1D() {
//		RandomDataStream base = new RandomDataStream(new Constant1D(2), null, new Dt(1, TUnit.MINUTE));
//		base.setLabel("A");
//		StreamingMean mean = new StreamingMean(base);
//		for(int i=0; i<10; i++) {
//			Datum d = mean.next();
//			assert d.x() == 2;
//			assert d.getLabel().equals("A");
//		}
//	}
//	
//	public void test1D() {
//		ADataStream base = new RandomDataStream(new Gaussian1D(5,2), null, new Dt(1, TUnit.MINUTE));
//		StreamingMean mean = new StreamingMean(base);
//		// burn in
//		for(int i=0; i<10000; i++) {
//			mean.next();
//		}
//		for(int i=0; i<10; i++) {
//			Datum d = mean.next();
//			assert Math.abs(d.x() - 5) < 0.1 : d;
//		}
//	}
//	
//	public void testLossy1D() {
//		{
//			ADataStream base = new RandomDataStream(new Constant1D(5), null, new Dt(1, TUnit.MINUTE));
//			StreamingMean mean = new StreamingMean(base);
//			mean.setLossFactor(0.1);
//			// burn in
//			for(int i=0; i<10000; i++) {
//				mean.next();
//			}
//			for(int i=0; i<10; i++) {
//				Datum d = mean.next();
//				assert Math.abs(d.x() - 5) < 0.1 : d;
//			}
//		}
//		{
//			ADataStream base = new RandomDataStream(new Gaussian1D(5,2), null, new Dt(1, TUnit.MINUTE));
//			StreamingMean mean = new StreamingMean(base);
//			mean.setLossFactor(0.01);
//			// burn in
//			for(int i=0; i<10000; i++) {
//				mean.next();
//			}
//			for(int i=0; i<10; i++) {
//				Datum d = mean.next();
//				assert Math.abs(d.x() - 5) < 0.25 : d;
//			}			
//		}
//	}
// }
