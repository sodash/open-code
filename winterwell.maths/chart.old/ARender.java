package com.winterwell.maths.chart;

import java.awt.Image;
import java.io.File;

import com.winterwell.utils.gui.GuiUtils;

/**
 * @deprecated Use {@link Renderer} instead.
 */
public abstract class ARender {

	protected int height = 400;
	protected int width = 600;

	public ARender() {
		super();
	}

	/**
	 * Handy for debugging
	 * 
	 * @param chart
	 */
	public void renderAndPopupAndBlock(Chart... charts) {
		Chart chart = charts.length == 1 ? charts[0] : new CombinationChart(
				charts);
		Image img = renderToImage(chart);
		assert img != null : chart;
		GuiUtils.popupAndBlock(img);
	}

	public void renderToFile(Chart chart, File file) {
		GuiUtils.saveImage(renderToImage(chart), file);
	}

	public String renderToHtml(AChart achart)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public abstract Image renderToImage(Chart chart);

	/**
	 * 500x400 by default
	 * 
	 * @param width
	 * @param height
	 */
	public final void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

}