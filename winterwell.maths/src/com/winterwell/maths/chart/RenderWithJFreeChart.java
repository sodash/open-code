package com.winterwell.maths.chart;

import java.awt.Image;

import com.winterwell.utils.TodoException;

@Deprecated
public class RenderWithJFreeChart extends ARender {
	@Override
	public Image renderToImage(Chart chart) {
		throw new TodoException("uncomment the real code");
	}	
}

//
//import gnu.trove.TDoubleArrayList;
//
//import java.awt.BasicStroke;
//import java.awt.Color;
//import java.awt.Font;
//import java.awt.Image;
//import java.awt.image.BufferedImage;
//
//import no.uib.cipr.matrix.Vector;
//
//import org.jfree.chart.ChartFactory;
//import org.jfree.chart.ChartPanel;
//import org.jfree.chart.JFreeChart;
//import org.jfree.chart.axis.DateAxis;
//import org.jfree.chart.axis.NumberAxis;
//import org.jfree.chart.axis.NumberTickUnit;
//import org.jfree.chart.axis.SymbolAxis;
//import org.jfree.chart.axis.ValueAxis;
//import org.jfree.chart.labels.StandardXYToolTipGenerator;
//import org.jfree.chart.plot.PlotOrientation;
//import org.jfree.chart.plot.XYPlot;
//import org.jfree.chart.renderer.xy.StandardXYBarPainter;
//import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
//import org.jfree.chart.renderer.xy.XYBarRenderer;
//import org.jfree.chart.renderer.xy.XYItemRenderer;
//import org.jfree.chart.title.TextTitle;
//import org.jfree.chart.urls.StandardXYURLGenerator;
//import org.jfree.data.xy.DefaultIntervalXYDataset;
//import org.jfree.data.xy.DefaultXYDataset;
//import org.jfree.ui.RectangleInsets;
//
//import com.winterwell.maths.IGridInfo;
//import com.winterwell.utils.TodoException;
//import com.winterwell.utils.gui.GuiUtils;
//import com.winterwell.utils.time.Time;
//
///**
// * Status: deprecated as we don't like JFreeChart
// * 
// * @testedy {@link RenderWithJFreeChartTest}
// * @author daniel
// * 
// */
//public class RenderWithJFreeChart extends ARender {
//
//	private ValueAxis render3_getJFCAxis(NumericalAxis axis) {
//		if (axis instanceof TimeAxis) {
//			TimeAxis timeAxis = (TimeAxis) axis;
//			Time start = timeAxis.getStartTime();
//			Time end = timeAxis.getEndTime();
//			DateAxis xAxis = new DateAxis(timeAxis.getTitle());
//			xAxis.setTimeZone(timeAxis.timeZone);
//			return xAxis;
//		}
//		if (axis instanceof NominalAxis) {
//			SymbolAxis jfcAxis = new SymbolAxis(axis.getTitle(),
//					((NominalAxis) axis).categories.toArray(new String[0]));
//			jfcAxis.setMinorTickMarksVisible(false);
//			RectangleInsets ri = jfcAxis.getTickLabelInsets();
//			jfcAxis.setTickUnit(new NumberTickUnit(1.0));
//			// jfcAxis.setTickLabelsVisible(false);
//			jfcAxis.setRange(axis.getRange().low - 0.5,
//					axis.getRange().high - 0.5);
//			return jfcAxis;
//		}
//		// standard axis
//		NumberAxis jfcAxis = new NumberAxis(axis.getTitle());
//		jfcAxis.setRange(axis.getRange().low, axis.getRange().high);
//		return jfcAxis;
//	}
//
//	JFreeChart renderToChart(Chart chart) {
//		JFreeChart jfchart = renderToChart2(chart);
//		if (chart.getTitle() != null) {
//			jfchart.setTitle(chart.getTitle());
//		}
//		themeChart(chart, jfchart);
//		return jfchart;
//	}
//
//	JFreeChart renderToChart2(Chart chart) {
//		// Big ugly switch on class
//		if (chart instanceof XYChart) {
//			if (chart.type == ChartType.COLUMN)
//				return renderToChart2_barChart((XYChart) chart);
//			return renderToChart2_xyChart((XYChart) chart);
//		}
//		throw new TodoException();
//	}
//
//	/**
//	 * @param chart
//	 * @return
//	 */
//	private JFreeChart renderToChart2_barChart(XYChart chart) {
//		Axis xAxis = chart.axes[0];
//		Axis yAxis = chart.axes[1];
//		IGridInfo gi = chart.getAxis(0).getGrid();
//
//		// Awful hack to get labels positioned correctly
//		double offset = (xAxis instanceof NominalAxis) ? -0.5 : 0;
//
//		TDoubleArrayList ysl = new TDoubleArrayList();
//		TDoubleArrayList xslb = new TDoubleArrayList();
//		TDoubleArrayList xslm = new TDoubleArrayList();
//		TDoubleArrayList xslt = new TDoubleArrayList();
//		for (Vector d : chart.getData()) {
//			int bucketIndex = gi.getBucket(d.get(0));
//			xslb.add(gi.getBucketBottom(bucketIndex) + offset);
//			xslm.add(gi.getBucketMiddle(bucketIndex) + offset);
//			xslt.add(gi.getBucketTop(bucketIndex) + offset);
//			ysl.add(d.get(1));
//		}
//		double[] ys = ysl.toNativeArray();
//
//		// In principle this supports uneven grids
//		DefaultIntervalXYDataset dataset = new DefaultIntervalXYDataset();
//		dataset.addSeries("f",
//				new double[][] { xslm.toNativeArray(), xslb.toNativeArray(),
//						xslt.toNativeArray(), ys, ys, ys });
//
//		JFreeChart jfc = ChartFactory.createHistogram(chart.getTitle(),
//				xAxis.getTitle(), yAxis.getTitle(), dataset, PlotOrientation.VERTICAL,
//				chart.getShowLegend(), true, true);
//
//		ValueAxis xAxisJFC = render3_getJFCAxis(chart.getAxis(0));
//		ValueAxis yAxisJFC = render3_getJFCAxis(chart.getAxis(1));
//
//		jfc.getXYPlot().setDomainAxis(xAxisJFC);
//		jfc.getXYPlot().setRangeAxis(yAxisJFC);
//
//		// Turn off hideous 3d effect
//		// TODO: Improve delineation of bars
//		StandardXYBarPainter barPainter = new StandardXYBarPainter();
//		XYBarRenderer renderer = (XYBarRenderer) jfc.getXYPlot().getRenderer();
//		renderer.setDrawBarOutline(true);
//		renderer.setBarPainter(barPainter);
//		renderer.setSeriesOutlineStroke(0, new BasicStroke());
//		renderer.setSeriesOutlinePaint(0, Color.DARK_GRAY);
//		return jfc;
//	}
//
//	private JFreeChart renderToChart2_xyChart(XYChart chart) {
//		DefaultXYDataset dataset = new DefaultXYDataset();
//		Axis timeAxis = chart.axes[0];
//		Axis yAxis = chart.axes[1];
//
//		TDoubleArrayList xs = new TDoubleArrayList(); // time values
//		TDoubleArrayList ys = new TDoubleArrayList(); // data values
//
//		for (Vector d : chart.getData()) {
//			xs.add(d.get(0));
//			ys.add(d.get(1));
//		}
//		double[][] data = new double[][] { xs.toNativeArray(),
//				ys.toNativeArray() };
//		dataset.addSeries("f", data);
//		// Line based?
//		if (chart.type == ChartType.LINE) {
//			JFreeChart jfc = ChartFactory.createXYLineChart(chart.title,
//					timeAxis.getTitle(), yAxis.getTitle(), dataset,
//					PlotOrientation.VERTICAL, true, true, true);
//			return jfc;
//		}
//
//		// Point chart
//		ValueAxis xAxis = render3_getJFCAxis(chart.getAxis(0));
//		ValueAxis yAxisJFC = render3_getJFCAxis(chart.getAxis(1));
//		StandardXYItemRenderer renderer = new StandardXYItemRenderer(
//				StandardXYItemRenderer.SHAPES);
//		renderer.setBaseShapesFilled(false);
//		XYPlot plot = new XYPlot(dataset, xAxis, yAxisJFC, renderer);
//		plot.setOrientation(PlotOrientation.VERTICAL);
//		renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
//		renderer.setURLGenerator(new StandardXYURLGenerator());
//
//		JFreeChart jfc = new JFreeChart(chart.title,
//				JFreeChart.DEFAULT_TITLE_FONT, plot, true);
//		return jfc;
//	}
//
//	@Override
//	public Image renderToImage(Chart chart) {
//		JFreeChart jfc = renderToChart(chart);
//		ChartPanel panel = new ChartPanel(jfc);
//		panel.setSize(width, height);
//		panel.validate();
//		BufferedImage img = GuiUtils.getImage(panel);
//		// TODO: Add watermark
//		return img;
//	}
//
//	/**
//	 * Apply Winterwell specific styling JFC has its own themeing mechanism, but
//	 * this seemed simpler for the time being
//	 * 
//	 * @param chart
//	 */
//	void themeChart(Chart wchart, JFreeChart chart) {
//		chart.setAntiAlias(true);
//		TextTitle title = chart.getTitle();
//		if (title != null) {
//			chart.getTitle().setFont(new Font("Helvetica", Font.PLAIN, 14));
//		}
//		// This works for all current chart types. Might have to do a
//		// case switch here in future.
//		XYItemRenderer renderer = chart.getXYPlot().getRenderer();
//		// TODO: Use the rest of the graphing palette
//		renderer.setSeriesPaint(0, new Color(0x55, 0xd4, 0x00));
//		// renderer.setSeriesPaint(0, GuiUtils.WINTERWELL_PINK);
//		renderer.setSeriesOutlineStroke(0, new BasicStroke());
//		renderer.setSeriesOutlinePaint(0, Color.DARK_GRAY);
//
//		// chart specific colours
//		if (wchart.bgColor != null) {
//			chart.setBackgroundPaint(wchart.bgColor);
//		}
//	}
//}
