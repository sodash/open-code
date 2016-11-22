package com.winterwell.datalog;

import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.RateCounter;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.web.SimpleJson;

/**
 * A tagged rate, e.g. "Lines of Awesome Code: 10 per hour"<br>
 * This is what gets returned from a Stat query.
 * 
 * @author daniel

 * @testedby RateTest
 */
public final class Rate 
//extends ANumber 
implements Comparable<Rate>, IHasJson {
	private static final long serialVersionUID = 1L;

	/**
	 * Use this for "this rate should be auto-set and adjusted by the system"
	 */
	public static final double AUTO = -101;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dt == null) ? 0 : dt.hashCode());
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Rate other = (Rate) obj;
		if (dt == null) {
			if (other.dt != null)
				return false;
		} else if (!dt.equals(other.dt))
			return false;
		if (tag == null) {
			if (other.tag != null)
				return false;
		} else if (!tag.equals(other.tag))
			return false;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		return true;
	}

	/**
	 * @param tag
	 * @return zero, but it has the tag field
	 */
	public static final Rate ZERO(String tag) {
		return new Rate(0, TUnit.MILLISECOND.dt, tag);
	}

	/**
	 * 
	 * @param x
	 * @param dt
	 * @param tag Can be null
	 */
	public Rate(double x, Dt dt, String tag) {
		this.x = x;
		this.dt = dt;
		assert dt != null : this;
		this.tag = tag;
	}
	
	public Rate(double x, Dt dt) {
		this(x,dt,null);
	}
	public Rate(double x, TUnit dt) {
		this(x,dt.dt,null);
	}

	public Rate(RateCounter rate) {
		this(rate.get(), rate.getPeriod(), null);
	}

	/**
	 * Convert the rate to a different unit. E.g. 1/hour = 24/day
	 * 
	 * @param rate
	 * @param unit
	 */
	public Rate(Rate rate, TUnit unit) {
		this(rate.per(unit), unit.dt, rate.tag);
	}
	
	/**
	 * Warning: makes a new Rate! Rates are immutable.
	 * @param tag
	 * @return a copy with the given tag
	 */
	public Rate setTag(String stag) {
		return new Rate(x, dt, stag);
	}

	/**
	 * Can be null
	 */
	public final String tag;
	public final Dt dt;
	public final double x;

	public double get() {
		return x;
	}

	@Override
	public String toString() {
		if (x==AUTO) return "auto";
		String dts = dt.getValue() == 1 ? dt.getUnit().toString().toLowerCase()
				: dt.toString();
		return Printer.prettyNumber(x, 2) + "/" + dts;
	}

//	@Override
	public double doubleValue() {
		return get();
	}

	public double per(TUnit day) {
		return day.getMillisecs() * x / dt.getMillisecs();
	}
	
	public double per(Dt gap) {
		return gap.getMillisecs() * x / dt.getMillisecs();
	}
	
	@Override
	public int compareTo(Rate o) {
		double a = per(TUnit.SECOND);
		double b = o.per(TUnit.SECOND);
		return a==b? 0 : a<b? -1 : 1;
	}

	@Override
	public void appendJson(StringBuilder sb) {
		sb.append(toJSONString());
	}

	@Override
	public String toJSONString() {
		String json = new SimpleJson().toJson(toJson2());
		return json;
	}

	@Override
	public Object toJson2() throws UnsupportedOperationException {
		return new ArrayMap(
				"dt", dt.toJson2(),
				"tag", tag,
				"x", x,
				"str", toString()
				);
	}

	/**
	 * @return the gap between events, if they are evenly spaced.
	 */
	public Dt getGap() {
		if (x==0) {
			return TimeUtils.FOREVER;
		}
		return new Dt(dt.getValue() / x, dt.getUnit());
	}

	public boolean isLessThan(Rate big) {
		return compareTo(big)==-1;
	}
	
	public boolean isGreaterThan(Rate big) {
		return compareTo(big)==1;
	}


}
