/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.winterwell.maths.stats.algorithms;

import static com.winterwell.maths.matrix.MatrixUtils.mtj;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Precision;
import org.junit.Assert;
import org.junit.Test;

import com.winterwell.maths.matrix.IdentityMatrix;
import com.winterwell.maths.matrix.Matrix1D;
import com.winterwell.maths.matrix.MatrixUtils;
import com.winterwell.maths.stats.distributions.Gaussian;
import com.winterwell.maths.stats.distributions.GaussianBall;
import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.maths.vector.X;
import com.winterwell.maths.vector.XY;
import com.winterwell.utils.containers.Range;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.Vector.Norm;

/**
 * Tests for {@link KalmanFilter}. Adapted from the Apache Commons Math3 test
 *
 */
public class KalmanFilterTest {

	/**
	 * Check that observations reduce even very large errors
	 */
    @Test
    public void testCorrectingABigError() {
    	KalmanFilter kf = new KalmanFilter(2, 2);
    	// data: random walk with drift
    	GaussianBall gb = new GaussianBall(new XY(1,2), 2);
    	List<Vector> obs = new ArrayList();
    	Vector x = new XY(0,0);
    	obs.add(x);
    	for(int i=0; i<100; i++) {
    		Vector dx = gb.sample();
    		x = x.copy().add(dx);
    		obs.add(x);
    	}
    	kf.train(obs);
    	kf.finishTraining();
    	double score = kf.getScore();
    	System.out.println(score);
    	
    	double s2len;
    	// Now try a big error    	
	    	Gaussian state = new Gaussian(new XY(1000,1000), new IdentityMatrix(2,1000000));
			Gaussian state2 = kf.filter(state, new XY(0,0));
			s2len = state2.getMean().norm(Norm.Two);
			assert state2.getMean().norm(Norm.Two) < state.getMean().norm(Norm.Two);
			assert s2len < 100;
    			
    	// a big error with more confident wrongness    	
	    	Gaussian stateb = new Gaussian(new XY(1000,1000), new IdentityMatrix(2,1));
			Gaussian state2b = kf.filter(stateb, new XY(0,0));
			double s2lenb = state2b.getMean().norm(Norm.Two);
			assert state2b.getMean().norm(Norm.Two) < stateb.getMean().norm(Norm.Two);
			assert s2lenb < 1000 : s2lenb;
			assert s2lenb > 100 : s2lenb;
			assert s2lenb > 2*s2len;
    	
    	// a big error with off-centre observation    	
	    	Gaussian statec = new Gaussian(new XY(1000,1000), new IdentityMatrix(2,1));
			Gaussian state2c = kf.filter(statec, new XY(1,2));
			double s2lenc = state2c.getMean().norm(Norm.Two);
			assert state2c.getMean().norm(Norm.Two) < statec.getMean().norm(Norm.Two);
			assert s2lenc < 1000 : s2lenc;
			assert s2lenc > 100 : s2lenc;
			assert s2lenc > 2*s2len;
    	
    }
    
    
    /**
	 * Check that observations reduce even very large errors
	 */
    @Test
    public void testCorrectingABigError1D() {
    	KalmanFilter kf = new KalmanFilter(1, 1);
    	// data: random walk with drift
    	GaussianBall gb = new GaussianBall(new X(1), 2);
    	List<Vector> obs = new ArrayList();
    	Vector x = new X(0);
    	obs.add(x);
    	for(int i=0; i<100; i++) {
    		Vector dx = gb.sample();
    		x = x.copy().add(dx);
    		obs.add(x);
    	}
    	kf.train(obs);
    	kf.finishTraining();
    	double score = kf.getScore();
    	System.out.println(score);
    	
    	double s2len;
    	// Now try a big error    	
	    	Gaussian state = new Gaussian(new X(1000), new IdentityMatrix(1,1000000));
			Gaussian state2 = kf.filter(state, new X(0));
			s2len = state2.getMean().norm(Norm.Two);
			assert state2.getMean().norm(Norm.Two) < state.getMean().norm(Norm.Two);
			assert s2len < 100;
    			
    	// a big error with more confident wrongness 
	    	Gaussian stateb = new Gaussian(new X(1000), new IdentityMatrix(1,0.0000001));
			Gaussian state2b = kf.filter(stateb, new X(0));
			double s2lenb = state2b.getMean().norm(Norm.Two);
			assert state2b.getMean().norm(Norm.Two) < stateb.getMean().norm(Norm.Two);
			assert s2lenb < 1000 : s2lenb;
			assert s2lenb > 1 : s2lenb;
			assert s2lenb > 1.5*s2len;
    	
    	// a big error with off-centre observation    	
	    	Gaussian statec = new Gaussian(new X(1000), new IdentityMatrix(1,1));
			Gaussian state2c = kf.filter(statec, new X(2));
			double s2lenc = state2c.getMean().norm(Norm.Two);
			assert state2c.getMean().norm(Norm.Two) < statec.getMean().norm(Norm.Two);
			assert s2lenc < 1000 : s2lenc;
			assert s2lenc > 2 : s2lenc;
			assert s2lenc > 2*s2len;
    	
    }
	
	
    @Test
    public void testConstant() {
        // simulates a simple process with a constant state and no control input

        double constantValue = 10d;
        double measurementNoise = 0.1d;
        double processNoise = 1e-5d;

        // A = [ 1 ]
        Matrix A = new Matrix1D(1);
        // no control input
        RealMatrix B = null;
        // H = [ 1 ]
        Matrix H = new Matrix1D(1);
        // x = [ 10 ]
        Vector x = new X(constantValue);
        // Q = [ 1e-5 ]
        Matrix Q = new Matrix1D(processNoise);
        // R = [ 0.1 ]
        Matrix R = new Matrix1D(measurementNoise);

//        ProcessModel pm
//            = new DefaultProcessModel(A, B, Q,
//                                      new ArrayRealVector(new double[] { constantValue }), null);
//        MeasurementModel mm = new DefaultMeasurementModel(H, R);

        KalmanFilter filter = new KalmanFilter(1, 1);
        filter.setTransitionMatrix(A);
        filter.setTransitionNoise(Q);
        filter.setEmissionMatrix(H);
        filter.setEmissionNoise(R);
        

        Assert.assertEquals(1, filter.getMeasurementDimension());
        Assert.assertEquals(1, filter.getStateDimension());

        MatrixUtils.equals(Q, filter.getTransitionNoise());

//        // check the initial state
//        double[] expectedInitialState = new double[] { constantValue };
//        assertVectorEquals(expectedInitialState, filter.getStateEstimation());

        Gaussian hiddenState = new Gaussian(new X(constantValue), new Matrix1D(0));
        
        Vector pNoise = new X(0);
        Vector mNoise = new X(0);

        RandomGenerator rand = new JDKRandomGenerator();
        // iterate 60 steps
        for (int i = 0; i < 60; i++) {            
        	hiddenState = filter.predict(hiddenState, null);

            // Simulate the process
            pNoise.set(0, processNoise * rand.nextGaussian());

            // x = A * x + p_noise
            x = MatrixUtils.apply(A, x).add(pNoise);

            // Simulate the measurement
            mNoise.set(0, measurementNoise * rand.nextGaussian());

            // z = H * x + m_noise
            Vector z = MatrixUtils.apply(H, x).add(mNoise);

            hiddenState = filter.correct(hiddenState, z);

            // state estimate shouldn't be larger than measurement noise
            double diff = FastMath.abs(constantValue - hiddenState.getMean().get(0));
            // System.out.println(diff);
            Assert.assertTrue(Precision.compareTo(diff, measurementNoise, 1e-6) < 0);
        }

        // error covariance should be already very low (< 0.02)
        Assert.assertTrue(Precision.compareTo(hiddenState.getCovar().get(0, 0),
                                              0.02d, 1e-6) < 0);
    }

    @Test
    public void testConstantAcceleration() {
        // simulates a vehicle, accelerating at a constant rate (0.1 m/s)

        // discrete time interval
        double dt = 0.1d;
        // position measurement noise (meter)
        double measurementNoise = 10d;
        // acceleration noise (meter/sec^2)
        double accelNoise = 0.2d;

        // A = [ 1 dt ]
        //     [ 0  1 ]
        RealMatrix A = new Array2DRowRealMatrix(new double[][] { { 1, dt }, { 0, 1 } });

        // B = [ dt^2/2 ]
        //     [ dt     ]
        RealMatrix Bnull = new Array2DRowRealMatrix(
                new double[][] { { FastMath.pow(dt, 2d) / 2d }, { dt } });

        // H = [ 1 0 ]
        RealMatrix H = new Array2DRowRealMatrix(new double[][] { { 1d, 0d } });

        // x = [ 0 0 ]
        RealVector x = new ArrayRealVector(new double[] { 0, 0 });

        RealMatrix tmp = new Array2DRowRealMatrix(
                new double[][] { { FastMath.pow(dt, 4d) / 4d, FastMath.pow(dt, 3d) / 2d },
                                 { FastMath.pow(dt, 3d) / 2d, FastMath.pow(dt, 2d) } });

        // Q = [ dt^4/4 dt^3/2 ]
        //     [ dt^3/2 dt^2   ]
        RealMatrix Q = tmp.scalarMultiply(FastMath.pow(accelNoise, 2));

        // P0 = [ 1 1 ]
        //      [ 1 1 ]
        RealMatrix P0 = new Array2DRowRealMatrix(new double[][] { { 1, 1 }, { 1, 1 } });

        // R = [ measurementNoise^2 ]
        RealMatrix R = new Array2DRowRealMatrix(
                new double[] { FastMath.pow(measurementNoise, 2) });

        // constant control input, increase velocity by 0.1 m/s per cycle
        double uv = 0.1d*dt;
        RealVector u = new ArrayRealVector(new double[] {uv*uv/2, uv});

        ProcessModel pm = new DefaultProcessModel(A, Bnull, Q, x, P0);
        MeasurementModel mm = new DefaultMeasurementModel(H, R);
        KalmanFilter filter = new KalmanFilter(pm, mm);

        Assert.assertEquals(1, filter.getMeasurementDimension());
        Assert.assertEquals(2, filter.getStateDimension());
        
        Gaussian state = new Gaussian(mtj(x), mtj(P0));
        MatrixUtils.equals(mtj(P0), state.getCovar());

        // check the initial state
        double[] expectedInitialState = new double[] { 0.0, 0.0 };
//        assertVectorEquals(expectedInitialState, filter.getStateEstimation());

        RandomGenerator rand = new JDKRandomGenerator();

        RealVector tmpPNoise = new ArrayRealVector(
                new double[] { FastMath.pow(dt, 2d) / 2d, dt });

        // iterate 60 steps
        for (int i = 0; i < 60; i++) {
            state = filter.predict(state, mtj(u));

            // Simulate the process
            RealVector pNoise = tmpPNoise.mapMultiply(accelNoise * rand.nextGaussian());

            // x = A * x + B * u + pNoise
            x = A.operate(x).add(u).add(pNoise);

            // Simulate the measurement
            double mNoise = measurementNoise * rand.nextGaussian();

            // z = H * x + m_noise
            RealVector z = H.operate(x).mapAdd(mNoise);

            state = filter.correct(state, mtj(z));

            // state estimate shouldn't be larger than the measurement noise
            double diff = FastMath.abs(x.getEntry(0) - state.getMean().get(0));
            Assert.assertTrue(Precision.compareTo(diff, measurementNoise, 1e-6) < 0);
        }

        // error covariance of the velocity should be already very low (< 0.1)
        Assert.assertTrue(Precision.compareTo(state.getCovar().get(1,1),
                                              0.1d, 1e-6) < 0);
    }

    /**
     * Represents an idealized Cannonball only taking into account gravity.
     */
    public static class Cannonball {

        private final double[] gravity = { 0, -9.81 };

        private final double[] velocity;
        private final double[] location;

        private double timeslice;

        public Cannonball(double timeslice, double angle, double initialVelocity) {
            this.timeslice = timeslice;

            final double angleInRadians = FastMath.toRadians(angle);
            this.velocity = new double[] {
                    initialVelocity * FastMath.cos(angleInRadians),
                    initialVelocity * FastMath.sin(angleInRadians)
            };

            this.location = new double[] { 0, 0 };
        }

        public double getX() {
            return location[0];
        }

        public double getY() {
            return location[1];
        }

        public double getXVelocity() {
            return velocity[0];
        }

        public double getYVelocity() {
            return velocity[1];
        }

        public void step() {
            // break gravitational force into a smaller time slice.
            double[] slicedGravity = gravity.clone();
            for ( int i = 0; i < slicedGravity.length; i++ ) {
                slicedGravity[i] *= timeslice;
            }

            // apply the acceleration to velocity.
            double[] slicedVelocity = velocity.clone();
            for ( int i = 0; i < velocity.length; i++ ) {
                velocity[i] += slicedGravity[i];
                slicedVelocity[i] = velocity[i] * timeslice;
                location[i] += slicedVelocity[i];
            }

            // cannonballs shouldn't go into the ground.
            if ( location[1] < 0 ) {
                location[1] = 0;
            }
        }
    }

    @Test
    public void testCannonball() {
        // simulates the flight of a cannonball (only taking gravity and initial thrust into account)

        // number of iterations
        final int iterations = 144;
        // discrete time interval
        final double dt = 0.1d;
        // position measurement noise (meter)
        final double measurementNoise = 30d;
        // the initial velocity of the cannonball
        final double initialVelocity = 100;
        // shooting angle
        final double angle = 45;

        final Cannonball cannonball = new Cannonball(dt, angle, initialVelocity);

        final double speedX = cannonball.getXVelocity();
        final double speedY = cannonball.getYVelocity();

        // A = [ 1, dt, 0,  0 ]  =>  x(n+1) = x(n) + vx(n)
        //     [ 0,  1, 0,  0 ]  => vx(n+1) =        vx(n)
        //     [ 0,  0, 1, dt ]  =>  y(n+1) =              y(n) + vy(n)
        //     [ 0,  0, 0,  1 ]  => vy(n+1) =                     vy(n)
        final Matrix A = new DenseMatrix(new double[][] {
                { 1, dt, 0,  0 },
                { 0,  1, 0,  0 },
                { 0,  0, 1, dt },
                { 0,  0, 0,  1 }
        });

        // The control vector, which adds acceleration to the kinematic equations.
        // 0          =>  x(n+1) =  x(n+1)
        // 0          => vx(n+1) = vx(n+1)
        // -9.81*dt^2 =>  y(n+1) =  y(n+1) - 1/2 * 9.81 * dt^2
        // -9.81*dt   => vy(n+1) = vy(n+1) - 9.81 * dt
        final Vector controlVector =
                new DenseVector(new double[] { 0, 0, 0.5 * -9.81 * dt * dt, -9.81 * dt } );

//        // The control matrix B only expects y and vy, see control vector
//        final Matrix B = MatrixUtils.createRealMatrix(new double[][] {
//                { 0, 0, 0, 0 },
//                { 0, 0, 0, 0 },
//                { 0, 0, 1, 0 },
//                { 0, 0, 0, 1 }
//        });

        // We only observe the x/y position of the cannonball
        final Matrix H = new DenseMatrix(new double[][] {
                { 1, 0, 0, 0 },
                { 0, 0, 0, 0 },
                { 0, 0, 1, 0 },
                { 0, 0, 0, 0 }
        });

        // our guess of the initial state.
        final Vector initialState = new DenseVector(new double[] { 0, speedX, 0, speedY } );

        // the initial error covariance matrix, the variance = noise^2
        final double var = measurementNoise * measurementNoise;
        final Matrix initialErrorCovariance = new DenseMatrix(new double[][] {
                { var,    0,   0,    0 },
                {   0, 1e-3,   0,    0 },
                {   0,    0, var,    0 },
                {   0,    0,   0, 1e-3 }
        });

        // we assume no process noise -> zero matrix
        final Matrix Q = new DenseMatrix(4, 4);

        // the measurement covariance matrix
        final Matrix R = new DenseMatrix(new double[][] {
                { var,    0,   0,    0 },
                {   0, 1e-3,   0,    0 },
                {   0,    0, var,    0 },
                {   0,    0,   0, 1e-3 }
        });

//        final ProcessModel pm = new DefaultProcessModel(A, B, Q, initialState, initialErrorCovariance);
//        final MeasurementModel mm = new DefaultMeasurementModel(H, R);
        final KalmanFilter filter = new KalmanFilter(4, 4);
        filter.setTransitionMatrix(A);
        filter.setTransitionNoise(Q);
        filter.setEmissionMatrix(H);
        filter.setEmissionNoise(R);
        
        Gaussian state = new Gaussian(initialState, initialErrorCovariance);
        
        final RandomGenerator rng = new Well19937c(1000);
        final NormalDistribution dist = new NormalDistribution(rng, 0, measurementNoise);

        for (int i = 0; i < iterations; i++) {
            // get the "real" cannonball position
            double x = cannonball.getX();
            double y = cannonball.getY();

            // apply measurement noise to current cannonball position
            double nx = x + dist.sample();
            double ny = y + dist.sample();

            cannonball.step();

            state = filter.predict(state, controlVector);
            // correct the filter with our measurements
            state = filter.correct(state, new DenseVector(new double[] { nx, 0, ny, 0 } ));

            // state estimate shouldn't be larger than the measurement noise
            double diff = FastMath.abs(cannonball.getY() - state.getMean().get(2));
            Assert.assertTrue(Precision.compareTo(diff, measurementNoise, 1e-6) < 0);
        }

        // error covariance of the x/y-position should be already very low (< 3m std dev = 9 variance)

        Assert.assertTrue(Precision.compareTo(state.getCovar().get(0,0),
                                              9, 1e-6) < 0);

        Assert.assertTrue(Precision.compareTo(state.getCovar().get(2,2),
                                              9, 1e-6) < 0);
    }
    
    @Test
    public void testEM() {
    	KalmanFilter gen = new KalmanFilter(2, 2);
    	Matrix a = new DenseMatrix(new double[][]{
    			{1.0, 0.1}, {0.1, 0.9}
    	});
		gen.setTransitionMatrix(a);
		Matrix Q = new IdentityMatrix(2, 0.1);
		gen.setTransitionNoise(Q);
    	Matrix H = new IdentityMatrix(2);
		gen.setEmissionMatrix(H);
		Matrix R = new IdentityMatrix(2, 0.1);
		gen.setEmissionNoise(R);
		Gaussian emitNoise = new Gaussian(new XY(0,0), R);
		Gaussian initialState = new Gaussian(new XY(1,1), new IdentityMatrix(2, 0.1));
		Gaussian state = initialState;
		List<Gaussian> trueHidden = new ArrayList();
		List<Vector> observations = new ArrayList();
		for(int i=0; i<20; i++) {
			trueHidden.add(state);
			Vector x = state.sample();
			x = MatrixUtils.apply(H, x);
			x.add(emitNoise.sample());
			observations.add(x);
			Gaussian state2 = gen.predict(state, null);
			state = state2;
		}
		
		System.out.println(trueHidden);
		System.out.println(observations);
		
		// Now let's fit it
		
		KalmanFilter kf = new KalmanFilter(2, 2);
		kf.trainEmit = true;
		kf.trainTrans = true;
		kf.train(observations);		
		kf.finishTraining();
		
		// How did we do?
		
		List<IDistribution> smoothed = kf.smooth(initialState, new ListDataStream(observations));
		int n = smoothed.size();
		assert n== observations.size();
		for (int i = 0; i <n; i++) {
			IDistribution est = smoothed.get(i);
			Gaussian was = trueHidden.get(i);
			Vector residual = was.getMean().copy().add(-1, est.getMean());
			System.out.println(was.getMean()+"	residual: "+residual.norm(Norm.Two));
		}
    }
    
    
    /**
     * Test with random trans and emit matrices, and offset
     */
//    @Test This produces singular matrices too easily
    public void testEMRandom() {
    	for(int tst=0; tst<10; tst++) {
	    	KalmanFilter gen = new KalmanFilter(2, 2);
	    	Matrix trueTRans = MatrixUtils.getRandomDenseMatrix(2, 2, new Range(-0.5, 1.5));
			gen.setTransitionMatrix(trueTRans);
			Matrix Q = new IdentityMatrix(2, 0.1);
			gen.setTransitionNoise(Q);
	    	Matrix H = MatrixUtils.getRandomDenseMatrix(2, 2, new Range(0, 2));
			gen.setEmissionMatrix(H);
			Matrix R = new IdentityMatrix(2, 0.1);
			gen.setEmissionNoise(R);
			Vector trueEmitOffset = new GaussianBall(new XY(0,0), 2).sample();
			gen.setEmissionOffset(trueEmitOffset);
			
			// make some data
			Gaussian emitNoise = new Gaussian(new XY(0,0), R);
			Gaussian initialState = new Gaussian(new XY(1,1), new IdentityMatrix(2, 0.1));
			Gaussian state = initialState;
			List<Gaussian> trueHidden = new ArrayList();
			List<Vector> observations = new ArrayList();
			for(int i=0; i<100; i++) {
				trueHidden.add(state);
				Vector x = state.sample();
				x = MatrixUtils.apply(H, x);
				x.add(emitNoise.sample());
				observations.add(x);
				Gaussian state2 = gen.predict(state, null);
				state = state2;
			}
			
			System.out.println(trueHidden);
			System.out.println(observations);
			
			// Now let's fit it
			
			KalmanFilter kf = new KalmanFilter(2, 2);
			kf.trainEmit = true;
			kf.trainTrans = true;
			kf.train(observations);		
			kf.finishTraining();
			
			// How did we do?
			
			List<IDistribution> smoothed = kf.smooth(initialState, new ListDataStream(observations));
			int n = smoothed.size();
			assert n== observations.size();
			for (int i = 0; i <n; i++) {
				IDistribution est = smoothed.get(i);
				Gaussian was = trueHidden.get(i);
				Vector residual = was.getMean().copy().add(-1, est.getMean());
				System.out.println(was.getMean()+"	residual: "+residual.norm(Norm.Two));
			}
    	}
    }

}