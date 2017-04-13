package com.winterwell.maths.stats.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.winterwell.maths.stats.KScore;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.vector.XY;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Vector;

public class VariablePickerMetaPredictorTest {

	@Test
	public void testMultiDim() {
		VariablePickerMetaPredictor lr = new VariablePickerMetaPredictor();
		lr.resetup();
		// y = 2x1 -x2 + 0.x3 + 0.x4 + 5
		List<Double> testtargets = new ArrayList();
		List<Vector> traindata = new ArrayList();
		for (int i = 0; i < 50; i++) {
			for (int j = 0; j < 50; j++) {
				double y = 2 * i - j + 5 + Utils.getRandom().nextGaussian();
				double x3 = Utils.getRandom().nextDouble()*100;
				double x4 = Utils.getRandom().nextDouble()*100;
				DenseVector v = new DenseVector(new double[]{i, j, x3, x4});
				lr.train1(v, y);		
				testtargets.add(y);
				traindata.add(v);
			}
		}
		lr.finishTraining();
		Printer.out(lr.getVarMask());		
		double[] residuals = DataUtils.getResiduals(MathUtils.toArray(testtargets), lr, traindata);
		double score = DataUtils.getScore(KScore.ADJUSTED_R2, MathUtils.toArray(testtargets), residuals, lr.getNumExpVars());
		assert score > 0.75 && score <= 1 : score;
		// at least one of the duds should be dropped
		assert Containers.indexOf(false, lr.getVarMask()) > 1 : lr.getVarMask();
	}

	@Test
	public void testDependentVars() {
		VariablePickerMetaPredictor lr = new VariablePickerMetaPredictor();
		lr.resetup();		
		List<Double> testtargets = new ArrayList();
		List<Vector> traindata = new ArrayList();
		// y = 2x1 -x2 + 5 -- but x2 = 3*x1
		for (int i = 0; i < 10; i++) {
			int x2 = 3*i;
			double y = 2 * i - x2 + 5;
			lr.train1(new XY(i, x2), y);	
			testtargets.add(y);
			traindata.add(new XY(i, x2));
		}
		lr.finishTraining();
		Printer.out("Noise: "+lr.getNoise());
		// the values can differ 'cos we have dependent vars => multiple solutions
//			assert VectorUtils.equalish(lr.a, new XYZ(2, -1, 5)) : lr.a;
		assert MathUtils.approx(lr.getNoise().getVariance(), 0) : lr
				.getNoise();
		Printer.out(lr.getVarMask());
		double[] residuals = DataUtils.getResiduals(MathUtils.toArray(testtargets), lr, traindata);
		double score = DataUtils.getScore(KScore.ADJUSTED_R2, MathUtils.toArray(testtargets), residuals, lr.getNumExpVars());
		assert score > 0.75 && score <= 1 : score;
		// something must be dropped to solve the dependency issue
		assert Containers.indexOf(false, lr.getVarMask()) != -1;
	}

}
