package com.winterwell.maths.chart;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.maths.timeseries.BucketedDataStream;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.maths.timeseries.TimeSlicer;
import com.winterwell.maths.vector.CyclicMetric;
import com.winterwell.maths.vector.XY;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

import no.uib.cipr.matrix.Vector;

/**
 * Plot a function over time.
 * <p>
 * For creating the data for these, you may find these classes useful:
 * 
 * @see ListDataStream
 * @see TimeSlicer
 * @see BucketedDataStream <p>
 *      If you have multi-valued data (e.g. you want multiple lines), then you
 *      need to use {@link CombinationChart} -- but you can use the convenience
 *      method #newMultiValuedChart().
 * @author daniel
 * 
 */
public class TimeSeriesChart extends XYChart {

	/**
	 * 
	 * @param data
	 * @param labels Dimension names. Can be null. 
	 * @return
	 */
	public static CombinationChart newMultiValuedChart(IDataStream data,
			List<String> labels, boolean skipZeroLines) 
	{
		ListDataStream dl = data.list();
		CombinationChart cc = new CombinationChart();
		int n = data.getDim();
		assert n != 0 : data.size() + " " + data + " " + labels;
		// No labels... Did the data supply it's own labels?
		if (labels==null && data instanceof ListDataStream) {
			labels = ((ListDataStream)data).getLabels();
		}
		assert labels==null || labels.size() == n : n+" vs "+labels.size()+" "+labels;
		// assert labels==null || n == labels.size() : labels;
		for (int i = 0; i < n; i++) {
			IDataStream data1d = DataUtils.get1D(dl.clone(), i);
			// Is it a zero line?
			if (skipZeroLines) {
				data1d = data1d.list();
				if (data1d.size() == 0) {
					Log.d("chart", "Skipping no-data line");
					continue;
				}
				double min = Double.POSITIVE_INFINITY;
				double max = Double.NEGATIVE_INFINITY;
				for (Datum datum : data1d) {
					double x = datum.x();
					if (x < min) min = x;
					if (x > max) max = x;
				}
				if (min==0 && max==0) {
					Log.d("chart", "Skipping zero line");
					continue;
				}
			}
			// Make the chart
			TimeSeriesChart chart = new TimeSeriesChart();
			chart.setData(data1d);
			if (labels != null) {
				chart.setTitle(labels.get(i));
			}
			cc.add(chart);
		}
		return cc;
	}

	/**
	 * As newMultiValuedChart(IDataStream ...), but accepts a list of IDataStream.
	 * This means we don't have to deal with time alignment between the different
	 * datastreams (which is necessary to create an ExtraDimensionsDataStream).
	 * These datastreams can be multidimensional (and each dimension will be
	 * plotted).
	 * @param data
	 * @param labels
	 * @return chart
	 */
	public static CombinationChart newMultiValuedChart(List<IDataStream> data,
			List<String> labels) {
		CombinationChart cc = new CombinationChart();
		int n = data.size();
		assert n != 0 : "Received empty list of IDataStream";
		Integer label_idx = 0;
		for (int i = 0; i < n; i++) {
			ListDataStream data_stream = new ListDataStream(data.get(i));			
			for (int j = 0; j < data_stream.getDim(); j++) {
				IDataStream data1d = DataUtils.get1D(data_stream.clone(), j);
				TimeSeriesChart chart = new TimeSeriesChart();
				chart.setData(data1d);
				if (labels != null) {
					chart.setTitle(labels.get(label_idx));
				}
				cc.add(chart);
				label_idx++;
			}
		}
		return cc;
	}

//	/**
//	 * Convenience method for eyeballing time series charts
//	 * 
//	 * @param clone
//	 */
//	public static void popup(String name, IDataStream data) {
//		TimeSeriesChart chart = new TimeSeriesChart();
//		chart.setTitle(name);
//		chart.setData(data);
//		Renderer render = Dep.get(Renderer.class);
//		render.renderToBrowser(chart);
//	}
//
//	/**
//	 * Convenience method for eyeballing time series charts
//	 * Uses JFreeChart, so no dependency on soda.sh
//	 * @param clone
//	 */
//	public static void popupLocal(String name, IDataStream data) {
//		TimeSeriesChart chart = new TimeSeriesChart();
//		chart.setTitle(name);
//		chart.setData(data);
//		chart.setType(ChartType.LINE);
//		Renderer render = Dep.get(Renderer.class);
//		render.renderAndPopupAndBlock(chart);
//	}

	private Dt period;

	private Time start;

	public TimeSeriesChart() {
		this.setType(ChartType.LINE);
		this.setShowPoints(true);
	}

	public TimeSeriesChart(String name) {
		super(name);
	}

	public TimeSeriesChart(String name, IDataStream data) {
		super(name, data);
	}

	public void setCyclic(Time start, Dt period) {
		assert this.getData() == null || this.getData().isEmpty() : "set data first";
		this.start = start;
		this.period = period;
	}

	/**
	 * @param points
	 *            Can be 2-dimensional vectors, where x is the time component in
	 *            UTC long-values. Or 1-dimensional time-stamped Datums, where y
	 *            is the value and x is the timestamp.
	 */
	@Override
	public void setData(Iterable<? extends Vector> points) {
		List<? extends Vector> list = setData2_toList(points);
		if (period != null) {
			list = setData2_mod(list);
		}
		super.setData(list);
	}

	// TEST ME
	private List<Vector> setData2_mod(List<? extends Vector> list) {
		CyclicMetric cm = new CyclicMetric(period.getMillisecs());
		double s = start.longValue();
		List<Vector> modList = new ArrayList<Vector>(list.size());
		XY prev = new XY(TimeUtils.ANCIENT.getTime(), 0);
		for (int i = 0; i < list.size(); i++) {
			Vector v = list.get(i);
			double t = v.get(0);
			t -= s;
			t = cm.canonical(t);
			// have we looped? insert an ersatz point to prevent ugly
			// jump-cut loop-lines
			if (t < prev.x && type == ChartType.LINE) {
				double pt = prev.x - cm.getPeriod();
				modList.add(new XY(pt, prev.y));
			}
			XY tv = new XY(t, v.get(1));
			modList.add(tv);
			prev = tv;
		}
		return modList;
	}

	/**
	 * 
	 * @param points
	 * @return x=time, y=value May or may not copy
	 */
	private List<? extends Vector> setData2_toList(
			Iterable<? extends Vector> points) {
		List<? extends Vector> list = Containers.getList(points);
		assert !list.isEmpty() : points;
		Vector pt = list.get(0);
		if (pt.size() == 2)
			// this is XY data
			return list;
		assert pt.size() == 1 : pt;
		assert pt instanceof Datum : pt;
		// this is time-stamped Y data
		ArrayList<XY> list2 = new ArrayList<XY>(list.size());
		for (Vector v : list) {
			Datum dv = (Datum) v;
			double y = dv.x();
			assert MathUtils.isFinite(y) : dv;
			XY xy = new XY(dv.time.longValue(), y);
			list2.add(xy);
		}
		return list2;
	}

	@Override
	protected void setData3_setupAxes2_adjustRange(int i, Axis axs, double min,
			double max) {
		if (axs instanceof TimeAxis) {
			((TimeAxis) axs).setRange(new Time(min), new Time(max));
			return;
		}
		super.setData3_setupAxes2_adjustRange(i, axs, min, max);
	}

	@Override
	protected NumericalAxis setData3_setupAxes2_newAxis(int i) {
		if (i == 0) {
			TimeAxis axis = new TimeAxis();
			return axis;
		}
		return super.setData3_setupAxes2_newAxis(i);
	}

}
