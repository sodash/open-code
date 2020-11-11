package com.winterwell.utils.time;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Time units. Not called TimeUnit to avoid clashing with
 * java.utils.concurrent.TimeUnit. Calculations involving months are
 * approximate! Use Calendar if you need to step through months.
 * 
 * @author Daniel
 */
public enum TUnit {
	/*
	 * NB These *must* be ordered from smallest to largest in order to support
	 * {@link #getNextSmallerUnit()}
	 */	
	MILLISECOND(Calendar.MILLISECOND, 1), 
	SECOND(Calendar.SECOND, 1000L), 
	MINUTE(Calendar.MINUTE, 1000L * 60), 
	HOUR(Calendar.HOUR_OF_DAY, 1000L * 60 * 60), 
	DAY(Calendar.DAY_OF_MONTH, 1000L * 60 * 60 * 24), 
	WEEK(Calendar.WEEK_OF_YEAR, 1000L * 60 * 60 * 24 * 7L),
	/**
	 * WARNING! This is the length of a month, provided that the month in
	 * question has 30 days. It is also the "one calendar month" unit for 
	 * calendar operations. 
	 */
	MONTH(Calendar.MONTH, 1000L * 60 * 60 * 24 * 30L),
	// Quarter??
	YEAR(Calendar.YEAR, 1000L * 60 * 60 * 24 * 365L);

	private final int cal;
	/**
	 * The time period for 1 unit. Equivalent to new Dt(1, unit)
	 */
	public final Dt dt;
	public final long millisecs;

	private TUnit(int cal, long millisecs) {
		this.cal = cal;
		this.millisecs = millisecs;
		this.dt = new Dt(1, this);
	}

	/**
	 * @deprecated Use {@link Dt#convertTo(TUnit)} for preference.
	 */
	@Deprecated
	public double convert(double amount, TUnit otherUnit) {
		Dt dt2 = new Dt(amount, otherUnit);
		return dt2.convertTo(this).getValue();
	}

	public int getCalendarField() {
		return cal;
	}

	/**
	 * @return The time period for 1 unit. Equivalent to new Dt(1, unit)
	 */
	public Dt getDt() {
		return dt;
	}

	public long getMillisecs() {
		return millisecs;
	}

	/**
	 * Retrieve the next smallest unit
	 */
	public TUnit getNextSmallerUnit() {
		int index = ordinal();
		if (index > 0) {
			index = index - 1;
		}
		return TUnit.values()[index];
	}

	/**
	 * @param timeunit
	 * @return WARNING Can return null -- TUnit does not cover the sub-calendar microsecond and nanosecond
	 */
	public static TUnit valueOf(TimeUnit timeunit) {
		switch(timeunit) {
		case MILLISECONDS: return MILLISECOND;
		case SECONDS: return SECOND;
		case MINUTES: return MINUTE;
		case HOURS: return HOUR;
		case DAYS: return DAY;
		}
		// Nano and micro!
		return null;
	}

}
