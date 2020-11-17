/**
 * 
 */
package com.winterwell.web.fields;

/**
 * A text-entry field whose values are interpreted as doubles.
 * 
 * @author daniel TODO javascript validater
 * @testedby  DoubleFieldTest}
 */
public class DoubleField extends AField<Double> {

	private static final long serialVersionUID = 1L;

	public DoubleField(String name) {
		super(name, "number");
	}
	
	@Override
	protected void addAttributes(StringBuilder page, Object... attributes) {
		super.addAttributes(page, attributes);
		// Set a pattern -- otherwise floats get blocked!!
		page.append("pattern='[0-9]+([\\,\\.][0-9]+)?[kKmM]?' "); 
	}

	public DoubleField(String name, boolean isSlider) {
		super(name, isSlider ? "range" : "number");
		// pass in min/max as attributes :(
	}

	/**
	 * Interprets 10,000, 10k (thousand), and 10m (million), also 10% = 0.1
	 */
	@Override
	public Double fromString(String v) {
		// TODO unify with MathUtils.getNumber(_num)
		// 10% 
		if (v.endsWith("%")) {
			String p = v.substring(0, v.length()-1);
			double prob = Double.valueOf(p) / 100;
			return prob;
		}
		// Support 10k and 10m as number specs
		v = v.toLowerCase();
		v = v.replace(",", "");
		v = v.replace("k", "000");
		v = v.replace("m", "000000");
		return Double.valueOf(v);
	}

	@Override
	public Class<Double> getValueClass() {
		return Double.class;
	}

}
