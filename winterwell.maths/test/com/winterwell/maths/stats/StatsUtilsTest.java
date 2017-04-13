package com.winterwell.maths.stats;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.winterwell.maths.datastorage.DataSet;
import com.winterwell.maths.stats.distributions.AxisAlignedGaussian;
import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.maths.stats.distributions.IGaussian;
import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.ExtraDimensionsDataStream.KMatchPolicy;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.maths.timeseries.RandomDataStream;
import com.winterwell.maths.vector.X;
import com.winterwell.maths.vector.XY;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

import junit.framework.TestCase;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.sparse.SparseVector;

public class StatsUtilsTest extends TestCase {

	public void testCovar() {
		{ // no variance
			double[] xs = new double[] { 3, 3, 3, 3 };
			double[] ys = new double[] { 1, 1, 1, 1 };
			double v = StatsUtils.covar(xs, ys);
			assert v == 0;
		}
		{ // independent vars
			Gaussian1D g1 = new Gaussian1D(0, 1);
			double[] xs = StatsUtils.sample(g1, 10000);
			Gaussian1D g2 = new Gaussian1D(2, 2);
			double[] ys = StatsUtils.sample(g2, 10000);

			double vX = StatsUtils.var(xs);
			double vY = StatsUtils.var(ys);
			double cvXX = StatsUtils.covar(xs, xs);
			double cvXY = StatsUtils.covar(xs, ys);
			double cvYY = StatsUtils.covar(ys, ys);

			assert MathUtils.approx(cvXX, 1) : cvXX;
			assert MathUtils.approx(cvYY, 2) : cvYY;
			assert MathUtils.approx(cvXY, 0) : cvXY;
			assert MathUtils.approx(cvXX, vX) : cvXX;
			assert MathUtils.approx(cvYY, vY) : cvYY;
		}
		{ // dependent
			double[] xs = new double[] { 1, 2, 3, 4 };
			double[] ys = new double[] { 1, 2, 3, 4 };

			double mx = StatsUtils.mean(xs);
			assert mx == 10.0 / 4 : mx;
			double vx2 = MathUtils.sq(1 - mx) + MathUtils.sq(2 - mx)
					+ MathUtils.sq(3 - mx) + MathUtils.sq(4 - mx);
			vx2 = vx2 / 4;

			double vX = StatsUtils.var(xs);
			double vY = StatsUtils.var(ys);
			double cvXX = StatsUtils.covar(xs, xs);
			double cvXY = StatsUtils.covar(xs, ys);
			double cvYY = StatsUtils.covar(ys, ys);

			assert MathUtils.approx(cvXX, vX) : cvXX + " vs " + vX;
			assert MathUtils.approx(cvYY, vY) : cvYY;
			assert MathUtils.approx(cvXY, vX) : cvXY;
		}
	}

	public void testCovarMatrix() {
		AxisAlignedGaussian g = new AxisAlignedGaussian(new XY(0, 0), new XY(1,
				2));
		RandomDataStream stream = new RandomDataStream(g, new Time(),
				TUnit.HOUR.dt);
		ArrayList<Datum> data = DataUtils.sample(stream, 1000);
		// ScatterPlot plot = new ScatterPlot(data);
		// new RenderWithFlot().renderToBrowser(plot);

		Matrix cv = StatsUtils.covar(data);

		System.out.println(cv);
		for (MatrixEntry me : cv) {
			if (me.row() == me.column()) {
				assert Math.abs(me.get()) > 0.5 && Math.abs(me.get()) < 3;
			} else {
				// should be no covariance
				assert Math.abs(me.get()) < 0.3;
			}
		}
	}

	public void testDataSetCovar() throws IOException {
		{
			File temp = File.createTempFile("test", "matrix");
			DataSet dataset = new DataSet("test1", "col-a", "col-b", "col-c");
			ListDataStream data = new ListDataStream(3);
			data.add(new Datum(new double[] { 1, 1, 0 }));
			data.add(new Datum(new double[] { 2, 2, 1 }));
			data.add(new Datum(new double[] { 3, 3, -1 }));
			data.add(new Datum(new double[] { 4, 4, 0 }));
			dataset.setData(data);
			Matrix covar = StatsUtils.covar(dataset,
					KMatchPolicy.DISCARD_ON_MISMATCH, temp);
			for (MatrixEntry me : covar) {
				double cv = me.get();
				IDataStream x = dataset.getDataStream1D(me.row());
				IDataStream y = dataset.getDataStream1D(me.column());
				double[] xs = DataUtils.toArray(x, 0);
				double[] ys = DataUtils.toArray(y, 0);
				double cv2 = StatsUtils.covar(xs, ys);
				assertEquals(cv, cv2);
			}
		}
	}

	public void testDataSetCovar2() throws IOException {
		File temp = File.createTempFile("test", "matrix");
		DataSet dataset = new DataSet("test1", "col-a", "col-b", "col-c");
		ListDataStream data = new ListDataStream(3);
		data.add(new Datum(new double[] { 1, 1, 0 }));
		data.add(new Datum(new double[] { 2, 2, 1 }));
		data.add(new Datum(new double[] { 3, 3, -1 }));
		data.add(new Datum(new double[] { 4, 4, 0 }));
		dataset.setData(data);

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				double cv = StatsUtils.covar2(dataset, row, col,
						KMatchPolicy.DISCARD_ON_MISMATCH);
				IDataStream x = dataset.getDataStream1D(row);
				IDataStream y = dataset.getDataStream1D(col);
				double[] xs = DataUtils.toArray(x, 0);
				double[] ys = DataUtils.toArray(y, 0);
				double cv2 = StatsUtils.covar(xs, ys);
				assertEquals(cv, cv2);
			}
		}
	}

	public void testMean() {
		{
			IGaussian g = new AxisAlignedGaussian(new XY(-10, 5), new XY(1, 2));
			List<Vector> pts = StatsUtils.sample(g, 1000);
			Vector mean = StatsUtils.mean(pts);
			DataUtils.approx(mean, new XY(-10, 5));
		}
		{
			List<Vector> pts = new ArrayList<Vector>();
			for (int i = 0; i < 100; i++) {
				SparseVector v = new SparseVector(Integer.MAX_VALUE);
				int j = Math.abs(Utils.getRandom().nextInt());
				v.set(j, 1);
				pts.add(v);
			}
			SparseVector mean = (SparseVector) StatsUtils.mean(pts);
			assert mean.getUsed() < 101;
		}
	}

	public void testMeanLikelihood() {
		List<X> data = new LinkedList<X>();
		data.add(new X(2));
		data.add(new X(2));
		data.add(new X(3));
		IDistribution g = new AxisAlignedGaussian(new X(2), new X(1));
		double result = StatsUtils.meanLikelihood(data, g);
		assert MathUtils.equalish(result, 0.346) : result;
	}

	public void testNormaliseProbVector() {
		double[] v = new double[] { 1, 2, 3, 4 };
		StatsUtils.normaliseProbVector(v);
		assert v[0] == 0.1;
		assert v[1] == 0.2;
		assert v[2] == 0.3;
		assert v[3] == 0.4;
	}

	public void testNormaliseProbVectorInfiniteValues() {
		double[] v = new double[] { Double.POSITIVE_INFINITY, 27,
				Double.MAX_VALUE, Double.POSITIVE_INFINITY };
		StatsUtils.normaliseProbVector(v);
		assert MathUtils.equalish(v[0], 0.5) : v[0];
		assert MathUtils.equalish(v[1], 0.0) : v[1];
		assert MathUtils.equalish(v[2], 0.0) : v[2];
		assert MathUtils.equalish(v[3], 0.5) : v[3];
	}

	public void testNormaliseProbVectorLargeValues() {
		double[] v = new double[] { Double.MAX_VALUE, 27, Double.MAX_VALUE };
		StatsUtils.normaliseProbVector(v);
		assert MathUtils.equalish(v[0], 0.5) : v[0];
		assert MathUtils.equalish(v[1], 0.0) : v[1];
		assert MathUtils.equalish(v[2], 0.5) : v[2];
	}

	public void testNormaliseProbVectorZero() {
		double[] v = new double[] { 0, 0 };
		StatsUtils.normaliseProbVector(v);
		assert v[0] == 0.5 : v[0];
		assert v[1] == 0.5 : v[1];
	}

	public void testVar() {
		{ // no variance
			double[] xs = new double[] { 3, 3, 3, 3 };
			double v = StatsUtils.var(xs);
			assert v == 0;
		}
		{
			IGaussian g = new AxisAlignedGaussian(new XY(-10, 5), new XY(1, 2));
			List<Vector> pts = StatsUtils.sample(g, 1000);
			Vector v = StatsUtils.var(pts);
			DataUtils.approx(v, new XY(1, 2));
		}
		{
			List<Vector> pts = new ArrayList<Vector>();
			for (int i = 0; i < 100; i++) {
				SparseVector v = new SparseVector(Integer.MAX_VALUE);
				int j = Math.abs(Utils.getRandom().nextInt());
				v.set(j, 1);
				pts.add(v);
			}
			SparseVector var = (SparseVector) StatsUtils.var(pts);
			assert var.getUsed() < 101;
		}
	}

	public void testWeightedMean() {
		{ // constant
			double[] ws = new double[] { 0.04, 0.4, 1, 0, 0.04, 2 };
			double[] xs = new double[] { 20, 20, 20, 20, 20, 20 };
			double m = StatsUtils.weightedMean(ws, xs);
			assert m == 20;
		}
		{ // Gaussian
			double[] ws = StatsUtils.sample(new Gaussian1D(3, 0.2), 1000);
			double[] xs = StatsUtils.sample(new Gaussian1D(4, 1), 1000);
			double m = StatsUtils.weightedMean(ws, xs);
			assert MathUtils.approx(m, 4) : m;
		}
	}

	public void testWeightedVar() {
		double[] ws = new double[] { 0.04, 0.04, 0.04, 0.04, 0.04, 0.04 };
		double[] xs = new double[] { 20, 20, 20, 20, 20, 20 };
		double v = StatsUtils.weightedVar(ws, xs);
		assert MathUtils.equalish(v, 0);
	}

	public void testWeightedVar1D() {
		{ // no variance
			double[] ws = new double[] { 1, 2, 0.5, 0 };
			double[] xs = new double[] { 3, 3, 3, 3 };
			double m = StatsUtils.weightedMean(ws, xs);
			double v = StatsUtils.weightedVar(ws, xs);
			assert m == 3.0;
			assert v == 0 : v;
		}
		{ // Gaussian
			double[] ws = StatsUtils.sample(new Gaussian1D(3, 0.2), 10000);
			double[] xs = StatsUtils.sample(new Gaussian1D(5, 2), 10000);
			double m = StatsUtils.weightedMean(ws, xs);
			double v = StatsUtils.weightedVar(ws, xs);
			assert MathUtils.approx(m, 5) : m;
			assert MathUtils.approx(v, 2) : v;
		}
	}
}
