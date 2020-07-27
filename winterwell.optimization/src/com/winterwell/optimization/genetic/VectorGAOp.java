package com.winterwell.optimization.genetic;

import java.util.Random;

import com.winterwell.maths.stats.distributions.BiDiExponentialDistribution1D;
import com.winterwell.maths.stats.distributions.ExponentialBall;
import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.stats.distributions.discrete.RandomChoice;
import com.winterwell.utils.Utils;

import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;

/**
 * Generate, mutate and crossover for real-valued vectors.
 * @author Daniel
 *
 */
public class VectorGAOp implements IBreeder<Vector>
{

	private IDistribution generator;
	/**
	 * We use x' = x + m*x, so the mutation is always relative to the value
	 */
	private IDistribution1D mutations = new BiDiExponentialDistribution1D(0.25);
	
	private double mutation;

	public VectorGAOp(IDistribution generator) {
		this.generator = generator;
		int dims = generator.getDim();
		mutation = Math.pow(0.75, dims);
	}
	
	public VectorGAOp(int dims) {
		this(new ExponentialBall(dims, 1));		
	}

	/**
	 * @param mutation probability of each component being mutated 
	 * @param mutations The step to be applied
	 * to vector components is relative to the current value: dx = mutation-sample * x.
	 * Unless the current value is zero, in which case m is used directly.
	 * Hence mutations should normally generate in the [-0.25, 0.25] sort of range
	 */
	public void setMutationGenerator(double mutation, IDistribution1D mutations) {
		this.mutation = mutation;
		this.mutations = mutations;
	}
	
	@Override
	public Vector generate() {		
		return generator.sample();
	}

	final RandomChoice random = new RandomChoice();
	
	@Override
	public Vector mutate(Vector candidate) {
		Vector mutant = candidate.copy();
		for(VectorEntry ve : mutant) {
			if ( ! Utils.getRandomChoice(mutation)) continue;
			double x = ve.get();
			double x2 = mutate2(x);
			ve.set(x2);
		}
		return mutant;
	}

	/**
	 * The mutation size is relative to x (except when x is zero).
	 * @param x
	 * @return mutated x
	 */
	public double mutate2(double x) {
		double m = mutations.sample();
		// mutation is relative to x, except when x is zero
		if (x==0) return m; 
		return x + m*x;			
	}
	
	@Override
	public Vector crossover(Vector a, Vector b) {
		assert a.size() == b.size() : "size mismatch: "+a.size()+"v"+b.size();
		if (a.size()<2) {
			// Too small to crossover
			return random.sample()? a : b;
		}
		int switchDim = 1+Utils.getRandom().nextInt(a.size()-1);
		// this is NOT optimised for sparse vectors
		Vector offspring = a.copy();
		for(int i=switchDim,n=a.size(); i<n; i++) {
			offspring.set(i, b.get(i));
		}
		return offspring;
	}

	@Override
	public void setRandomSource(Random r) {
		random.setRandomSource(r);
		generator.setRandomSource(r);
		mutations.setRandomSource(r);
	}

	public int getDim() {
		return generator.getDim();
	}

}
