package com.winterwell.maths.chart;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.winterwell.utils.ShellScript;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.web.WebPage;

/**
 * Highcharts based renderer.
 * WARNING: Highcharts requires a license!
 * 
 * @author Steven
 *
 */
public class HighchartsRenderer extends Renderer {
	private final List<String> dependencies;
	
	public HighchartsRenderer() {
		this.dependencies = new ArrayList<String>();
//		this.dependencies.add("jquery.js");
		this.dependencies.add("highcharts.js");
		this.dependencies.add("HighchartsRenderer.js");
	}
	
	@Override
	public String renderToHtmlPage(AChart chart) {
		WebPage writer = new WebPage();
		BufferedReader reader = null;		
		try {	
			writer.addScript("jquery");
			writer.append("<script>");
			for (String dependency : dependencies) {
				InputStream res = getClass().getResourceAsStream(dependency);
				assert res!=null : dependency;
				reader = FileUtils.getReader(res);
				
				String currentLine = "";				
				while (currentLine != null) {
					writer.append(currentLine + "\n");					
					currentLine = reader.readLine();
				}
			}
			writer.append("</script>");
			
			// I'd rather not have to perform this cast, but it doesn't
			// behave correctly without. 
			if (chart instanceof LinkedChart) {
				writer.append(renderToHtml((LinkedChart)chart));
			} else {
				writer.append(renderToHtml(chart));
			}			
			return writer.toString();
		} catch (IOException e) {
			throw Utils.runtime(e);
		} finally {
			FileUtils.close(reader);
		}
	}
	
	@Override
	public Image renderToImage(AChart chart, ImageFormat format) {
		// TODO Auto-generated method stub
		throw new TodoException();
	}
	
	@Override
	public void renderToFile(AChart chart, File file) {
		String command = "phantomjs highcharts-convert.js -outfile " + file.getAbsolutePath() + " -constr Chart";
		
		this.renderToFile(chart, file, command);
	}
	
	public void renderToFile(AChart chart, File file, double scale) {
		String command = "phantomjs highcharts-convert.js -outfile " + file.getAbsolutePath() + " -constr Chart -scale " + scale;
		
		this.renderToFile(chart, file, command);
	}
	
	public void renderToFile(AChart chart, File file, int width) {
		String command = "phantomjs highcharts-convert.js -outfile " + file.getAbsolutePath() + " -constr Chart -width " + width;
		
		this.renderToFile(chart, file, command);
	}
	
	private void renderToFile(AChart chart, File file, String command) {
		File chartData = FileUtils.createTempFile("HighchartsRenderToFile", ".json");
		
		StringBuilder writer = new StringBuilder();
		
		writer.append(chart.toJSONString());
		
		FileUtils.write(chartData, writer.toString());
		
		command += " -infile " + chartData.getAbsolutePath();
		
		ShellScript.run(command);
	}
}
