package com.winterwell.maths.montecarlo;

import java.util.ArrayList;
import java.util.Random;

import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.utils.containers.Range;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

public class ParticleCloud extends ArrayList<Particle> implements IDistribution {

	private static final long serialVersionUID = 1L;

	@Override
	public boolean isNormalised() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void normalise() throws UnsupportedOperationException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Vector sample() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRandomSource(Random randomSrc) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double density(Vector x) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Matrix getCovar() {
		Matrix m = StatsUtils.covar(this);
		return m;
	}

	@Override
	public int getDim() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Vector getMean() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Range[] getSupport() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector getVariance() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double logDensity(Vector x) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double prob(Vector minCorner, Vector maxCorner) {
		// TODO Auto-generated method stub
		return 0;
	}


}
