package com.winterwell.utils.time;

import java.util.List;

import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Pair;

import junit.framework.TestCase;

public class TimeIteratorTest extends TestCase {

	public void testDateIterator() {
		Pair<Time> range = new Pair<Time>(new Time(2008, 12, 30), new Time(
				2009, 1, 2));
		TimeIterator ti = new TimeIterator(range.first, range.second, new Dt(1,
				TUnit.DAY));
		AbstractIterator<Time> di = ti.iterator();
		assert di.hasNext();
		Time next = di.peekNext();
		assert next.equals(new Time(2008, 12, 30)) : next;
		List<Time> list = Containers.getList(ti);
		assert list.size() == 4 : Printer.toString(list);
		assert list.get(0).equals(new Time(2008, 12, 30)) : list;
		assert list.get(1).equals(new Time(2008, 12, 31)) : list;
		assert list.get(2).equals(new Time(2009, 1, 1)) : list;
		assert list.get(3).equals(new Time(2009, 1, 2)) : list;
	}

	public void testEndPoints() {
		// includes start and end
		Time now = new Time();
		TimeIterator di = new TimeIterator(now, now, new Dt(1, TUnit.DAY));
		AbstractIterator<Time> it = di.iterator();
		assert it.hasNext();
		Time next = it.peekNext();
		assert next.equals(now) : next;
		List<Time> list = Containers.getList(di);
		assert list.size() == 1 : Printer.toString(list);
		assert list.get(0).equals(now) : list;
	}
}
