package com.winterwell.maths.chart;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.winterwell.json.JSONArray;
import com.winterwell.json.JSONObject;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.maths.vector.X;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;

import no.uib.cipr.matrix.Vector;

/**
 * Hold a distribution, and some
 * 
 * @author daniel
 * 
 */
public class PieChart<LabelType> extends AChart {

	/**
	 * Use with {@link #setLabeller(String)} to specify labels show actual
	 * values (not %s which is the default).
	 */
	public static final String LABEL_WITH_ABS_VALUES = "abs";
	private String labeller;
	private ArrayList<LabelType> labels;
	protected List<String> names;
	protected List<Color> colors;
	private Rainbow rainbow;

	private PieChart(String title) {
		this.type = ChartType.PIE;
		
		this.title = title;
	}
	
	/**
	 * 
	 * @param name Can be null
	 * @param dist
	 *            This will be normalised, and may be edited.
	 */
	public PieChart(String title, IFiniteDistribution<LabelType> dist) {
		this(title);
		
		setDist(dist);
	}
	
	
	/**
	 * Create a pie chart from lists of labels and values. Use this to preserve points
	 * whose value is zero.
	 * 
	 * @param title
	 * @param values
	 */
	public PieChart(String title, List<LabelType> labels, List<Double> values) {
		this(title);
		
		this.series = new Series();
		
		this.labels = new ArrayList<LabelType>();
		
		this.names = new ArrayList<String>();
		
		ArrayList<Vector> data = new ArrayList<Vector>();
		
		assert labels.size() == values.size();
		
		for (int i = 0; i < labels.size(); i++) {
			this.labels.add(labels.get(i));
			
			this.names.add(labels.get(i).toString());
			
			data.add(new X(values.get(i)));
		}
		
		this.series.setData(data);
	}
	
	/**
	 * 
	 * @param name
	 * @param chart A set of XY charts. The Y data will be summed to create the pie slice.
	 * The X data is ignored.
	 */
	public PieChart(String title, CombinationChart chart) {
		this(title);
		
		ObjectDistribution _dist = new ObjectDistribution();
		for(Chart c : chart.getCharts()) {
			// Skip ALL
			if (CombinationChart.ALL.equals(c.getTitle())) {
				continue;
			}
			double sum = 0;
			for(Vector v : c.getData()) {
				sum += v.get(1);
			}
			_dist.setProb(c.getTitle(), sum);
		}
		setDist(_dist);
	}
	
	public Color getColor(LabelType label) {
		if (rainbow == null) {
			setRainbow(new Rainbow(labels.size()));
		}
		// by key?
		if (rainbow.getKeys() != null) {
			try {
				return rainbow.getColor(label);
				
			} catch (IllegalArgumentException ex) { // HACK: Bug seen May 2012 from a TrafficReport. Hopefully a historical yeti
				// TODO delete if no re-occurences
				Log.e("piechart", ex);				
			}			
		}
		// by label index
		int i = labels.indexOf(label);
		if (i==-1) {
			Log.e("piechart", "Unrecognised label: "+label+" in "+labels);
			// Fallback: make a randomish colour
			int r= Math.abs(label.hashCode() % 255);
			int g= Math.abs(5*label.hashCode() % 255);
			int b= Math.abs(31*label.hashCode() % 255);
			return new Color(r,g,b);
		}
		return rainbow.getColor(i);
	}

	/**
	 * @return A list of 1-d vectors, in the same order as {@link #getLabels()}.
	 *         This is NOT normalised!
	 */
	@Override
	public List<Vector> getData() {
		if (this.series == null) {
			return null;
		}
		
		return this.series.getData();
	}
	
	/**
	 * Update the series for this pie chart. The data will *not* be changed.
	 */
	@Override
	public void setSeries(Series series) {
		if (series != null) {
			series.setData(this.getData());
		}
		
		super.setSeries(series);
	}
	
	/**
	 * @return null by default
	 */
	public String getLabeller() {
		return labeller;
	}

	/**
	 * @return the actual labels. So you could e.g. sort them.
	 */
	public ArrayList<LabelType> getLabels() {
		return labels;
	}

	public boolean getShowLegend() {
		if (this.legend == null) {
			return true; // The default for legend.isVisible().
		} else {
			return this.legend.isVisible();
		}
	}

	private void setDist(IFiniteDistribution<LabelType> dist) {
		this.names = new ArrayList<String>();
		
		this.labels = new ArrayList<LabelType>(dist.size());
		
		this.series = new Series();
		
		ArrayList<Vector> vectors = new ArrayList<Vector>();
		
		for (LabelType label : dist) {
			// Filter out the zero-prob ones? -- no could make the colouring
			// unpredictable.
			// But be aware that some distributions do this filtering
			// themselves.
			labels.add(label);
			
			this.names.add(label.toString());
			
			vectors.add(new X(dist.prob(label)));
		}
		
		this.series.setData(vectors);
	}

	/**
	 * Set slice labelling options.
	 * 
	 * @param labeller
	 * @see #LABEL_WITH_ABS_VALUES
	 */
	public void setLabeller(String labeller) {
		this.labeller = labeller;
	}

	public void setRainbow(Rainbow rainbow) {
		this.rainbow = rainbow;
	}
	
	@Override
	protected JSONObject toJsonObject(JSONObject jsonObject) {
		jsonObject = super.toJsonObject(jsonObject);		
//		try {
		if (this.series == null) {
			return jsonObject;
		}
		JSONObject seriesJsonObject = series.toJsonObject();
		
		if (series.data != null) {
			JSONArray jsonArray = new JSONArray();
			
			int index = 0;
			
			for (Vector point : series.data) {
				JSONObject pointJsonObject = new JSONObject();
				
				if (point.size() == 1) {
					pointJsonObject.put("y", point.get(0));
				} else if (point.size() > 1) {
					pointJsonObject.put("x", point.get(0));
					
					pointJsonObject.put("y", point.get(1));
				}
				
				if (this.names != null && this.names.size() > index) {
					pointJsonObject.put("name", names.get(index));
				}
				
				if (this.colors != null && this.colors.size() > index) {
					LabelType lbl = getLabels().get(index);
					pointJsonObject.put("color", WebUtils.color2html(getColor(lbl)));
				}
				
				jsonArray.put(pointJsonObject);
				
				index += 1;
			}
			
			seriesJsonObject.put("data", jsonArray);
		}
		
		jsonObject.put("series", new JSONArray().put(seriesJsonObject));		
//		} catch (JSONException e) {
//			// This should not happen - we have ensured that all calls to JSONObject.put have
//			// a key that is not null.
//			jsonObject = new JSONObject();
//		}
		
		return jsonObject;
	}
}