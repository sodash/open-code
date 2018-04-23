package com.winterwell.utils.time;

import java.io.Serializable;
import java.time.Duration;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.utils.web.SimpleJson;

/**
 * An amount of time, e.g. 5 minutes. This class is immutable. Dts can be
 * positive or negative (positive is more common).
 * 
 * @testedby {@link DtTest}
 * @author Daniel
 * 
 */
public final class Dt implements Comparable<Dt>, Serializable, IHasJson {
	private static final long serialVersionUID = 1L;

	private final double n;
	private final TUnit unit;

	public Dt(double n, TUnit unit) {
		this.n = n;
		this.unit = unit;
		assert unit != null;
	}

	/**
	 * @return ISO 8601 duration format PdateTtime
	 * See https://en.wikipedia.org/wiki/ISO_8601#Durations
	 */
	public String toISOString() {
		return "P"+(unit.millisecs < TUnit.DAY.millisecs? "T" : "")+n+unit.toString().substring(0, 1);		
	}
	
	/**
	 * Convenience for a Dt measured in milliseconds.
	 * 
	 * @param millisecs
	 */
	public Dt(long millisecs) {
		this(millisecs, TUnit.MILLISECOND);
	}

	public Dt(long timeout, TimeUnit unit) {
		TUnit tu = TUnit.valueOf(unit);
		if (tu != null) {
			this.n = timeout;
			this.unit = tu;
			return;
		}
		this.n = unit.toMillis(timeout);
		this.unit = TUnit.MILLISECOND;
	}

	/**
	 * Some rounding may occur if the value is not an integer. The integer part
	 * of a Dt is added using Calendar's field-specific mechanisms. The
	 * fractional part is added as milliseconds.
	 * 
	 * @param cal
	 */
	public void addTo(Calendar cal) {
		// add integer part using Calendar field
		int i = (int) n;
		cal.add(unit.getCalendarField(), i);
		double r = n - i;
		if (r == 0)
			return;
		// add fractional part using millisecs
		int ms = (int) (r * unit.millisecs);
		cal.add(TUnit.MILLISECOND.getCalendarField(), ms);
	}

	/**
	 * Dts are typically positive, eg. +1 hour. This convenience method returns
	 * the opposite negative Dt, eg. -1 hour. (It is currently an error to call
	 * this on a Dt which is already negative)
	 */
	public Dt ago() {
		assert n >= 0 : this;
		return new Dt(-n, unit);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Dt dt2) {
		long ms = getMillisecs();
		long ms2 = dt2.getMillisecs();
		if (ms == ms2)
			return 0;
		return ms < ms2 ? -1 : 1;
	}

	/**
	 * Convert this Dt to a different unit.
	 * <p>
	 * Uses {@link #divide(Dt)}, which mostly does millisecond arithmetic. But
	 * it has a couple of special cases to handle: weeks-in-the-year
	 * months-in-the-year -- though for integer valued Dts only.
	 * 
	 * @param unit
	 * @return
	 */
	public Dt convertTo(TUnit tunit) {
		double n2 = divide(tunit.dt);
		return new Dt(n2, tunit);
	}

	/**
	 * Return the number of the specified time delta that would fit into this
	 * one e.g. TUnit.MINUTE.getDt().divide(TUnit.SECOND.getDt()) -> 60
	 * <p>
	 * Mostly this does millisecond arithmetic. But it has a couple of special
	 * cases to handle: weeks-in-the-year months-in-the-year -- though for
	 * integer valued Dts only.
	 * 
	 * @param bucketSize
	 */
	public double divide(Dt other) {
		if (n == 0)
			return 0;
		// special case integer arithmetic
		if (unit == TUnit.YEAR && other.n == Math.round(other.n)) {
			if (other.unit == TUnit.MONTH)
				return n * 12 / other.n;
			else if (other.unit == TUnit.WEEK)
				return n * 52 / other.n;
		}
		return (n * unit.millisecs) / (other.n * other.unit.millisecs);
	}

	/**
	 * Equals if the same to the millisecond. So 1 day equals 24 hours
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		// NOTE: TUnit is not equals()
		if (obj.getClass() != Dt.class)
			return false;
		Dt dt = (Dt) obj;
		return getMillisecs() == dt.getMillisecs();
	}

	/**
	 * @return the number of milliseconds represented by this period. This
	 *         rounds if we have a fraction.
	 */
	public long getMillisecs() {
		return Math.round(unit.millisecs * n);
	}

	public TUnit getUnit() {
		return unit;
	}

	/**
	 * E.g. the 5 in "5 minutes"
	 */
	public double getValue() {
		return n;
	}

	@Override
	public int hashCode() {
		return new Double(getMillisecs()).hashCode();
	}

	/**
	 * Compare absolute values. So +1 minute is shorter than -1 hour.
	 * 
	 * @param dt2
	 * @return true if this is the shorter Dt
	 */
	public boolean isShorterThan(Dt dt2) {
		assert dt2 != null;
		return Math.abs(getMillisecs()) < Math.abs(dt2.getMillisecs());
	}

	/**
	 * @param x
	 * @return A new Dt which is x times this Dt
	 */
	public Dt multiply(double x) {
		return new Dt(x * n, unit);
	}

	/**
	 * A new DT which keeps the unit of this dt
	 * 
	 * @param b If b is negative, then this will subtract.
	 * @return this + b
	 */
	public Dt plus(Dt b) {
		return new Dt(n + b.divide(unit.dt), unit);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		// avoid "1.0 week"				
		if (Math.abs(n-Math.round(n)) < 0.1) {
			long ni = Math.round(n);
			sb.append(ni);
		} else {
			sb.append((float) n);
		}
		sb.append(" ");
		sb.append(unit.toString().toLowerCase());
		if (n!=1) sb.append("s");
		return sb.toString();
	}

	@Override
	public void appendJson(StringBuilder sb) {
		new SimpleJson().appendJson(sb, toJson2());
	}

	@Override
	public String toJSONString() {
		StringBuilder sb = new StringBuilder();
		appendJson(sb);
		return sb.toString();
	}

	@Override
	public Map toJson2() throws UnsupportedOperationException {
		return new ArrayMap(
				"n",n,
				"unit",unit,
				"ms",getMillisecs(),
				"str", toString());
	}

	public static Dt max(Dt a, Dt b) {
		if (a==null) return b;
		if (b==null) return a;
		return a.isShorterThan(b)? b : a;
	}
	
	public static Dt min(Dt a, Dt b) {
		if (a==null) return b;
		if (b==null) return a;
		return a.isShorterThan(b)? a : b;
	}

}
