package com.winterwell.utils.time;

import java.util.List;

import org.junit.Test;

import com.winterwell.utils.Printer;
import com.winterwell.utils.time.TimeUtils;

import com.winterwell.utils.containers.AbstractIterator;

public class OfficeHoursPeriodTest {
	
	Time sun2pm = new Time(2014,1,19,14,0,0);
	Time mon11am = new Time(2014,1,20,11,0,0);
	Time mon5pm = new Time(2014,1,20,17,0,0);
	Time mon7pm = new Time(2014,1,20,19,0,0);
	Period tues = new Period(new Time(2014,1,21,9,0,0), new Time(2014,1,21,17,0,0));
	Time thurs9am = new Time(2014,1,23,9,0,0);
	Time thurs4pm = new Time(2014,1,23,16,0,0);
	Time thurs7pm = new Time(2014,1,23,19,0,0);
	Time fri10pm = new Time(2014,1,24,22,0,0);
	Time sat10pm = new Time(2014,1,25,22,0,0);

	@Test
	public void testGetTotalOfficeTime() {
		{
			OfficeHours oh = new OfficeHours(TimeUtils._GMT_TIMEZONE);		
			OfficeHoursPeriod ohp = new OfficeHoursPeriod(oh, mon11am, mon7pm);
			Dt total = ohp.getTotalOfficeTime();
			assert total.equals(new Dt(6,TUnit.HOUR)) : total;
		}
		{	// several days
			OfficeHours oh = new OfficeHours(TimeUtils._GMT_TIMEZONE);		
			OfficeHoursPeriod ohp = new OfficeHoursPeriod(oh, mon11am, thurs4pm);
			Dt total = ohp.getTotalOfficeTime();
			assert total.equals(new Dt(6 + 8 + 8 + 7, TUnit.HOUR)) : total;
		}
		{	// out of hours
			OfficeHours oh = new OfficeHours(TimeUtils._GMT_TIMEZONE);		
			OfficeHoursPeriod ohp = new OfficeHoursPeriod(oh, fri10pm, sat10pm);
			Dt total = ohp.getTotalOfficeTime();
			assert total.getMillisecs() == 0 : total;
		}
	}

	@Test
	public void testIterator() {
		{	// output
			OfficeHours oh = new OfficeHours(TimeUtils._GMT_TIMEZONE);
			OfficeHoursPeriod ohp = new OfficeHoursPeriod(oh, mon11am, TimeUtils.WELL_FUTURE);
			AbstractIterator<Period> it = ohp.iterator();
			List<Period> ps = it.next(10);
			System.out.println(Printer.toString(ps, "\n"));
		}
		{	// within 1 day
			OfficeHours oh = new OfficeHours(TimeUtils._GMT_TIMEZONE);
			OfficeHoursPeriod ohp = new OfficeHoursPeriod(oh, mon11am, mon7pm);
			AbstractIterator<Period> it = ohp.iterator();
			Period n = it.next();
			assert n.equals(new Period(mon11am, mon5pm)) : n;
			assert ! it.hasNext() : it.next();
		}		
		{
			OfficeHours oh = new OfficeHours(TimeUtils._GMT_TIMEZONE);
			OfficeHoursPeriod ohp = new OfficeHoursPeriod(oh, mon11am, thurs4pm);
			AbstractIterator<Period> it = ohp.iterator();
			Period n = it.next();
			assert n.equals(new Period(mon11am, mon5pm)) : n;
			
			Period n2 = it.next();
			assert n2.equals(tues) : n2;
			
			Period n3 = it.next(); // weds
			
			Period n4 = it.next(); // thurs
			assert n4.equals(new Period(thurs9am, thurs4pm)) : n;
			
			assert ! it.hasNext() : it.next(); 
		}
		
	}

}
