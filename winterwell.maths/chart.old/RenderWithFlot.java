package com.winterwell.maths.chart;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Range;
import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.utils.web.WebUtils;

import no.uib.cipr.matrix.Vector;

/**
 * Produce html graphs using the open-source Flot library.
 * 
 * Javascript dependencies: jquery.js jquery.flot.js RenderWithFlotWidget.js
 * (must be added separately)
 * 
 * @author daniel
 * @testedby  RenderWithFlotTest}
 * 
 * @deprecated Use subclasses of {@link Renderer} instead. Probably {@link HighchartsRenderer}
 */
//@Resource(name = "RenderWithFlotWidget.js", type = File.class)
public class RenderWithFlot extends ARender {

	/**
	 * For use within SoDash
	 * @deprecated It's better to link directly to the dependencies. This is a
	 *             hack for convenient testing. Relies on soda.sh to supply the
	 *             files.
	 * 
	 *             Does NOT include JQuery. Does include RenderWithFlotWidget.js
	 */	
	public static final String RWF_DEPENDENCIES = 
		"<!--[if lt IE 9]><script language='javascript' type='text/javascript' src='/static/code/excanvas.min.js'></script><![endif]-->\n" +
		"<script type='text/javascript' language='javascript' src='/static/code/flot-all.min.js'></script>\n" +
		"<script type='text/javascript' language='javascript' src='/static/code/widgets/Charts.min.js'></script>\n" +
		"<script type='text/javascript' language='javascript' src='/static/code/widgets/RenderWithFlotWidget.min.js'></script>";
			
	/**
	 * For use outside SoDash
	 */
	@Deprecated
	public static final String DEPENDENCIES = "<!--[if lt IE 9]><script src='http://www.soda.sh/static/code/excanvas.min.js' language='javascript' type='text/javascript'></script><![endif]-->\n"
			+ "<script src='http://www.soda.sh/static/code/widgets/Charts.js' language='javascript' type='text/javascript'></script>\n"
			+ "<script src='http://www.soda.sh/static/code/jquery.flot.min.js' language='javascript' type='text/javascript'></script>\n"
			+ "<script src='http://www.soda.sh/static/code/jquery.flot.pie.js'></script>\n"
			+ "<script src='http://www.soda.sh/static/code/base/underscore-min.js'></script>\n"
			+ "<script src='http://local.soda.sh/static/code/flot/jquery.flot.toggle.js'></script>\n"
			+ "<script src='http://local.soda.sh/static/code/flot/jquery.flot.tooltips.js'></script>\n"
			+ "<script src='http://www.soda.sh/static/code/base/Utils.js'></script>\n"			
			+ "<script>"
			+ FileUtils.read(RenderWithFlot.class
					.getResourceAsStream("RenderWithFlotWidget.js"))
			+ "</script>\n";

	public static final String JQUERY_DEPENDENCY = "<script type='text/javascript' src='http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js' ></script>";

	/**
	 * TODO save to png via http://www.nihilogic.dk/labs/canvas2image/
	 */
	public static final String PNG = "<script>"
			+ "var oImgPNG = Canvas2Image.saveAsPNG(oCanvas, true);</script>";

	static SimpleJson sj = new SimpleJson();
//	private String clickFn;

	private String cssclass;
	
	private String elementId;
	
	private String masterChartId;

	private boolean interactive;

	// max ticks for nominal axes (for now applied to all nominal axes)
	// default 0, meaning don't impose a max
	private int max_nominal_ticks;

	private String tooltipFnName;
	
	public void setTooltipFnName(String tooltipFnName) {
		this.tooltipFnName = tooltipFnName;
	}

//	private String hoverFn;

	public RenderWithFlot() {
		max_nominal_ticks = 0; // default -- 0 means don't impose a max
	}

	public RenderWithFlot(int width, int height) {
		setSize(width, height);
	}

	/**
	 * A map for converting to JSON, which has two values:<br>
	 * - data: a list of the data for individual Flot chart lines<br>
	 * - options: a set of Flot chart drawing options, such as axes
	 * 
	 * @param achart
	 *            Can be a {@link CombinationChart} or a single chart
	 * @return
	 * @testedby  RenderWithFlotTest#testGetJSONPayload()}
	 */
	public Map<String, Object> getJSONPayload(AChart achart) {
		List<AChart> charts;
		if (achart instanceof CombinationChart) {
			charts = (List) ((CombinationChart) achart).charts;
		} else {
			charts = Collections.singletonList(achart);
		}
		List<Object> data = getJSONPayload2_data(charts);
		Map<String, Object> options = getJSONPayload2_chartOptions(achart, data.size());
		return new ArrayMap<String, Object>("data", data, "options", options);
	}

	/**
	 * @param achart
	 * @return This will become the flot options object, e.g. this.options =
	 *         {"xaxis":{"min":1.0, "max":2.0}, "yaxis":{"min":0.0, "max":3.0},
	 *         "legend":{"show":true}}
	 */
	private Map<String, Object> getJSONPayload2_chartOptions(AChart achart, int dataCount) {
		Map<String, Object> options = new ArrayMap<String, Object>();
		
		if (achart instanceof Chart) {
			Chart chart = (Chart) achart;
			// X axis
			getJSONPayload3_chartOptions2_xAxis((Chart) achart, options);

			// Y axis
			Axis yAxis = chart.getAxis(AChart.Y);
			ArrayMap<String, Object> yam = new ArrayMap<String, Object>();
			if (yAxis instanceof NumericalAxis) {
				Range range = ((NumericalAxis) yAxis).getRange();
				if (range != null) {
					yam.put("min", range.low);
					//yam.put("max", range.high);
				}
				Object ticks = ((NumericalAxis) yAxis).getTicks();
				if (ticks != null) {
					yam.put("ticks", ticks);
				}
			}
			options.put("yaxis", yam);
		
			
	
			// FIXME interactive?
			if (interactive) {
				options.put("grid", new ArrayMap("clickable", true, "hoverable", true));
				
				if (chart.getDataLabels() != null && ! chart.getDataLabels().isEmpty()) {
					options.put("dataLabels", chart.getDataLabels());	
				}
			}
		}
		
		// legend
		options.put("legend", new ArrayMap("show", achart.getShowLegend()));
			
		// Normal charts (line & bar)
		options.put("toggle", new ArrayMap("enabled", true, "hiddenOpacity", 0.5));
		
		ArrayMap tooltipOptions = new ArrayMap();
		
		tooltipOptions.put("enabled", true);
		
		// Do we have a custom tooltip?
		if (tooltipFnName!=null) tooltipOptions.put("message", tooltipFnName);
		
		options.put("tooltips", tooltipOptions);
		
		// TODO Set grid as clickable, but only if it is actually needed!
		//options.put("grid", new ArrayMap("clickable", true, "autoHighlight", false));
		
		return options;
	}

	/**
	 * 
	 * @param charts
	 * @return each element is for 1 chart. Elements are Maps for line charts,
	 *         Lists for PieCharts
	 */
	private List<Object> getJSONPayload2_data(List<AChart> charts) {
		assert charts != null && !charts.isEmpty() : charts;
		// int i = 0;
		List data = new ArrayList(charts.size());
		for (AChart chart : charts) {
			assert chart != null;
			if (chart instanceof PieChart) {
				PieChart pieChart = (PieChart) chart;
				//ArrayList<ArrayMap<String, Object>> vals = getJSONPayload3_data2_pie(pieChart);
				//data.add(vals);
				data = getJSONPayload3_data2_pie(pieChart);
			} else {
				Map<String, Object> map = getJSONPayload3_data2((Chart) chart);
				data.add(map);
			}
		}

		return data;
	}

	private void getJSONPayload3_chartOptions2_xAxis(Chart chart,
			Map<String, Object> options) {
		// X axis
		Axis xAxis = chart.getAxis(AChart.X);
		if (xAxis == null)
			return;
		ArrayMap xam = new ArrayMap();
		if (xAxis instanceof TimeAxis) {
			xam.put("mode", "time");
		}
		if (xAxis instanceof NumericalAxis) {
			Range range = ((NumericalAxis) xAxis).getRange();
			if (range != null) {
				xam.put("min", range.low);
				xam.put("max", range.high);
			}
		}
		// histogram labels?
		if (xAxis instanceof NominalAxis) {
			NominalAxis nominalAxis = (NominalAxis) xAxis;
			List<String> labels = ((NominalAxis) xAxis).categories;
			int tick_skip = 1;
			if (max_nominal_ticks != 0 && labels.size() > max_nominal_ticks) {
				// thin out the labels a bit
				tick_skip = (int) Math.max(2,
						Math.floor(labels.size() / max_nominal_ticks));
			}
			ArrayList ticks = new ArrayList();
			for (int i = 0; i < nominalAxis.categories.size(); i++) {
				if (i % tick_skip != 0) {
					continue;
				}
				String labelI = nominalAxis.categories.get(i);
				labelI = StrUtils.ellipsize(labelI, 30);
				ticks.add(Arrays.asList(i + 0.5, labelI));
			}
			xam.put("ticks", ticks);
		}
		options.put("xaxis", xam);
	}

	/**
	 * A JSON map which carries the data for a single line in a Flot chart.
	 * 
	 * @param chart
	 * @param html
	 */
	private Map<String, Object> getJSONPayload3_data2(Chart chart) {
		Map chartData = new ArrayMap();
		// lines / points / bars / pie
		if (chart.type == ChartType.SCATTER || chart.getShowPoints() == true) {
			chartData.put("points", new ArrayMap("show", true));
		}

		if (chart.type == ChartType.COLUMN) {
			chartData.put("bars", new ArrayMap("show", true));
		}

		if (chart.type == ChartType.LINE) {
			chartData.put("lines", new ArrayMap("show", true));
		}

		// color?
		if (chart.getColor() != null) {
			chartData.put("color", WebUtils.color2html(chart.getColor()));
		}

		// The Data!
		List<Vector> data = chart.getData();
		assert data != null && !data.isEmpty() : chart;

		ArrayList<double[]> vals = new ArrayList<double[]>();

		chartData.put("data", vals);		
		chartData.put("label", chart.getTitle());

		for (Vector v : data) {
			double x = v.get(0);
			double y = v.get(1);
			vals.add(new double[] { x, y });
		}
		// TODO how can we include on-hover & on-click data?

		return chartData;
	}

	private ArrayList<ArrayMap<String, Object>> getJSONPayload3_data2_pie(
			PieChart chart) {
		ArrayList<ArrayMap<String, Object>> vals = new ArrayList<ArrayMap<String, Object>>();
		// a list of 1-d vectors
		List<Vector> pieData = chart.getData();
		int j = 0;
		// normaliser
		double total = 0;
		for (Vector v : pieData) {
			total += v.get(0);
		}
		// data
		for (Vector v : pieData) {
			ArrayMap<String, Object> val = new ArrayMap<String, Object>();
			Object lbl = chart.getLabels().get(j);
			val.put("label", lbl);
			
			// Stupid structure, but flot.pie fails without it.
			ArrayList innerData = new ArrayList();
			innerData.add(j);
			innerData.add(v.get(0));
			
			ArrayList data = new ArrayList();
			data.add(innerData);
			
			val.put("data", data); // / total
			Color c = chart.getColor(lbl);
			val.put("color", WebUtils.color2html(c));
			// count -- used for abs-labelling
			// NB: this relies on the PieChart's distribution not having been
			// normalised
			//val.put("count", v.get(0));
			vals.add(val);
			j++;
		}
		return vals;
	}


	/**
	 * @param achart
	 * @return the data cargo for an ajax chart
	 * @see #renderHtmlSkeleton(AChart)
	 */
	public String renderJsonCargo(AChart achart) {
		throw new TodoException();
		// String json = sj.toJson(cargo);
		// return json;
	}

	public void renderToBrowser(AChart chart) {
		// don't try if headless (this is a convenience for headless unit tests)
		if (!GuiUtils.isInteractive())
			return;

		String html = renderToHtml(chart);
		html = "<html><head><title>" + chart.getTitle() + "</title>"
				+ JQUERY_DEPENDENCY + "\n" + DEPENDENCIES + "</head><body>"				
				+ html + "</body></html>";
		WebUtils.display(html);
	}

	@Override
	public void renderToFile(Chart chart, File file) {
		assert file.getAbsoluteFile().getParentFile().isDirectory() : file;
		String html = renderToHtml(chart);

		InputStream strm = RenderWithFlot.class
				.getResourceAsStream("RenderWithFlotWidget.js");
		String js = FileUtils.read(strm);

		html = "<html><head><title>" + chart.getTitle() + "</title>"
				+ JQUERY_DEPENDENCY + DEPENDENCIES + "</head><body><script>"
				+ js + "</script>" + html + "</body></html>";
		WebUtils.renderToPng(html, file);
		// done
	}

	/**
	 * This does NOT add dependencies.
	 * @warning these divs float, which can cause layout issues.
	 */
	@Override
	public String renderToHtml(AChart achart) {
		// Use Winterwell if this type is alien to Flot
		if (achart instanceof MatrixChart)
			return new RenderWithWinterwell().renderToHtml(achart);

		List<AChart> charts;
		if (achart instanceof CombinationChart) {
			charts = (List) ((CombinationChart) achart).charts;
			assert !charts.isEmpty() : achart;
			// TODO axes
		} else {
			charts = Collections.singletonList(achart);
		}

		// the container div
		StringBuilder html = new StringBuilder();

		this.elementId = "flot-plot" + Utils.getRandomString(4);

		html.append("<div");
		
		if (cssclass != null) {
			html.append(" class=\"" + cssclass + "\"");
		}
		
		html.append(">");

		if (achart.getTitle() != null) {
			html.append("<h3>" + achart.getTitle() + "</h3>");
		}

		/*html.append("<div id='" + id + "' style='width:" + width + "px;height:"
				+ height + "px;' class='renderWithFlot"
				+ ((achart instanceof PieChart) ? " pie" : "") + (cssclass==null? "" : " "+cssclass)
				+ "'>\n");*/
		
		html.append("<div id='" + this.elementId
				+ "' style='width:" + width + "px;height:" + height + "px;'"
				+ " class='renderWithFlot"
				+ ((achart instanceof PieChart) ? " pie" : "")
				+ (cssclass == null ? "" : " " + cssclass)
				+ "'"); // End of class attribute.
		
		// Data
		StringBuilder chartData = new StringBuilder();
		
		List<Object> data = getJSONPayload2_data(charts);
		
		Iterator iter = data.iterator();
		
		while (iter.hasNext()) {
			sj.appendJson(chartData, iter.next());
			if (iter.hasNext()) {
				chartData.append(",");
			}
		}
		
		html.append(" data-chart-data='[" + chartData.toString() + "]'");
		
		// Options
		StringBuilder chartOptions = new StringBuilder(); 
		Map<String, Object> options = getJSONPayload2_chartOptions(achart, data.size());

		sj.appendJson(chartOptions, options);
		
		html.append(" data-chart-options='" + chartOptions + "'");
		
		if (this.masterChartId != null) {
			html.append(" data-chart-master-id='" + this.masterChartId + "'");
		}
		
		// Close html element.
		html.append(">");
		
		/*if (achart instanceof AChart) {
			html.append("var customOptions = ");
			sj.appendJson(html, ((AChart) achart).getFlotOptions());
			html.append(";\n");
		}*/

//		// clickFn TODO have a standard version defined in the .js
//		if (clickFn != null) {
//			html.append("this.clickFn = " + clickFn + ";\n");
//		}
//		// hoverFn
//		if (hoverFn != null) {
//			html.append("this.hoverFn = " + hoverFn + ";\n");
//		}

		// Label sorting
		/*
		 * if(achart instanceof PieChart) { html.append(
		 * "d[0] = d[0].sort(function(a, b) { return a.label < b.label; });\n");
		 * } else {
		 * html.append("d = d.sort(function(a, b) { return a.label < b.label; });\n"
		 * ); }
		 */
		
		html.append("</div></div>");

		return html.toString();
	}

	@Override
	public Image renderToImage(Chart chart) {
		try {
			File file = File.createTempFile("chart", ".png");
			renderToFile(chart, file);
			BufferedImage img = GuiUtils.load(file);
			FileUtils.delete(file);
			return img;
		} catch (IOException e) {
			throw Utils.runtime(e);
		}
	}

	public void setClass(String cssclass) {
		this.cssclass = cssclass;
	}
	
	public void setMasterChartId(String masterChartId) {
		this.masterChartId = masterChartId;
	}
	
	public String getElementId() {
		return this.elementId;
	}
	/**
	 * TODO 
	 */
	public void setInteractive(boolean interactive) {
		this.interactive = interactive;
	}

	public void setMaxNominalTicks(int max_ticks) {
		this.max_nominal_ticks = max_ticks;
	}

}
