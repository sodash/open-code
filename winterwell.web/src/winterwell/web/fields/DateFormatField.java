package winterwell.web.fields;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import winterwell.utils.time.Time;

/**
 * Parse a Date from one user-set format 
 * @see TimeField which is more flexible (but re-uses {@link #toString2(Time)} from here)
 * @see DateField which is more flexible
 * @author daniel
 * @testedby {@link DateFieldTest}
 */
public class DateFormatField extends AField<Time> {	
	private static final long serialVersionUID = 1L;
	final DateFormat df;

	public DateFormatField(String name, DateFormat df) {
		super(name, "text");
		this.df = df;
	}

	@Override
	public Time fromString(String v) throws ParseException {
		// NOTE: SimpleDateFormat.parse and SimpleDateFormat.format
		// are not thread safe... hence the .clone
		// (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4228335)
		// - @richardassar
		DateFormat df2 = (DateFormat) df.clone();
		df2.setLenient(df.isLenient());
		
		Date date = df2.parse(v);
		if (date==null) {
			String patternForDebug;
			if (df2 instanceof SimpleDateFormat) {
				patternForDebug = ((SimpleDateFormat) df2).toPattern();
			} else {
				patternForDebug = df.toString();
			}
			throw new NullPointerException(v+" parsed with "+patternForDebug+" = null");
		}
		Time t = new Time(date);
		return t;
	}

	@Override
	public String toString(Time time) {
		return df.format(time.getDate());
	}
	
	
	@Override
	public Class getValueClass() {
		return Time.class;
	}
}
