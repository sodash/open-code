package com.winterwell.maths.chart;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;

import com.winterwell.maths.chart.Scaling.KScaling;
import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.maths.stats.distributions.MixtureModel;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Range;
import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.web.WebUtils;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;

/**
 * Extra rendering functions if we need them. Currently: matrix city grid
 * 
 * @author daniel
 * 
 */
public class RenderWithWinterwell extends ARender {

	private int[] convert(Vector xy, XYChart chart) {
		int[] ixy = new int[2];
		for (int i = 0; i < 2; i++) {
			double dx = xy.get(i);
			NumericalAxis axis = chart.getAxis(i);
			Range range = axis.getRange();
			dx = dx - range.low;
			dx = (dx * width) / range.size();
			ixy[i] = (int) Math.round(dx);
		}
		return ixy;
	}

	private Image render2_dist2d(Distribution2DChart chart) {
		// TODO this is a horrible custom hack
		IDistribution dist = chart.dist;
		assert dist instanceof MixtureModel;
		MixtureModel mm = (MixtureModel) dist;
		List<IDistribution> comps = mm.getComponents().getSortedObjects();

		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		g.setColor(chart.bgColor);
		g.fillRect(0, 0, width, height);

		Color[] cols = new Color[] { Color.RED, Color.GREEN, Color.BLUE,
				Color.PINK, Color.YELLOW, Color.CYAN, Color.GRAY, Color.ORANGE };

		for (int i = 0; i < 1000; i++) {
			Datum x = mm.sample();
			int ci = comps.indexOf(x.getLabel());
			Color col = cols[ci % cols.length];
			g.setColor(col);
			int[] ixy = convert(x, chart);
			g.fillRect(ixy[0], ixy[1], 2, 2);
		}

		g.dispose();
		return img;
	}

	private BufferedImage render2_matrix(MatrixChart chart) {
		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		g.setColor(chart.bgColor);
		g.fillRect(0, 0, width, height);
		Matrix matrix = chart.matrix;
		int cw = width / matrix.numRows();
		cw = Math.max(cw, 1);
		int ch = height / matrix.numRows();
		ch = Math.max(ch, 1);
		for (MatrixEntry me : matrix) {
			if (me.get() == 0) {
				continue;
			}
			int x = me.row() * cw;
			int y = me.column() * ch;
			// exponential fade
			double fade = 1 - Math.exp(-Math.abs(me.get()));
			Color col = GuiUtils.fade(fade, Color.WHITE,
					me.get() < 0 ? Color.RED : Color.BLUE);
			g.setColor(col);
			g.fillRect(x, y, cw, ch);
		}
		g.dispose();
		return img;
	}

	@Override
	public String renderToHtml(AChart achart) {
		if (achart instanceof MatrixChart)
			return renderToHtml2_matrixChart((MatrixChart) achart);
		throw new UnsupportedOperationException(achart.toString());
	}

	private String renderToHtml2_matrixChart(MatrixChart mchart) {
		Matrix matrix = mchart.matrix;
		StringBuilder html = new StringBuilder();
		if (mchart.title != null) {
			html.append("<h3>" + mchart.title + "</h3>");
		}
		String id = Utils.getRandomString(6);
		// TODO width, height
		html.append("<style>table#" + id
				+ " td {width:20px; height:20px;}</style>\n<table id='" + id
				+ "'>");
		boolean rLabels = mchart.rowLabels != null
				&& mchart.rowLabels.size() < 25;
		boolean cLabels = mchart.colLabels != null
				&& mchart.colLabels.size() < 25;
		if (rLabels) {
			html.append("\n<tr>");
			html.append("<td> </td>");
			for (int i = 0; i < mchart.rowLabels.size(); i++) {
				html.append("<td>" + (i + 1) + "</td>");
			}
			html.append("\n</tr>");
		}

		// Matrix
		// Use 1 as the lowest value (i.e. if all relations are weak - show
		// this)
		double max = 1;
		for (MatrixEntry me : matrix) {
			double v = me.get();
			if (!MathUtils.isFinite(v)) {
				continue;
			}
			max = Math.max(max, Math.abs(v));
		}
		for (int r = 0; r < matrix.numRows(); r++) {
			html.append("\n<tr>");
			if (cLabels) {
				html.append("<td>" + (r + 1) + "</td>");
			}
			for (int c = 0; c < matrix.numColumns(); c++) {
				// fade by cell-value
				double v = matrix.get(r, c);
				// scale by the largest value present
				double sv = v / max;
				// exponential or linear colour scaling?
				double fade;
				if (mchart.colorScaling.type == KScaling.EXPONENTIAL) {
					fade = 1 - Math.exp(-mchart.colorScaling.param
							* Math.abs(sv));
				} else if (mchart.colorScaling.type == KScaling.GAMMA) {
					fade = Math.pow(Math.abs(sv), mchart.colorScaling.param);
				} else {
					fade = Math.abs(sv);
				}
				Color col = GuiUtils.fade(fade, Color.WHITE, v < 0 ? Color.RED
						: Color.BLUE);
				// catch dud values and set to gray
				if (!MathUtils.isFinite(v)) {
					col = Color.GRAY;
				}
				String hcol = WebUtils.color2html(col);
				String tooltip = rLabels && cLabels ? v + " "
						+ mchart.rowLabels.get(r) + " / "
						+ mchart.colLabels.get(c) : "" + v;
				html.append("<td style='background:" + hcol + ";' title='"
						+ tooltip + "'>&nbsp;</td>");
			}
			html.append("</tr>");
		}
		html.append("\n</table>");

		// Labels
		if (rLabels && cLabels) {
			html.append("<table><tr><td>");
		}
		if (rLabels) {
			if (!mchart.rowLabels.equals(mchart.colLabels)) {
				html.append("<h4>Rows</h4>");
			}
			html.append("<ol>");
			for (int i = 0; i < mchart.rowLabels.size(); i++) {
				String lbl = mchart.rowLabels.get(i);
				html.append("<li>" + lbl + "</li>");
			}
			html.append("</ol>");
		}
		if (rLabels && cLabels) {
			html.append("</td><td>");
		}
		if (cLabels) {
			if (mchart.rowLabels.equals(mchart.colLabels)) {
				html.append(""); // no duplication
			} else {
				html.append("<h4>Columns</h4>");
				html.append("<ol>");
				for (int i = 0; i < mchart.colLabels.size(); i++) {
					String lbl = mchart.colLabels.get(i);
					html.append("<li>" + lbl + "</li>");
				}
				html.append("</ol>");
			}
		}
		if (rLabels && cLabels) {
			html.append("</td></tr></table>");
		}
		return html.toString();
	}

	@Override
	public Image renderToImage(Chart chart) {
		if (chart instanceof MatrixChart)
			return render2_matrix((MatrixChart) chart);
		if (chart instanceof Distribution2DChart)
			return render2_dist2d((Distribution2DChart) chart);
		throw new TodoException();
	}

}
