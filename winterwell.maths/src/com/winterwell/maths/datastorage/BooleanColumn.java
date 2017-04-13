package com.winterwell.maths.datastorage;

/**
 * A true/false valued attribute. I.e. maps between true/false and doubles.
 * 
 * @testedby {@link BooleanAttributeTest}
 * @author daniel
 */
public class BooleanColumn extends ColumnInfo<Boolean> {

	public BooleanColumn(String name) {
		super(name, Boolean.class);
	}

	/**
	 * Anything under 0.5 is interpreted as false, anything above as true.
	 */
	@Override
	public Boolean convertFromDouble(double x) {
		return x < 0.5 ? Boolean.FALSE : Boolean.TRUE;
	}

	@Override
	public double convertToDouble(Boolean x) {
		return x ? 1 : 0;
	}
}
