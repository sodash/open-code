package com.winterwell.maths.chart;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.winterwell.maths.vector.XY;

import no.uib.cipr.matrix.Vector;

public class HighchartsRenderToFileDemo {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		XY point;
		List<Vector> data = new ArrayList<Vector>();
		Series series = new Series();
		Chart chart = new Chart();
		Random random = new Random();
		
		for (int i = 0; i < 10; i++) {
			point = new XY(i, random.nextDouble());
			
			data.add(point);
		}
		
		series.setData(data);
		
		chart.setSeries(series);
		HighchartsRenderer renderer = new HighchartsRenderer();
		File file = new File("/tmp/HighchartsRenderToFileDemo.png");
		
		renderer.renderToFile(chart, file, 2.0);
		Desktop.getDesktop().open(file);
	}
}
