package com.winterwell.maths.chart;

import java.awt.Color;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.gui.GuiUtils;

/**
 * Pick colours that spread nicely across the spectrum.
 * 
 * See WebUtils#color2html(Color) for conversion to html.
 * 
 * @author daniel
 * @testedby  RainbowTest}
 */
public class Rainbow<X> {

	final Color[] cols;
	List<X> keys;

	/**
	 * color 0 = slightly pastel blue
	 * 
	 * @param n
	 */
	public Rainbow(int n) {
		// for two colours, using Rainbow(3) gives nicer colours!
		this(n == 2 ? 3 : n, new Color(32, 32, 192));
	}

	/**
	 * Create a rainbow using your own palette.
	 * @param cols
	 */
	public Rainbow(Color[] cols) {
		this.cols = cols;
	}
	
	/**
	 * Create a rainbow using your own palette.
	 */
	public Rainbow(Map<X,Color> key2colour) {
		this.cols = new Color[key2colour.size()];
		setKeys(key2colour.keySet());
		for (X key : keys) {
			Color col = key2colour.get(key);
			setColor(key, col);
		}
	}
	
	/**
	 * 
	 * @param n
	 *            Must be > 0
	 * @param start
	 */
	public Rainbow(int n, Color start) {
		assert n > 0;
		cols = new Color[n];
		cols[0] = start;
		int alpha = start.getAlpha();
		assert alpha != 0 : start;
		int r = start.getRed();
		int g = start.getGreen();
		int b = start.getBlue();
		assert r + g + b != 0 : start;
		assert r + g + b != 3 * 255 : start;
		// is hue in [0,1]?
		float[] hsb = new float[3];
		Color.RGBtoHSB(r, g, b, hsb);
		for (int i = 1; i < cols.length; i++) {
			float fi = i;
			float h2 = hsb[0] + fi / n;
			Color c = Color.getHSBColor(h2, hsb[1], hsb[2]);
			cols[i] = alpha == 255 ? c : new Color(c.getRed(), c.getGreen(),
					c.getBlue(), alpha);
		}
	}

	public Rainbow(Collection<X> tags) {
		this(tags.size());
		setKeys(tags);
	}
	
	/**
	 * Override the default colour to set your own for this key.
	 * @param key Must be in the Rainbow's keys.
	 * @param col
	 */
	public void setColor(X key, Color col) {
		int i = index(key);
		cols[i] = col;
	}

	public Color getColor(int i) {
		// loop round if we have to??
		i = i % cols.length;
		return cols[i];
	}

	/**
	 * Use with {@link #setKeys(List)} to get object-to-color mappings.
	 * 
	 * @param key
	 *            must be in the keys
	 * @return Color for key
	 */
	public Color getColor(X key) {
		int i = index(key);
		return getColor(i);
	}

	private int index(X key) {
		X ckey = toCanonical(key);
		int i = keys.indexOf(ckey);
		if (i == -1)
			throw new IllegalArgumentException("Unknown key: " + key + "="
					+ ckey + " (expected: " + keys + ")");
		return i;
	}

	/**
	 * @return keys or null if index-number-only
	 */
	public List<X> getKeys() {
		return keys;
	}

	/**
	 * Call this to use as a key-to-color map. This is often "safer" for
	 * consistent coloring than using as an index-to-color map.
	 * 
	 * @param keys Warning: if this is an unordered Set (eg HashSet), then
	 * the colour assignment will be unpredictable! It's best to use a List.
	 */
	public void setKeys(Collection<X> keys) {
		this.keys = Containers.getList(keys);
	}

	public int size() {
		return cols.length;
	}

	protected X toCanonical(X key) {
		return key;
	}

	/**
	 * Like {@link #getColor(Object)}, but if key is not recognised it will return a random colour rather than throw an exception.
	 * @param key
	 * @return
	 */
	public Color getColorSafe(X key) {
		try {
			return getColor(key);
		} catch(Exception ex) {
			return GuiUtils.getRandomColour(0.5);
		}
	}

}
