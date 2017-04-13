package com.winterwell.maths.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import com.winterwell.maths.timeseries.ADataStream;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.time.Time;

import no.uib.cipr.matrix.Vector;

/**
 * A sort of heat trace of vector readings over time. This class can also be
 * used to visualise other vector-based things (e.g. the innards of a
 * classifier). The {@link #getImage()} method provides a non-Swing way to use
 * this class.
 * 
 * TODO currently only works well with [0,1] valued data
 * 
 * @author Daniel
 * @testedby {@link TimeSeriesVectorTraceTest}
 */
public class TimeSeriesVectorTrace extends JComponent {
	private static final long serialVersionUID = 1L;

	// TODO ADataStream data;
	List<Datum> data;

	int dims = 10;

	int MAX_SECONDS = 1000;

	int pixelsPerDimension = 10;

	int pixelsPerSlice = 10;

	// TODO optional exponential colouring and +/i support (ie 0=grey)
	private Color getColor(Datum datum, int d) {
		double x = datum.get(d);
		x = 1 - Math.pow(0.5, x);
		int gray = (int) (255 * x);
		if (gray < 0) {
			gray = 0;
		}
		return new Color(gray, gray, gray);
	}

	/**
	 * Create an image showing the time series trace of the vectors.
	 * 
	 * @return
	 */
	public BufferedImage getImage() {
		Dimension ps = getPreferredSize();
		BufferedImage pic = new BufferedImage(ps.width, ps.height,
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = pic.getGraphics();
		paint(g);
		g.dispose();
		return pic;
	}

	/**
	 * Convenience for using {@link #getImage()} via
	 * {@link #setData(ADataStream)}.
	 * 
	 * @param pts
	 * @return
	 */
	public Image getImage(List<? extends Vector> pts) {
		List<Datum> dpts = new ArrayList<Datum>();
		for (Vector v : pts) {
			dpts.add(new Datum(new Time(), v, null));
		}
		IDataStream ptsData = new ListDataStream(dpts);
		setData(ptsData);
		return getImage();
	}

	@Override
	public Dimension getPreferredSize() {
		int w = data == null || data.size() < 10 ? 100 : pixelsPerSlice
				* data.size();
		return new Dimension(w, pixelsPerDimension * dims);
	}

	@Override
	public synchronized void paint(Graphics g) {
		int N = Math.min(MAX_SECONDS, data.size());
		if (dims * pixelsPerDimension > getHeight()) {
			setSize(getWidth(), dims * pixelsPerDimension);
		}
		if (data.size() * pixelsPerSlice > getWidth()) {
			setSize(data.size() * pixelsPerSlice, getHeight());
		}
		// int w = getWidth();
		// assert w > 10 : getWidth();
		// g.setColor(Color.BLUE);
		// g.drawRect(0, 0, getWidth(), getHeight());
		for (int s = 0; s < N; s++) {
			Datum datum = data.get(s).copy();
			dims = datum.getDim();
			for (int d = 0; d < dims; d++) {
				int x = s * pixelsPerSlice;
				int y = d * pixelsPerDimension;
				// TODO rainbow colours for class labels
				Color col = getColor(datum, d);
				// col = Color.green;
				g.setColor(col);
				g.fillRect(x, y, pixelsPerSlice, pixelsPerDimension);
			}
		}
	}

	public synchronized void setData(IDataStream data) {
		this.data = DataUtils.toList(data, 1000);
		dims = data.getDim();
	}

	public void setPixelsPerDim(int h) {
		assert h > 0;
		pixelsPerDimension = h;
	}

	public void setPixelsPerSlice(int w) {
		assert w > 0;
		pixelsPerSlice = w;
	}

}
