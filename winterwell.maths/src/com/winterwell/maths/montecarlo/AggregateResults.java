package com.winterwell.maths.montecarlo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.winterwell.maths.datastorage.DataSet;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.ListMap;
import com.winterwell.utils.time.Time;

/**
 * Collect and merge results from many particles.
 * 
 * @author Daniel
 * 
 */
public class AggregateResults {

	private DataSet dataset;
	List<ListDataStream> traces = new ArrayList<ListDataStream>();

	/**
	 * @param data
	 *            The history from one particle's run
	 */
	public void addTrace(IDataStream data) {
		traces.add(new ListDataStream(data));
	}

	/**
	 * Get merged data for a specific dimension. This is either the mean value
	 * at each time step, or +/- n standard deviations from the mean.
	 * 
	 * @param stdDevs
	 *            0 for the mean
	 * @return a 1D data stream ??Note: this isn't terribly efficient as its
	 *         likely to be called 3 times
	 */
	public IDataStream getConfidenceLine(int dim, double stdDevs) {
		ListMap<Time, Double> data = new ListMap<Time, Double>();
		for (ListDataStream trace : traces) {
			trace = trace.clone();
			for (Datum d : trace) {
				data.add(d.time, d.get(dim));
			}
		}
		List<Time> times = new ArrayList<Time>(data.keySet());
		Collections.sort(times);
		ListDataStream line = new ListDataStream(1);
		for (Time t : times) {
			List<Double> values = data.get(t);
			double[] xs = MathUtils.toArray(values);
			double x = StatsUtils.mean(xs);
			if (stdDevs != 0) {
				double sd = Math.sqrt(StatsUtils.var(xs));
				x += stdDevs * sd;
			}
			line.add(new Datum(t, x, null));
		}
		return line;
	}

	public DataSet getDataSet() {
		return dataset;
	}

	public int numTraces() {
		return traces.size();
	}

	public void setDataset(DataSet dataset) {
		this.dataset = dataset;
	}

}
