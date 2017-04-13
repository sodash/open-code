package com.winterwell.maths.chart;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.winterwell.maths.vector.XY;
import com.winterwell.utils.gui.GuiUtils;

import no.uib.cipr.matrix.Vector;

/**
 * @tests {@link XYChart}
 * @author daniel
 * 
 */
public class XYChartTest {

	@Test
	public void testSetJitter() {
		{
			XYChart chart = new XYChart();
			List<Vector> data = new ArrayList<Vector>();
			for (int i = 0; i < 100; i++) {
				data.add(new XY(1, 1));
				data.add(new XY(0, 0));
			}
			chart.setData(data);
			chart.setJitter(0.1);
			RenderWithJFreeChart render = new RenderWithJFreeChart();
			Image img = render.renderToImage(chart);
			
				GuiUtils.popupAndBlock(img);
			
		}
		{ // should look the same inspite of the scale change
			XYChart chart = new XYChart();
			List<Vector> data = new ArrayList<Vector>();
			for (int i = 0; i < 100; i++) {
				data.add(new XY(100, 100));
				data.add(new XY(0, 0));
			}
			chart.setData(data);
			chart.setJitter(0.1);
			RenderWithJFreeChart render = new RenderWithJFreeChart();
			Image img = render.renderToImage(chart);
			
				GuiUtils.popupAndBlock(img);
			
		}
		{
			XYChart chart = new XYChart();
			List<Vector> data = new ArrayList<Vector>();
			for (int i = 0; i < 100; i++) {
				data.add(new XY(1, 1));
				data.add(new XY(0, 0));
			}
			// reverse the call order
			chart.setJitter(0.1);
			chart.setData(data);
			RenderWithJFreeChart render = new RenderWithJFreeChart();
			Image img = render.renderToImage(chart);
			
				GuiUtils.popupAndBlock(img);
			
		}
	}
	
	@Test
	public void testLinePlot() {
		List<Vector> data = new ArrayList<Vector>(100);
		for (int i = 0; i < 100; i++) {
			double x = i * 0.1;
			Vector v = new XY(x, Math.sin(x));
			data.add(v);
		}
		
		XYChart chart = new XYChart();
		chart.setData(data);
		chart.setType(ChartType.LINE);
		RenderWithJFreeChart render = new RenderWithJFreeChart();
		Image img = render.renderToImage(chart);
		
			GuiUtils.popupAndBlock(img);
		
	}

}
