package com.winterwell.datalog;

import winterwell.utils.Utils;
import winterwell.utils.time.Dt;
import winterwell.utils.time.TUnit;
import winterwell.utils.time.Time;

/**
 * Volume alerts.
 * 
 * Usage: Attach it to Stat. Every call to Stat.count will call {@link #handleCount(double, double, String)}
 * 
 * Alert alert = new Alert(rate);
 * Stat.setListener(alert, alert.getTag());
 * 
 * @author daniel
 */
public class Alert implements IListenStat {

	private IListenStat handler;

	public void setBelow(boolean below) {
		this.below = below;
	}
	
	public IListenStat getHandler() {
		return handler;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (below ? 1231 : 1237);
		result = prime * result + ((handler == null) ? 0 : handler.hashCode());
		result = prime * result + ((threshold == null) ? 0 : threshold.hashCode());
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
		Alert other = (Alert) obj;
		if (below != other.below)
			return false;
		if (handler == null) {
			if (other.handler != null)
				return false;
		} else if (!handler.equals(other.handler))
			return false;
		if (threshold == null) {
			if (other.threshold != null)
				return false;
		} else if (!threshold.equals(other.threshold))
			return false;
		return true;
	}

	public boolean isBelow() {
		return below;
	}

	public Rate getThreshold() {
		return threshold;
	}

	public Alert(Rate threshold) {
		this.threshold = threshold;
		this.bucketEnd = new Time().plus(threshold.dt);
		assert ! Utils.isBlank(threshold.tag);
	}
	

	public void setHandler(IListenStat handler) {
		this.handler = handler;
	}
	
	final Rate threshold;

	/**
	 * If true, this is an alert for low volumes
	 */
	boolean below;

	Dt minInterval = new Dt(12, TUnit.HOUR);

	public Dt getMinInterval() {
		return minInterval;
	}
	
	Time bucketEnd;

	Time lastAlert;

	transient double x;
	
	/**
	 * true if this is NOT the first bucket.
	 * Used to avoid firing low-count alerts for the first (possibly short) bucket.
	 */
	private transient boolean bucket2;
	
	@Override
	public synchronized void handleCount(double _x, double dx, String tag) {
		Time now = new Time();
		// Swap buckets?
		if (now.isAfter(bucketEnd)) {
			// below alerts only fire at the end of a bucket
			if (below && bucket2 && x < threshold.x) {
				fire(now);
			}
			// new bucket
			bucketEnd = bucketEnd.plus(threshold.dt);
			bucket2 = true;
			x = 0;
		}
		
		x += dx; // What about thread safety??
		if ( ! below && x > threshold.x) {
			fire(now);
		}
	}

	private void fire(Time now) {
		if (lastAlert != null && lastAlert.dt(now).isShorterThan(minInterval)) {
			// no fire
			return;
		}
		// Count Alert firing
		Stat.count(1, getFiringStatTag());
		setLastAlert(new Time());
		// handle it
		handler.handleCount(x, 0, threshold.tag);
	}
	
	public String[] getFiringStatTag() {
		return new String[]{"Alert.fire", threshold.tag+"_"+threshold};
	}

	public String toString(){
		String s = "Alert: ";
		s = s + "rate is " + (below == true? "below" : "above") + " " + threshold;
		return s;
	}
	
	public String getTag() {
		return threshold.tag;
	}

	public double getAt(){
		return x;
	}
	
	/**
	 * @deprecated Mostly managed internally by fire()
	 * @param time
	 */
	public void setLastAlert(Time time) {
		this.lastAlert = time;				
	}
	
	public Time getLastAlert() {
		return lastAlert;
	}

}
