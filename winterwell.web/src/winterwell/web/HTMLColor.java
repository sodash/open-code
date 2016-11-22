package winterwell.web;

import java.awt.Color;
import java.io.Serializable;

/**
 * Support for working with colours in html.
 * 
 * Very limited at present!
 * 
 * @testedby {@link HTMLColorTest}
 * @author daniel
 */
public class HTMLColor implements Serializable {
	private static final long serialVersionUID = 1L;

	private int blue;
	private int green;
	private int red;

	public HTMLColor(Color col) {
		this.red = col.getRed();
		this.green = col.getGreen();
		this.blue = col.getBlue();
	}

	/**
	 * An rgb() value suitable for use in css
	 */
	@Override
	public String toString() {
		return "rgb(" + red + "," + green + "," + blue + ")";
	}
}
