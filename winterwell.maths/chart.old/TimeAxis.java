package com.winterwell.maths.chart;

import java.util.TimeZone;

import com.winterwell.utils.containers.Range;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

public class TimeAxis extends NumericalAxis {

	public TimeZone timeZone = TimeUtils._GMT_TIMEZONE;

	public TimeAxis() {
		this.type = AxisType.DATETIME;
	}

	public TimeAxis(Time start, Time end) {
		this();
		
		setRange(start, end);
	}

	public Time getEndTime() {
		return new Time((long) getRange().high);
	}

	public Time getStartTime() {
		return new Time((long) getRange().low);
	}

	public void setRange(Time start, Time end) {
		setRange(new Range(start.longValue(), end.longValue()));
	}

	@Override
	public String toString() {
		return "TimeAxis[" + getStartTime() + ", " + getEndTime() + "]";
	}
	
	public void setTimeZone(TimeZone tz){
		this.timeZone = tz; 
	}

}
