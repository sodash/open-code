/**
 * 
 */
package com.winterwell.optimization.genetic;

import com.winterwell.maths.GridInfo;
import com.winterwell.maths.stats.distributions.GridDistribution2D;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.optimization.AEvaluate;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;

import junit.framework.TestCase;
import no.uib.cipr.matrix.Vector;

/**
 * 
 * @author Daniel
 */
public class VectorGAOperatorTest extends TestCase {

	/**
	 * Test method for {@link com.winterwell.optimization.genetic.VectorGAOp#generate()}.
	 */
	public void testGenerate() {
		VectorGAOp vop = new VectorGAOp(2);		
		GridDistribution2D dist = new GridDistribution2D(new GridInfo(-10, 10, 100));
		for(int i=0; i<10000; i++) {
			dist.count(vop.generate());
		}
		dist.normalise();
//		Distribution2DChart chart = new Distribution2DChart(dist);
//		HighchartsRenderer r = new HighchartsRenderer();
//		Image img = r.renderToImage(chart, com.winterwell.maths.chart.ImageFormat.PNG);
//		JFrame frame = GuiUtils.popup(img, "Generated Points");
//		GuiUtils.blockWhileOpen(frame);
	}
	
	/**
	 * FIXME The output plot looks worrying
	 */
	public void testMutate() {
		VectorGAOp vop = new VectorGAOp(2);		
		GridDistribution2D dist = new GridDistribution2D(new GridInfo(-10, 10, 100));
		Vector x = vop.generate();
		Vector prev = x;
		for(int i=0; i<10000; i++) {
			Vector mx = vop.mutate(x);			
			dist.count(mx);
			// go for a walk
			Vector mx2 = vop.mutate(prev);
			if (Math.abs(mx2.get(0)) > 10) {
				mx2.set(0, 0);
			}
			if (Math.abs(mx2.get(1)) > 10) {
				mx2.set(1, 0);
			}
			dist.count(mx2);
			prev = mx2;
		}
		dist.normalise();
//		Distribution2DChart chart = new Distribution2DChart(dist);
//		HighchartsRenderer r = new HighchartsRenderer();
//		Image img = r.renderToImage(chart, com.winterwell.maths.chart.ImageFormat.PNG);
//		JFrame frame = GuiUtils.popup(img, "Mutated Points");
//		GuiUtils.blockWhileOpen(frame);
	}

	public void testGA() {
		VectorGAOp vop = new VectorGAOp(2);
		GA<Vector> ga = new GA<Vector>(100, vop);
		Vector best = ga.optimize(new AEvaluate<Vector>() {
			@Override
			public double evaluate(Vector candidate) {
				// distance between capped x0 and x1
				double a = candidate.get(0);
				double b = candidate.get(1);
				double cap = 1000;
				if (a>cap) a = cap;
				if (a<-cap) a = -cap;
				if (b>cap) b = cap;
				if (b<-cap) b = -cap;
				return Math.abs(a - b);
			}		
		});
		Printer.out(best);
		double min = DataUtils.min(best);
		double max = DataUtils.max(best);
		assert min < -10 && max > 10 : best;
	}
	
	public void testGAPoly() {
		VectorGAOp vop = new VectorGAOp(4);
		GA<Vector> ga = new GA<Vector>(500, vop);
		Vector best = ga.optimize(new AEvaluate<Vector>() {
			@Override
			// A simple parabola with max at (0, 1, 2, 3...)
			public double evaluate(Vector candidate) {
				double result = 1;
				for (int i = 0; i<4; i++) {
					double cd = candidate.get(i);
					result *= (i - cd) * (i - cd);
				}
				// Flip the result to be negative (for 4 dimensions)
				return -result;
			}		
		});
		Printer.out(best);
		for (int i = 0; i<4; i++) {
			assert MathUtils.equalish(i, Math.abs(best.get(i)));			
		}
	}
}
