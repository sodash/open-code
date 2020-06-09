package com.winterwell.maths.chart;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.winterwell.json.JSONObject;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.containers.Range;

import no.uib.cipr.matrix.Vector;

/**
 * Adds Axes to AChart
 * TODO Merge with ACHart??
 *
 *
 */
public class Chart extends AChart {
	
	/**
	 * @deprecated Store in Series instead
	 * 0=X 1=Y 2=Z
	 */
	protected Axis[] axes;
	
	/**
	 * Color for the chart background. Can be ignored.
	 */
	Color bgColor = Color.WHITE;

	int dim;

	String drawType = "";

	// TODO merge with options
	// Custom options for Flot. This is a bit of a hack, but sometimes Flot
	// needs a
	// nudge in the right direction ....
	private Map<String, Object> flot_options = new HashMap<String, Object>();

	public Chart() {
	}

	public Chart(String name) {
		this.title = name;
	}

	public Chart(String name, Iterable<? extends Vector> data) {
		this.title = name;
		setData(data);
	}
	
	private double boostAxisRange(double x, boolean up) {
		if (x == 0)
			return 0;
		// add 10% on top
		x = x * 1.1;
		double v = Math.abs(x);
		double lv = Math.floor(Math.log10(v));
		double tens = Math.pow(10, lv);
		double keepMe = Math.ceil(x / tens); // + 1;
		double vt = keepMe * tens;
		return vt;
	}

	/**
	 * @Deprecated // Use Series instead
	 * @param index
	 * @return null if the axis is unset
	 */
	public Axis getAxis(int index) {
		return axes == null ? null : index < axes.length ? axes[index] : null;
	}

	/**
	 * @return color for this line / set of points. Often returns null, either
	 *         because it's not set, or a single color is not appropriate here.
	 */
	public Color getColor() {
		if (this.series == null) {
			return null;
		}
		
		return this.series.getColor();
	}
		
	protected Map<Integer, String> dataLabels;

	// TODO Move into the Axis object, merge with TimeAxis
	private TimeZone timeZone;

	protected boolean showPoints;

	public void setDataLabels(Map<Integer,String> index2label) {
		this.dataLabels = index2label;
	}
	
	/**
	 * Usually null
	 * @return index-in-series to label
	 */
	public Map<Integer, String> getDataLabels() {
		return dataLabels;
	}

	public boolean getShowPoints() {
		return this.showPoints;
	}
	
	public Map<String, Object> getFlotOptions() {
		return this.flot_options;
	}
	
	/**
	 * Normally, the axes are setup by the chart. This provides an over-ride
	 * (which should rarely be needed).
	 * 
	 * @param i
	 * @param axis
	 */
	public void setAxis(int i, Axis axis) {
		if (axes == null) {
			axes = new Axis[2];
		}
		axes[i] = axis;
		
		// settings
		if (timeZone!=null && axis instanceof TimeAxis) {
			((TimeAxis) axis).setTimeZone(timeZone);
		}
	}

	public void setBgColor(Color bgColor) {
		this.bgColor = bgColor;
	}

	public void setColor(Color color) throws UnsupportedOperationException {
		if (this.series == null) {
			this.series = new Series();
		}
		
		this.series.setColor(color);
	}
	
	public void setSeries(Series series) {
		this.series = series;
	}
	
	/**
	 * 
	 * @param data
	 *            Could be a list or an {@link IDataStream}. Must be 1D
	 *            (histogram), 2D (line or scatter plot), or 3D (heat map)
	 *            depending on the class of chart.
	 */
	public void setData(Iterable<? extends Vector> points) {
		if (axes == null) {
			setData2_all(points);
		} else
			// TODO filter data to fit axes
			if (this.series == null) {
				this.series = new Series();
			}
			
			this.series.setData((List<Vector>) Containers.getList(points));
//			for(Vector v : points) {
//				
//			}
//			throw new TodoException();
	}
	
	/**
	 * Take all data, set axes to fit, use Datum labels to {@link #setDataLabels(Map)}.
	 * 
	 * @param data2
	 */
	private void setData2_all(Iterable<? extends Vector> points) {
		assert axes == null;
		if (this.series == null) {
			this.series = new Series();
		}
		
		this.series.setData((List<Vector>) Containers.getList(points));

		// set data labels?
		Map<Integer,String> labels = new HashMap();
		int i=0;
		for (Vector vector : points) {
			if (vector instanceof Datum) {
				Object label = ((Datum) vector).getLabel();
				if (label!=null) {
					labels.put(i, Printer.toString(label));
				}
			}
			i++;
		}
		if ( ! labels.isEmpty() && new HashSet(labels.values()).size() > 1) {
			setDataLabels(labels);
		}
		
		// setup axes
		setData3_setupAxes();
	}

	/**
	 * Create axes (and ranges) if not already created.
	 * Bollox code -- require the use of this.series
	 */
	protected void setData3_setupAxes() {
		List<Vector> data = null;
		
		if (this.series != null) {
			data = this.series.getData();
		} else {
			// No data!
			return;
		}
		
		Pair<Vector> bounds = DataUtils.getBounds(data);
		int dim = bounds.first.size();
		if (axes == null) {
			axes = new Axis[2];
		}
		for (int i = 0; i < dim; i++) {
			if (axes[i] == null) {
				// create a fresh axis
				axes[i] = setData3_setupAxes2_newAxis(i);
			}
			double min = bounds.first.get(i);
			double max = bounds.second.get(i);
			setData3_setupAxes2_adjustRange(i, axes[i], min, max);
		}
	}

	/**
	 * @param i
	 * @param axs
	 * @param min
	 *            If this doesn't include zero, then it will be reduced to 0 for
	 *            the y-axis. If you don't want zero in the chart, use
	 *            {@link NumericalAxis#setRange(Range)}.
	 * @param max
	 *            For the Y axis, this will be rounded up to give a nicer axis
	 */
	protected void setData3_setupAxes2_adjustRange(int i, Axis axs, double min,
			double max) {
		if (!(axs instanceof NumericalAxis))
			return;
		// don't have the chart stop exactly at the min/max
		if (i == Y) {
			// zero origin?
			if (min > 0) {
				min = 0;
			} else {
				min = boostAxisRange(min, false);
			}
			max = boostAxisRange(max, true);
		}
		assert MathUtils.isFinite(min) && MathUtils.isFinite(max) : min + " "
				+ max;
		// set range (only if unset)
		NumericalAxis axis = (NumericalAxis) axs;
		if (axis.getRange() == null) {
			axis.setRange(new Range(min, max));
		}
	}

	/**
	 * Create a new axis. Override if you want a special axis.
	 * 
	 * @param i
	 * @return
	 */
	protected NumericalAxis setData3_setupAxes2_newAxis(int i) {
		NumericalAxis axis = new NumericalAxis();
		return axis;
	}

	@Deprecated // Use Series instead
	public Axis[] getAxes(){
		return axes;
	}
	
	public void setShowPoints(boolean showPoints) {
		this.showPoints = showPoints;
	}
	
	public void setType(ChartType type) {
		this.type = type;
	}

	public void setFlotOptions(Map<String, Object> _flot_options) {
		this.flot_options = _flot_options;
	}

	public void setTimeZone(TimeZone tz) {
		this.timeZone = tz;
		Axis[] axs = getAxes();
		if (axs == null) return;
		for (Axis ax : axs) {
			if (ax instanceof TimeAxis) {
				TimeAxis ta = (TimeAxis) ax;
				ta.setTimeZone(tz);
			}
		}
	}
	
	@Override
	protected JSONObject toJsonObject(JSONObject jsonObject) {
		jsonObject = super.toJsonObject(jsonObject);		
//		try {
		if (this.axes != null) {
			if (this.axes[0] != null) {
				jsonObject.put("xAxis", this.axes[0]);
			}
			
			if (this.axes[1] != null) {
				jsonObject.put("yAxis", this.axes[1]);
			}
		}
//		} catch (JSONException e) {
//			// This should not happen - we have ensured that all calls to JSONObject.put have
//			// a key that is not null.
//			jsonObject = new JSONObject();
//		}		
		return jsonObject;
	}
}
