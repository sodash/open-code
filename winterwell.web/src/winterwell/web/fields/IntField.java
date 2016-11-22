package winterwell.web.fields;

import java.math.BigInteger;

import winterwell.utils.StrUtils;
import winterwell.web.WebInputException;

/**
 * Form field for an integer. Supports the special value: "max"
 * 
 * @author Daniel
 */
public final class IntField extends AField<Integer> {

	private static final long serialVersionUID = 1L;

	private int max;

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

}
