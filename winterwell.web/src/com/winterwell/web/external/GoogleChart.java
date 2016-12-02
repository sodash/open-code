package com.winterwell.web.external;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.web.FakeBrowser;

/**
 * Use the Google chart API: http://code.google.com/apis/chart/
 * 
 * @author Daniel
 * 
 */
public class GoogleChart {

	public enum KChartType {
		/** horizontal bar chart, grouped */
		bhg,
		/**
		 * line chart for pairs of data series, first x then y lxy, /**
		 * horizontal bar chart
		 */
		bhs,
		/** vertical bar chart, grouped */
		bvg,
		/** vertical bar chart */
		bvs,
		/** meter (like a car speedometer) */
		gom,
		/** line chart */
		lc,
		/** line chart without axes */
		ls,
		/** pie chart */
		p,
		/** 3D pie chart */
		p3,
		/** concentric pie charts */
		pc,
		/** qr code */
		qr,
		/**
		 * radar chart r, /** radar chart with filled shapes
		 */
		rs,
		/** scatter plot */
		s,
		/** map */
		t,
		/** venn diagrams */
		v
	}

	private List<String> colors;
	private final List<double[]> data = new ArrayList<double[]>();
	private List<String> legend;
	private final Dimension size;
	private final KChartType type;

	public GoogleChart(KChartType type, double[] data, Dimension size) {
		this.type = type;
		this.data.add(data);
		this.size = size;
	}

	public void addColor(String color) {
		if (colors == null) {
			colors = new ArrayList<String>();
		}
		colors.add(color);
	}

	public void addDataSeries(double[] dataSeries) {
		data.add(dataSeries);
	}

	public void addLegendLabel(String label) {
		if (legend == null) {
			legend = new ArrayList<String>();
		}
		legend.add(label);
	}

	public void appendTo(StringBuilder page) {
		page.append("\n<img src='" + getUrl() + "'>\n");
	}

	/**
	 * Call Google and return the resulting image file.
	 */
	public File fetchImage() {
		FakeBrowser b = new FakeBrowser();
		b.setIgnoreBinaryFiles(false);
		return b.getFile(getUrl());
	}

	public String getUrl() {
		StringBuilder uri = new StringBuilder();
		uri.append("http://chart.apis.google.com/chart?");
		uri.append("cht=");
		uri.append(type);
		uri.append('&');
		uri.append("chs=");
		uri.append(size.width);
		uri.append('x');
		uri.append(size.height);
		uri.append('&');
		// data
		uri.append("chd=t:");
		for (double[] series : data) {
			for (double d : series) {
				uri.append(d);
				uri.append(',');
			}
			StrUtils.pop(uri, 1);
			uri.append('|');
		}
		StrUtils.pop(uri, 1);
		// colour
		if (colors != null) {
			uri.append("&chco=");
			Printer.append(uri, colors, ",");
		}
		// Legend
		if (legend != null) {
			uri.append("&chdl=");
			Printer.append(uri, legend, "|");
		}
		uri.append("&chxt=x,y"); // axes
		return uri.toString();
	}

}
