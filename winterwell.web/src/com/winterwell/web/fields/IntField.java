package com.winterwell.web.fields;

import java.math.BigInteger;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.web.WebInputException;

/**
 * Form field for an integer. Supports the special value: "max"
 * 
 * WARNING: Do you need a Long? See {@link LongField}.
 * 
 * @testedby IntFieldTest
 * @author Daniel
 */
public final class IntField extends AField<Integer> {

	private static final long serialVersionUID = 1L;

	private int max;
	private int min;

	private Boolean rounding;

	public IntField(String name) {
		this(name, "number");
	}

	public IntField(String name, String type) {
		super(name, type);
	}

	@Override
	public Integer fromString(String v) {
		// special case: "max"
		if ("max".equals(v))
			return max == 0 ? Integer.MAX_VALUE : max;
		if (v.endsWith(".0")) v = v.substring(0, v.length()-2);
		try {
			Integer i = Integer.valueOf(v);
			if (max != 0 && i > max)
				return max;
			return i;
		} catch (NumberFormatException e) {
			if (StrUtils.isInteger(v)) {
				BigInteger l = new BigInteger(v);
				throw new WebInputException(v+" is too big!");
			}
			// round a decimal?
			if (Utils.yes(rounding)) {
				try {
					double d = Double.valueOf(v);
					int i = (int) Math.round(d);
					if (max != 0 && i > max)
						return max;
					return i;
				} catch(Exception ex2) {
					// oh well, we tried
				}
			}
			throw new WebInputException(v
					+ " is not in the correct form (a number)");
		}
	}

	public int getMax() {
		return max;
	}

	@Override
	public Class<Integer> getValueClass() {
		return Integer.class;
	}

	/**
	 * @param max
	 *            If non-zero, specifies the maximum value this field can
	 *            return. Anything over the max will just be quietly capped.
	 * @return this
	 */
	public IntField setMax(int max) {
		this.max = max;
		return this;
	}

	public IntField setRounding(boolean b) {
		rounding = b;
		return this;
	}

}
