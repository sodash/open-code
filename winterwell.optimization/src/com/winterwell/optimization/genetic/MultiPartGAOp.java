/**
 * 
 */
package com.winterwell.optimization.genetic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import winterwell.optimization.genetic.MultiPartGAOpTest;

/**
 * A mutator for things made up of several smaller mutatable things.
 * Example Use: in a GA where a candidate consists of a few vectors + some strings. 
 * 
 * @testedby  MultiPartGAOpTest}
 * @author daniel
 */
public class MultiPartGAOp implements IBreeder<List> {

	private IBreeder[] breeders;

	public MultiPartGAOp(IBreeder... breeders) {
		this.breeders = breeders;
	}
	public MultiPartGAOp(List<IBreeder> breeders) {
		this(breeders.toArray(new IBreeder[0]));
	}
	
	@Override
	public List crossover(List a, List b) {
		assert a.size() == b.size() : a+" vs "+b;
		assert a.size() == breeders.length : a;
		List c = new ArrayList(a.size());
		for(int i=0; i<a.size(); i++) {
			Object ai = a.get(i);
			Object bi = b.get(i);
			IBreeder op = breeders[i];
			Object ci = op.crossover(ai, bi);
			c.add(ci);
		}
		return c;
	}

	@Override
	public List generate() {
		List c = new ArrayList(breeders.length);
		for(IBreeder b : breeders) {
			Object ci = b.generate();
			c.add(ci);
		}
		return c;
	}

	@Override
	public List mutate(List a) {
		assert a.size() == breeders.length : a;
		List c = new ArrayList(a.size());
		for(int i=0; i<a.size(); i++) {
			Object ai = a.get(i);
			IBreeder op = breeders[i];
			Object ci = op.mutate(ai);
			c.add(ci);
		}
		return c;
	}

	@Override
	public void setRandomSource(Random seed) {
		for (IBreeder b : breeders) {
			b.setRandomSource(seed);
		}
	}

	
}
