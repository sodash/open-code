package com.winterwell.maths.stats.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.winterwell.maths.chart.ChartType;
import com.winterwell.maths.chart.Renderer;
import com.winterwell.maths.chart.XYChart;
import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.vector.X;
import com.winterwell.maths.vector.XY;
import com.winterwell.maths.vector.XYZ;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;

public class LinearRegressionTest {

	@Test
	public void testExponential() {
		LinearRegression lr = new LinearRegression();
		lr.resetup();
		// y = 1/4 * e^2x
		List<XY> data = new ArrayList();
		for (double i = 0; i < 5; i += 0.5) {
			double y = Math.exp(2 * i) * 0.25;
			// train on ln(y)
			lr.train1(new X(i), Math.log(y));
			data.add(new XY(i, Math.log(y)));
		}
		XYChart chart = new XYChart();
		chart.setType(ChartType.SCATTER);
		chart.setData(data);
		
		lr.finishTraining();
		Printer.out(lr.a);
		// read out: if y = b.e^a.x, we fitted ln(y) = a.x + ln(b)
		double a = Math.exp(lr.a.get(1));
		double lambda = lr.a.get(0);
		assert MathUtils.equalish(a, 0.25) : a + " " + lambda;
		assert MathUtils.equalish(lambda, 2) : a + " " + lambda;
		
		Renderer.popup(chart);
	}

	@Test
	public void testMultiDim() {
		LinearRegression lr = new LinearRegression();
		lr.resetup();
		// y = 2x1 -x2 + 5
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				double y = 2 * i - j + 5;
				lr.train1(new XY(i, j), y);
			}
		}
		lr.finishTraining();
		Printer.out(lr.a);
		assert DataUtils.equalish(lr.a, new XYZ(2, -1, 5)) : lr.a;
		assert MathUtils.equalish(lr.getNoise().getVariance(), 0) : lr
				.getNoise();
	}

	@Test
	public void testDependentVars() {
		// should fail
		try {
			LinearRegression lr = new LinearRegression();
			lr.setResilient(false);
			lr.resetup();		
			// y = 2x1 -x2 + 5 -- but x2 = 3*x1
			for (int i = 0; i < 10; i++) {
				int x2 = 3*i;
				double y = 2 * i - x2 + 5;
				lr.train1(new XY(i, x2), y);				
			}
			lr.finishTraining();
//			assert false; // If we use pseudo-inverse, this will be fine
			Printer.out(lr.a);
			Printer.out("Noise: "+lr.getNoise());
		} catch(Exception ex) {
			// yeh
		}
		// should succeed
		{
			LinearRegression lr = new LinearRegression();
			lr.setResilient(true);
			lr.resetup();		
			// y = 2x1 -x2 + 5 -- but x2 = 3*x1
			for (int i = 0; i < 20; i++) {
				int x2 = 3*i;
				double y = 2 * i - x2 + 5;
				lr.train1(new XY(i, x2), y);				
			}
			lr.finishTraining();
			Printer.out(lr.a);
			Printer.out("Noise: "+lr.getNoise());
			// the values can differ 'cos we have dependent vars => multiple solutions
//			assert VectorUtils.equalish(lr.a, new XYZ(2, -1, 5)) : lr.a;
			assert MathUtils.approx(lr.getNoise().getVariance(), 0) : lr
					.getNoise();
		}
	}
	
	
	@Test	
	public void testReal1D() {
		LinearRegression lr = new LinearRegression();
		Gaussian1D noise = new Gaussian1D(0, 0.1);
		lr.resetup();
		// y = 2x + 1
		for(int j=0; j<10; j++) {
			for (int i = 0; i < 200; i++) {
				double y = 2 * i + 1 + noise.sample();
				lr.train1(new X(i), y);
			}
		}
		lr.finishTraining();
		Printer.out(lr.a);
		assert DataUtils.approx(lr.a, new XY(2, 1)) : lr.a;
		assert MathUtils.approx(lr.getNoise().getVariance(), 0.1) : lr.getNoise();
	}

	@Test
	public void testSimple() {
		LinearRegression lr = new LinearRegression();
		lr.resetup();
		// y = 2x + 1
		for (int i = 0; i < 10; i++) {
			double y = 2 * i + 1;
			lr.train1(new X(i), y);
		}
		lr.finishTraining();
		Printer.out(lr.a);
		assert DataUtils.equalish(lr.a, new XY(2, 1)) : lr.a;
		assert MathUtils.equalish(lr.getNoise().getVariance(), 0) : lr
				.getNoise();
	}
}
