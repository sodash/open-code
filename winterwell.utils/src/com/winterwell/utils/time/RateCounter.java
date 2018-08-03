package com.winterwell.utils.time;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.winterwell.datalog.DataLog;
import com.winterwell.datalog.Rate;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;

/**
 * @see DataLog which largely replaces this with low-cost persistent rates.
 * <p>
 * Counter eg. to measure throughput or throttle rate-limited operations. Also
 * keeps a global running total. Should be thread-safe.
 * <p>
 * Note: If the rate is likely to be low, you'll get more accurate figures by
 * using a larger time-period and dividing the answer.
 * 
 * @author daniel
 * @testeby RateCounterTest
 * @see Rate, which is an immutable measure of Rate (with no counting code itself).
 */
public class RateCounter {

	AtomicInteger count2 = new AtomicInteger();

	Time cycleAt;

	/**
	 * The number of cycles we've gone through. If cycles==0 then the rate may
	 * be bogus/unset.
	 */
	int cycles;

	private Dt dt;

	@Deprecated
	transient private Double dt2;
	Time firstCycleAt;

	@Deprecated
	// delete after updating sodash
	transient Double frac;

	private Dt halfDt;

	// TODO this is a crude hack to provide smooth transition when we tick over
	// - can do better
	double oldCount;

	boolean overrideIfFirstCycle = true;

	@Deprecated
	// not thread safe. delete after updating sodash
	transient Long total;

	final AtomicLong ttl = new AtomicLong();

	/**
	 * 
	 * @param dt
	 *            the time period to calculate the rate over. Will use buckets
	 *            of half this period. If the rate is likely to be low, you'll
	 *            get more accurate figures by using a larger time-period and
	 *            dividing the answer.
	 */
	public RateCounter(Dt dt) {
		this.dt = dt;
		halfDt = dt.multiply(0.5);
		cycleAt = new Time().plus(halfDt);
		firstCycleAt = new Time();
	}

	/**
	 * @assumption This should be called (ie. the counter should be accessed via
	 *             get() or plus()) at least once per cycle. Otherwise, count
	 *             will not get flushed to zero properly. E.g. suppose dt = 1
	 *             day, and the counter is accessed twice on Monday, then not at
	 *             all. A reading taken on Friday will give a rate of one, as
	 *             the Monday:2 will go into oldCount, rather than being
	 *             discarded as they should be.
	 */
	private void cycle() {
		Time now = new Time();
		if (now.longValue() < cycleAt.longValue())
			return;
		synchronized (this) {
			// race condition
			if (now.longValue() < cycleAt.longValue())
				return;
			// double oldOld = oldCount;
			oldCount = count2.get();
			assert MathUtils.isFinite(oldCount) : count2;
			count2.set(0);
			cycles++;
			// Log.trace(cycles);
			cycleAt = now.plus(halfDt);
			// dt2 = (oldCount - oldOld); doesn't work?!
		}
	}

	public double get() {
		cycle();

		// Special case for if the first cycle isn't done yet.
		// Removes misleading multiply-ups in the case of dates.
		double _frac = get2_fraction();

		// first cycle?
		if (cycles == 0) {
			if (_frac < MathUtils.getMachineEpsilon())
				return 0;
			double v = count2.get() / _frac;
			assert MathUtils.isFinite(v) : v;
			return v;
		}

		double v = oldCount + count2.get();
		v = v / (0.5 + _frac);
		assert MathUtils.isFinite(v) : v;
		return v;
	}

	double get2_fraction() {
		if (cycles == 0 && overrideIfFirstCycle)
			return 1;
		// weighted sum of old and new
		Time now = new Time();
		double _frac = 1.0 - now.diff(cycleAt) / (1.0 * halfDt.getMillisecs());
		assert _frac >= 0 && _frac <= 1 : _frac + " " + now.longValue() + " "
				+ cycleAt.longValue();
		_frac *= 0.5;
		return _frac;
	}

	/**
	 * Atomic get-then-set operation.
	 * 
	 * @see #getTotal()
	 * @param newValue
	 */
	public long getAndSetTotal(long newValue) {
		return ttl.getAndSet(newValue);
	}

	/**
	 * For nicer Strings.
	 * 
	 * @param sb
	 * @param period
	 *            The reporting period. E.g. a probe might run every 30 minutes,
	 *            but report a per-day rate.
	 * @param max
	 *            Cap returned values at this level. Use Integer.MAX_VALUES if
	 *            you don't care
	 * @return the (rounded) rate-per-period, possibly capped.
	 */
	public int getPer(TUnit period, int max) {
		double n = period.dt.divide(dt);
		double rate = get() * n;
		int cnt = (int) Math.round(rate);
		if (cnt == 0)
			return 0;
		// Have we maxed out?
		// Allow some tolerance for peak/slack periods
		if (cnt > 0.85 * max)
			return max;
		return cnt;
	}

	public Dt getPeriod() {
		return dt;
	}

	public long getTotal() {
		// if (ttl==null) ttl = new AtomicLong(total);
		// total = 0;
		return ttl.get();
	}

	public boolean isFirstCycle() {
		return cycles == 0;
	}

	public void plus(int i) {
		// if (ttl == null) { // FIXME delete soon
		// // old version seen 25th Nov 2011 :(
		// ReflectionUtils.setPrivateField(this, "ttl", new AtomicLong(
		// total == null ? 0 : total));
		// }

		ttl.addAndGet(i);
		cycle();
		count2.addAndGet(i);
		// assert get() >= i/2 : this; // this is a good sanity test, but there
		// is a possible race-condition
	}

	/**
	 * Override the counter multiplying up if we are within the first cycle?
	 * If true (the default) then the first cycle will not apply a fractional
	 * boost.
	 */
	public void setFirstDtOverride(boolean override) {
		this.overrideIfFirstCycle = override;
	}

	/**
	 * Allow manual poking of the rate, e.g. for new searches which discover a
	 * lot of old events.
	 * <p>
	 * Note: This also sets cycle time back and increments the cycles count by
	 * 1. It does *not* add anything to the total.
	 * 
	 * @param rate
	 */
	public void setRate(double rate) {
		this.overrideIfFirstCycle = false;
		assert Math.abs(rate) < Float.MAX_VALUE : rate;
		oldCount = rate / 2;
		assert MathUtils.isFinite(oldCount) : rate + " " + oldCount;
		count2.set(0);
		cycleAt = new Time().plus(halfDt);
		// This counts as a cycle
		cycles++; // also cycles=0 triggers get() to ignore oldCount
	}

	/**
	 * E.g. "100 per hour"
	 */
	@Override
	public String toString() {
		String dts = dt.getValue() == 1 ? dt.getUnit().toString().toLowerCase()
				: dt.toString();
		return Printer.prettyNumber(get(), 2) + "/" + dts;
	}

	/**
	 * Convenience for converting x-per-hour into a nice string
	 * 
	 * @param rate
	 * @param interval
	 * @return
	 */
	public static String str(double rate, Dt interval) {
		RateCounter rc = new RateCounter(interval);
		rc.setRate(rate);
		return rc.toString();
	}

	/**
	 * @return the rate
	 */
	public Rate rate() {
		return new Rate(get(), getPeriod(), null);
	}
}
