package com.winterwell.maths.montecarlo;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.maths.stats.algorithms.ITimeSeriesFilter;
import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;

import no.uib.cipr.matrix.Vector;

public class ParticleFilter implements ITimeSeriesFilter 
{

	private ParticleCloud cloud;

	int cloudSize = 1000;

	ListDataStream data;

	IDistribution generator;

	/**
	 * The current particle
	 */
	private Particle particle;

	private AggregateResults results;

	ISimulator simulator;

	private Dt timeStep;

	public ParticleFilter(IDistribution generator, ISimulator simulator,
			Dt timeStep) {
		this.generator = generator;
		this.simulator = simulator;
		this.timeStep = timeStep;
	}

	public AggregateResults getResults() {
		return results;
	}

	/**
	 * Generate a particle cloud and advance it for period
	 * 
	 * @param period
	 */
	public void run(Period period) {
		// make cloud
		run2_generateCloud(period.getStart());
		// run
		run2_simCloud(period.length());
	}

	private void run2_generateCloud(Time start) {
		cloud = new ParticleCloud();
		for (int i = 0; i < cloudSize; i++) {
			// let's start with a simple generation strategy
			// - means that outliers are unlikely to be generated
			Vector x = generator.sample();
			Datum d = new Datum(start, x, null);
			Particle p = new Particle(d, 1);
			cloud.add(p);
		}
	}

	List<Particle> run2_simCloud(Dt period) {
		results = new AggregateResults();
		List<Particle> after = new ArrayList<Particle>(cloud.size());
		for (Particle p : cloud) {
			run3_simParticle(p, period);
			// results from that run
			results.addTrace(data);
			after.add(particle);
		}
		return after;
	}

	/**
	 * Advance the particle until it is period (or later).
	 * 
	 * @param start
	 * @param period
	 * @return weight of final particle
	 */
	double run3_simParticle(Particle start, Dt period) {
		// a fresh data collection object
		data = new ListDataStream(start.state.size());
		particle = start;
		Time endTime = start.state.time.plus(period);
		while (particle.state.time.longValue() < endTime.longValue()) {
			particle = simulator.sim1step(particle, timeStep);
			data.add(particle.state);
		}
		return particle.weight;
	}

	@Override
	public int getMeasurementDimension() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getStateDimension() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IDistribution filter(IDistribution state, Vector observation) {
//		cloud = ParticleCloud.toCloud(state, cloudSize);
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<IDistribution> smooth(IDistribution initialState, IDataStream observations) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dt getTimeStep() {
		return dt;
	}
	
	Dt dt;
}
