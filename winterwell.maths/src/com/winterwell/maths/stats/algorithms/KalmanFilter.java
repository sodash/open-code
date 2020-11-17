package com.winterwell.maths.stats.algorithms;

import static com.winterwell.maths.matrix.MatrixUtils.apply;
import static com.winterwell.maths.matrix.MatrixUtils.approx;
import static com.winterwell.maths.matrix.MatrixUtils.getDiagonal;
import static com.winterwell.maths.matrix.MatrixUtils.invert;
import static com.winterwell.maths.matrix.MatrixUtils.multiply;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.matrix.DiagonalMatrix;
import com.winterwell.maths.matrix.IdentityMatrix;
import com.winterwell.maths.matrix.MatrixUtils;
import com.winterwell.maths.stats.KScore;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.stats.distributions.Gaussian;
import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.Vector.Norm;

/**
 * Why don't the usual Java suspects have a KalmanFilter that implements learning?
 * @author daniel
 * @testedby  KalmanFilterTest}
 */
public class KalmanFilter implements ITrainable.Unsupervised<Vector>, ITimeSeriesFilter {

	/**
	 * The smoothed states from the last training, or null
	 * @return
	 */
	public List<IDistribution> getSmoothedFromTraining() {
		return smoothed;
	}
	
	int numHiddenDimensions;
	int numObservedDimensions;

	// NB: name matches Apache Commons version
	@Override
	public int getStateDimension() {
		return numHiddenDimensions;
	}
	
	@Override
	public int getMeasurementDimension() {
		return numObservedDimensions;
	}
	
	public KalmanFilter(int numHiddenDimensions, int numObservedDimensions) {
		this.numHiddenDimensions = numHiddenDimensions;
		this.numObservedDimensions = numObservedDimensions;		
	}
	
	public KalmanFilter(ProcessModel pm, MeasurementModel mm) {
		this(pm.getStateTransitionMatrix().getColumnDimension(), mm.getMeasurementMatrix().getRowDimension());
		setTransitionMatrix(MatrixUtils.mtj(pm.getStateTransitionMatrix()));
		setTransitionNoise(MatrixUtils.mtj(pm.getProcessNoise()));
		setEmissionMatrix(MatrixUtils.mtj(mm.getMeasurementMatrix()));
		setEmissionNoise(MatrixUtils.mtj(mm.getMeasurementNoise()));
	}

	Matrix trans;
	Matrix transNoise;
	Vector transOffset;
	Matrix emit;
	Matrix emitNoise;
	Vector emitOffset;
	
	boolean trainTrans = true;
	boolean trainEmit = true;
	boolean trainEmitNoise  = true;
	private ListDataStream trainData;
	
	public void setTrainEmit(boolean trainEmit) {
		this.trainEmit = trainEmit;
	}
	/**
	 * The smoothed states from training
	 */
	transient private List<IDistribution> smoothed;
	
	@Override
	public void finishTraining() {
		// EM
		// init sensibly -- identity matrices
		if (trans==null) {
			trans = new IdentityMatrix(numHiddenDimensions);
		}
		if (transNoise==null) {
			transNoise = new IdentityMatrix(numHiddenDimensions);
		}
		if (emit==null) {
			assert numHiddenDimensions==numObservedDimensions; // TODO cover EM for dropping/adding dims
			emit = new IdentityMatrix(numObservedDimensions);
		}
		if (emitNoise==null) {
			emitNoise = new IdentityMatrix(numHiddenDimensions);
		}
		// offsets start at 0

		int N = 3;
		KalmanFilter prevGood = new KalmanFilter(numHiddenDimensions,numObservedDimensions);
		for(int i=0; i<N; i++) {			
			try {
				// Do 1 Iteration
				emIteration();
				// score it
				double score = getScore();
				System.out.println("EM "+i+": R2 = "+score);
				prevGood.trans = trans;
				prevGood.transNoise = transNoise;
				prevGood.transOffset = transOffset;
				prevGood.emit = emit;
				prevGood.emitNoise = emitNoise;
				prevGood.emitOffset = emitOffset;
			} catch(Throwable ex) {
				if (i==0) {
					Log.w("KalmanFilter.EM", "No score at iteration "+i+": "+ex);
					// allow another iteration
					continue;
				}
				if (prevGood.trans==null) {
					FailureException.fail(ex);
				}
				// fallback to previous good version
				Log.w("KalmanFilter.EM", "Error at iteration "+i+": "+ex+"; Stopping at last good solution.");
				this.trans = prevGood.trans;
				this.transNoise = prevGood.transNoise;
				this.transOffset = prevGood.transOffset;
				this.emit = prevGood.emit;
				this.emitNoise = prevGood.emitNoise;
				this.emitOffset = prevGood.emitOffset;
				break;
			}
		}
	}
	
	/**
	 * Note: we use the 1st data-point as the hidden state.
	 * @return R2 Which is a bit odd, as we don't want to exactly emulate the observatiobs (R2=1).
	 * But it makes decent enough sense. Log-likelihood would be even more sensible but more faff.
	 */
	public double getScore() {		
		// smooth it
		List<IDistribution> smoothed = smooth(null, trainData);
		// get the residuals vs the observations
		List<Vector> residuals = getResidualsFromSmoothed(smoothed);
		int numExpVars = numHiddenDimensions;
		double r2 = DataUtils.getScore(KScore.R2, trainData, residuals, numExpVars);		
		return r2;
	}
	
	private List<Vector> getResidualsFromSmoothed(List<IDistribution> smoothed2) {		
		List<Vector> residuals = new ArrayList();
		for(int i=0; i<trainData.size(); i++) {
			IDistribution si = smoothed2.get(i);
			Vector sMean = si.getMean();
			assert DataUtils.isSafe(sMean) : i+" "+sMean;
			Vector pi = MatrixUtils.apply(emit, sMean);
			if (emitOffset!=null) pi.add(emitOffset);
			// target - predicted
			Datum ti = trainData.get(i).copy();
			Vector residual = ti.add(-1, pi);
			residuals.add(residual);
		}
		return residuals;
	}

	private void emIteration() {
		int n = trainData.size();

		// Predict the hidden state
		smoothed = smooth(null, trainData);
		
		if (trainEmit) {	// Fit the emission matrix -- dim by dim
			DenseVector emitOffset2 = new DenseVector(numObservedDimensions);
			DenseMatrix emitter = new DenseMatrix(numObservedDimensions, numHiddenDimensions);
			List<double[]> allResiduals = new ArrayList();
			for(int d=0; d<numObservedDimensions; d++) {
				VariablePickerMetaPredictor lr = new VariablePickerMetaPredictor();			
				for(int i=0; i<n; i++) {
					// example: we'd like E_dj.state_estimate = observation_d 
					double y = trainData.get(i).get(d);
					lr.train1(smoothed.get(i).getMean(), y, 1);
				}
				lr.finishTraining();
				Vector ws = lr.getWeights();
				// split the weights into emit row & offset
				Vector row = DataUtils.slice(ws, 0, -1);
				emitOffset2.set(d, ws.get(ws.size()-1)); 
				MatrixUtils.setRow(emitter, d, row);
			}
			emit = emitter;
			emitOffset = emitOffset2;			
		}
		if (trainEmit || trainEmitNoise) {			
			// Fit the emission noise
			// ...collect the residuals
			List<Vector> residuals = getResidualsFromSmoothed(smoothed);
			Matrix cv = StatsUtils.covar(residuals);
			emitNoise = cv;
			double traceEmitNoise = MatrixUtils.trace(emit);
			System.out.println("traceEmitNoise "+traceEmitNoise);
		}
		if (trainTrans) {	// Fit the transition matrix -- dim by dim
			DenseVector transOffset2 = new DenseVector(numHiddenDimensions);
			DenseMatrix trans2 = new DenseMatrix(numHiddenDimensions, numHiddenDimensions);
			List<double[]> transResiduals = new ArrayList();
			for(int d=0; d<numHiddenDimensions; d++) {			
				VariablePickerMetaPredictor lr = new VariablePickerMetaPredictor();			
				for(int i=0; i<n-1; i++) {
					Vector before = smoothed.get(i).getMean();
					Vector after = smoothed.get(i+1).getMean();
					lr.train1(before, after.get(d), 1);
				}
				lr.finishTraining();
				Vector ws = lr.getWeights();
				Vector row = DataUtils.slice(ws, 0, -1);
				transOffset2.set(d, ws.get(ws.size()-1)); 
				MatrixUtils.setRow(trans2, d, row);
				// collect the residuals
				double[] residuals = lr.getResiduals();
				transResiduals.add(residuals);
			}
			trans = trans2;
			transOffset = transOffset2;
			assert DataUtils.isSafe(trans);
			assert DataUtils.isSafe(transOffset);
			double traceEmitNoise = MatrixUtils.trace(trans);
			System.out.println("traceTrans "+traceEmitNoise);
			
			// Fit the transmission noise
			ListDataStream vtresiduals = DataUtils.combineColumns(transResiduals);
			Matrix tcv = StatsUtils.covar(vtresiduals);
			transNoise = tcv;
			assert DataUtils.isSafe(tcv);
			double traceTransNoise = MatrixUtils.trace(transNoise);
			System.out.println("traceTransNoise "+traceTransNoise);
		}
		
		// TODO fit the initialState covar (we'll take the initial-state from an observation)
	}

	@Override
	public Gaussian filter(IDistribution state, Vector observation) {	
		assert observation==null || DataUtils.isSafe(observation) : observation;
		if (state==null) {	
			// we start really uncertain, so that the first observation + emission-noise will set the state.
			Vector m;
			if (MatrixUtils.approx(emit, new IdentityMatrix(numHiddenDimensions))) {
				// Use the observation as the initial state
				m = observation;
			} else {
				// Use a blank-ish initial state
				m = transOffset==null? DataUtils.newVector(numHiddenDimensions) : transOffset;
			}
			assert transNoise != null;
			Matrix covar = new DenseMatrix(transNoise).scale(1000000);
//			Matrix covar = new IdentityMatrix(numHiddenDimensions, 1000000000); // StatsUtils.covar(trainData).scale(2);
			state = new Gaussian(m, covar);
		}
		Gaussian hstate = StatsUtils.toGaussian(state);
		assert DataUtils.isSafe(hstate.getMean());
		assert DataUtils.isSafe(hstate.getCovar());
		
		// 1. predict
		Gaussian prior = predict(hstate, null);
		
		assert DataUtils.isSafe(prior.getMean());
		assert DataUtils.isSafe(prior.getCovar());
		if (prior.getMean().norm(Norm.One) > 1000000000) {
			// somethings' gone wrong
			System.out.println(prior);
		}
		
		// 2. correct
		Gaussian posterior = correct(prior, observation);

		// sanity check
		assert DataUtils.isSafe(posterior.getMean()) : posterior.getMean();
		assert DataUtils.isSafe(posterior.getCovar()) : posterior.getCovar();
		if (observation==null) return posterior;
		
		// the correction ought to bring the prediction closer to the observation
		Vector priorObs = getPredictedObservation(prior.getMean());
		Vector postObs = getPredictedObservation(posterior.getMean());
		double diff1 = DataUtils.dist(priorObs, observation);
		double diff2 = DataUtils.dist(postObs, observation);
		if (diff1 < diff2) {
			// Inverse and solve ops can go awry. (seen with Ring v1 spreadsheet, April 2016)
			// Is emission close to a diagonal matrix? 
			// If so, let's do an average using "safe" approximate inverses.
			DiagonalMatrix diagonal = new DiagonalMatrix(MatrixUtils.getDiagonal(emit));
			if (approx(emit, diagonal)) {
				// TODO Project the observation back into the hidden state space
				// NB: This is possible because the emission matrix is approx an identity matrix.
				Matrix invEmit;
				try {
					invEmit = invert(emit, false);
				} catch (Exception ex) {
					invEmit = MatrixUtils.invert(diagonal);
				}
				Vector obs = observation;
				if (emitOffset!=null) {
					obs = observation.copy().add(-1, emitOffset);
				}
				Vector emitVar = getDiagonal(emitNoise);
				Gaussian obsg = new Gaussian(obs, new DiagonalMatrix(emitVar));
				Gaussian projectedBackObs = obsg.apply(invEmit);
				// average it
				double[] postmean = new double[numHiddenDimensions];
				// drop correlations and force it to diagonal?? TODO explore keeping them
				Matrix postcovar = new DiagonalMatrix(new DenseVector(numHiddenDimensions));
				// Z = X.Y is Gaussian with variance 
				// vz = vx.vy/(vx+vy), and mean z = (vz/vx).x + (vz/vy).y  
				for(int d=0; d< postmean.length; d++) {
					double vx=projectedBackObs.getCovar().get(d,d), vy = prior.getCovar().get(d, d);
					if (vx + vy == 0) {
						// A no-variance column? The caller should probably have dropped that. 
						// Still, since we're here, let's manage it.
//						Log.d("KalmanFilter", "Managing no-variance column "+d);
						postcovar.set(d, d, 0);
						postmean[d] = (projectedBackObs.getMean().get(d) + prior.getMean().get(d))/2;
						continue;
					}
					double vz = vx*vy/(vx + vy);
					assert MathUtils.isFinite(vz);
					postcovar.set(d, d, vz);
					postmean[d] = (vy/(vx+vy))*projectedBackObs.getMean().get(d) + (vx/(vx+vy))*prior.getMean().get(d);
				}				
				DenseVector hackPosterior = new DenseVector(postmean, false);
				Vector hackpostObs = apply(emit, hackPosterior);
				double hackdiff2 = DataUtils.dist(hackpostObs, observation);
				assert hackdiff2 <= diff2 : hackdiff2+" worse than "+diff2+" worse than "+diff1;
				if (hackdiff2 > diff1) {
					Log.w("KalmanFilter", "KF may be adrift!");
				}
				return new Gaussian(hackPosterior, postcovar);
			} else {
				Log.w("KalmanFilter", "Drifting away from the solution?! "+diff2+" > "+diff1+" obs:"+observation);
			}
		}	
		
		return posterior;
	}
	
	 /**
     * Predict the internal state estimation one time step ahead.
     * 
     * h' = trans.h + transOffset + controlVector + transNoise
     * 
     * @param controlVector Can be null
     */
    Gaussian predict(Gaussian hiddenState, Vector controlVector) {    	
    	if (hiddenState==null) throw new NullPointerException();
    	assert DataUtils.isSafe(hiddenState.getMean()) : hiddenState;
    	assert controlVector==null || DataUtils.isSafe(controlVector);
    	
    	Gaussian predicted = hiddenState.apply(trans);
    	
        // project the state estimation ahead (a priori state)
        // xHat(k)- = A * xHat(k-1) + controlVector
    	// NB: Apache add in a controlMatrix, B, then add B.controlVector -- seems unnecessary to do inside the KF
    	Vector stateEstimation2 = predicted.getMean(); // apply(trans, hiddenState.getMean());
        // add control input if it is available
        if (controlVector != null) {
            stateEstimation2.add(controlVector);
        }
        // add the offset
        if (transOffset!=null) {
        	stateEstimation2.add(transOffset);
        }

        // project the error covariance ahead -- mostly done in the Gaussian.apply() method
        // P(k)- = A * P(k-1) * A' + Q
//    	Matrix transHidden = multiply(trans, hiddenState.getCovar());
//    	Matrix errorCovariance2 = MatrixUtils.newMatrix(hiddenState.getDim(), hiddenState.getDim());
//    	transHidden.transBmult(trans, errorCovariance2);
//        errorCovariance2
    	predicted.getCovar().add(transNoise); 
    	return predicted; //new Gaussian(stateEstimation2, errorCovariance2);
    }

    
    /**
     * Correct the current state estimate with an actual measurement.
     *
     */
    Gaussian correct(Gaussian hiddenState, Vector observation)
    {
    	if (observation==null) return hiddenState;
    	assert DataUtils.isSafe(observation);
    	assert DataUtils.isSafe(hiddenState.getMean());
    	assert DataUtils.isSafe(hiddenState.getCovar());
        // S = H * P(k) * H' + R
    	Matrix HP = multiply(emit, hiddenState.getCovar());
		Matrix emitT = MatrixUtils.transpose(emit);
		Matrix S = multiply(HP, emitT);
		assert DataUtils.isSafe(emitNoise);
    	S.add(emitNoise);
		assert DataUtils.isSafe(S);

        // Inn = z(k) - H * xHat(k)-   The residual
    	Vector innovation = observation.copy();    	
    	Vector prediction = getPredictedObservation(hiddenState.getMean());
        innovation.add(-1, prediction);

        // calculate gain matrix
        // K(k) = P(k)- * H' * (H * P(k)- * H' + R)^-1
        // K(k) = P(k)- * H' * S^-1

        // instead of calculating the inverse of S we can rearrange the formula,
        // and then solve the linear equation A x X = B with A = S', X = K' and B = (H * P)'

        // K(k) * S = P(k)- * H'
        // S' * K(k)' = H * P(k)-'
//        RealMatrix kalmanGain = new CholeskyDecomposition(s).getSolver()
//                .solve(measurementMatrix.multiply(errorCovariance.transpose()))
//                .transpose();
        Matrix HPt = MatrixUtils.multiply(emit, MatrixUtils.transpose(hiddenState.getCovar()));
		// sanity check
		assert DataUtils.isSafe(HPt);
		
        Matrix kalmanGain = MatrixUtils.transpose(MatrixUtils.solve(S, HPt));

        // update estimate with measurement z(k)
        // xHat(k) = xHat(k)- + K * Inn
        Vector hiddenState2 = hiddenState.getMean().copy();
		hiddenState2.add(MatrixUtils.apply(kalmanGain, innovation));

        // update covariance of prediction error
        // P(k) = (I - K * H) * P(k)-
        Matrix I_KH = Matrices.identity(kalmanGain.numRows());
        I_KH.add(-1, MatrixUtils.multiply(kalmanGain, emit));
        Matrix errorCovar2 = MatrixUtils.multiply(I_KH, hiddenState.getCovar());
        
        return new Gaussian(hiddenState2, errorCovar2);
    }
    

	private Vector getPredictedObservation(Vector mean) {
    	Vector prediction = apply(emit, mean);
    	if (emitOffset!=null) prediction.add(emitOffset);
    	return prediction;
	}

	@Override
	public boolean isReady() {
		return trans!=null && emit!=null && transNoise!=null && emitNoise!=null;
	}

	@Override
	public void resetup() {
		this.trainData = null;
		// ID matrices
		if (trainTrans) {
			trans = new IdentityMatrix(numHiddenDimensions);
		}
		if (trainEmit || emit==null) {			
			if (numHiddenDimensions==numObservedDimensions) {
				emit = new IdentityMatrix(numHiddenDimensions);	
			} else if (numHiddenDimensions > numObservedDimensions) {
				boolean[] varmask = new boolean[numHiddenDimensions]; // TODO some true!!!
				emit = MatrixUtils.getDropDimensionsMatrix(varmask);
			} else {
				boolean[] varmask = new boolean[numHiddenDimensions]; // TODO some true!!!
				Matrix _emit = MatrixUtils.getDropDimensionsMatrix(varmask);
				emit = _emit.transpose(new DenseMatrix(numObservedDimensions, numHiddenDimensions));
			}
		}
	}

	@Override
	public void train(Iterable<? extends Vector> data) throws UnsupportedOperationException {
		assert this.trainData==null || this.trainData.isEmpty();
		this.trainData = new ListDataStream(data, true);		
	}

	@Override
	public void train1(Vector data) throws UnsupportedOperationException {
		if (trainData==null) trainData = new ListDataStream(data.size());
		trainData.insert(Datum.datum(data));
	}

	public Matrix getTransitionNoise() {
		return transNoise;
	}

	public KalmanFilter setTransitionMatrix(Matrix a) {
		trans = a;
		return this;
	}

	public KalmanFilter setTransitionNoise(Matrix Q) {
		transNoise = Q;
		return this;
	}

	public KalmanFilter setEmissionMatrix(Matrix H) {
		emit = H;
		return this;
	}

	public KalmanFilter setEmissionNoise(Matrix R) {
		emitNoise = R;
		return this;
	}

	@Override
	public List<IDistribution> smooth(IDistribution initialState, IDataStream observations) {
		List<IDistribution> filtered = new ArrayList();
		IDistribution state = initialState;
		ListDataStream obs = observations.list();
		int n = obs.size();
				
//		// sanity check: also run Apache's version
//		ProcessModel pm = new DefaultProcessModel(MatrixUtils.commons(trans), null, MatrixUtils.commons(transNoise), null, null);
//		MeasurementModel mm = new DefaultMeasurementModel(MatrixUtils.commons(emit), MatrixUtils.commons(emitNoise));
//		org.apache.commons.math3.filter.KalmanFilter akf = new org.apache.commons.math3.filter.KalmanFilter(pm,mm);
		// ...which fails, I think because it requires invertible matrices at each step
		
		for (int i = 0; i < n; i++) {			
			Datum obsi = obs.get(i);
			
//			if (transOffset!=null) akf.predict(DataUtils.toArray(transOffset));
//			else 
//				akf.predict();
//			akf.correct(DataUtils.toArray(obsi));
//			double[] akfState = akf.getStateEstimation();
//			double[][] akfError = akf.getErrorCovariance();
			
			Gaussian state2 = filter(state, obsi);			
			assert DataUtils.isSafe(state2.getMean()) : state2;
			assert DataUtils.isSafe(state2.getCovar()) : state2;
			filtered.add(state2);
			state = state2;
		}
		
		// TODO smoothing!
		// See https://en.wikipedia.org/wiki/Kalman_filter#Rauch.E2.80.93Tung.E2.80.93Striebel
		
		return filtered;
	}

	@Override
	public Dt getTimeStep() {
		return null; // whatever
	}

	public void setEmissionOffset(Vector emitOffset) {
		this.emitOffset = emitOffset;
	}

}

