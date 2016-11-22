package winterwell.web.fields;

import java.util.TimeZone;

import winterwell.utils.time.TimeOfDay;

public class TimeOfDayField extends AField<TimeOfDay> {
	public TimeOfDayField(String name) {
		super(name, "text");
	}

	private static final long serialVersionUID = 1L;
	
	@Override
	public String toString(TimeOfDay value) {
		return value.toISOString();
	}

	TimeZone timezone;
	
	@Override
	public TimeOfDay fromString(String v) throws Exception {
		return new TimeOfDay(v, timezone);
	}
}
