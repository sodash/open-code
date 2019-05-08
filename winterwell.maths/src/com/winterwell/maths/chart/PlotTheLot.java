//package com.winterwell.maths.chart;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import com.winterwell.maths.timeseries.DataUtils;
//import com.winterwell.maths.timeseries.IDataStream;
//import com.winterwell.maths.timeseries.ListDataStream;
//import com.winterwell.utils.containers.Pair;
//import com.winterwell.utils.web.WebUtils;
//import com.winterwell.web.HtmlTable;
//import com.winterwell.web.WebPage;
//
///**
// * Eyeball it -- Given a data-stream, plot every x_i x_j scatter plot, one colour per label
// * @testedby {@link PlotTheLotTest}
// * @author daniel
// *
// */
//public class PlotTheLot {
//
//	
//	public PlotTheLot(IDataStream data) {
//		this.data = data.list();
//	}
//	
//	ListDataStream data;
//	
//	private List<String> labels;
//
//	private int width = 150;
//	
//	public void run() throws IOException {
//		// labels
//		List<String> allLabels = DataUtils.getLabels(data);
//		Rainbow rainbow = new Rainbow(allLabels); // TODO use this (beware toString)
//		
//		RenderWithFlot render = new RenderWithFlot(width, width);
//		
//		Map<Pair<Integer>,File> xy_file = new HashMap();
//		WebPage page = new WebPage();
//		page.addScript("jquery");
//		page.appendToHeader(RenderWithFlot.DEPENDENCIES);
//		HtmlTable tbl;
//		if (data.getLabels()==null) {
//			tbl = new HtmlTable(data.getDim()+1);
//			for(int ax=0; ax<data.getDim(); ax++) {
//				tbl.addCell("<th>"+label(ax)+"</th>");
//			}
//		} else {
//			List<String> lbls = new ArrayList(data.getLabels());
//			lbls.add(0, " "); // label column
//			tbl = new HtmlTable(lbls);
//		}
//		for(int ax=0; ax<data.getDim(); ax++) {
//			tbl.addCell("<th>"+label(ax)+"</th>");
//			for(int ay=0; ay<data.getDim(); ay++) {				
//				Chart chart = plot(ax,ay);				
//				
//				File file = File.createTempFile("plot", ".png");
//				xy_file.put(new Pair<Integer>(ax, ay), file);
//				// todo parallel for speed??
//				String chtml = render.renderToHtml(chart); //File(chart, file);
////				render.renderToBrowser(chart);
//				tbl.addCell(chtml); //"<img src='"+file.toURI()+"' width='"+width+"' height='"+width+"'>");
//			}
//		}
//		tbl.appendHtmlTo(page);
//		WebUtils.display(page.toString());
//	}
//
//	private Chart plot(int ax, int ay) {		
//		IDataStream data_xy = DataUtils.getColumns(data, ax,ay);
//		CombinationChart plot = ScatterPlot.multiColor(data_xy);
//		String axn = label(ax);
//		String ayn = label(ay);
//		plot.getAxis(plot.X).setTitle(axn);
//		plot.getAxis(plot.Y).setTitle(ayn);
//		plot.setShowLegend(false);
//		return plot;
//	}
//
//	private String label(int ax) {
//		return data.getLabels()==null? ""+ax : data.getLabels().get(ax);
//	}
//	
//}
