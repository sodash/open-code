package com.winterwell.maths.montecarlo;

import java.util.Iterator;

import com.winterwell.maths.timeseries.Datum;
import com.winterwell.utils.time.Time;

import no.uib.cipr.matrix.AbstractVector;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;

/**
 * A weighted time-stamped vector.
 * 
 * @author Daniel
 */
public final class Particle 
extends AbstractVector 
implements Vector
{
	private static final long serialVersionUID = 1L;

	Datum state;

	double weight;

	@Override
	public double get(int index) {
		return state.get(index);
	}
	@Override
	public Iterator<VectorEntry> iterator() {
		return state.iterator();
	}
	
	public Particle(Datum state, double weight) {
		super(state);
		this.state = state;
		this.weight = weight;
	}

	public Time getTime() {
		return state.getTime();
	}

	public Datum getVector() {
		return state;
	}

	public void setState(Vector x, Time t) {
		state = new Datum(t, x, state.getLabel());
	}
	
	@Override
	public void set(int index, double value) {
		state.set(index, value);
	}
	
	@Override
	public Particle copy() {
		return new Particle(state.copy(), weight);
	}
	
}
