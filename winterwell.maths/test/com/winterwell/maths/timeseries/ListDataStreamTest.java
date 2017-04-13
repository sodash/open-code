package com.winterwell.maths.timeseries;

import org.junit.Test;

import com.winterwell.utils.Printer;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.XStreamUtils;

public class ListDataStreamTest {

	@Test
	public void testEmpty() {
		{
			ListDataStream lds = new ListDataStream(new double[0]);
			for (Datum datum : lds) {
				System.out.println(datum);
			}
		}
		{
			ListDataStream lds = new ListDataStream(1);
			for (Datum datum : lds) {
				System.out.println(datum);
			}
		}
	}

	@Test
	public void testEmpty2() {
		{ // de-serialisation problems?
			ListDataStream lds = new ListDataStream();
			for (Datum datum : lds) {
				System.out.println(datum);
			}
		}
	}
	
	@Test
	public void testAdd() {
		{
			ListDataStream lds = new ListDataStream(1);
			for(int i=0; i<3; i++) {
				lds.add(new Datum(i));
			}
		}
		{
			ListDataStream lds = new ListDataStream(1);
			for(int i=0; i<3; i++) {
				lds.add(new Datum(new Time(i,1,1), i, null));
			}
		}
		{
			ListDataStream lds = new ListDataStream(1);
			for(int i=0; i<3; i++) {
				lds.add(new Datum(new Time(i,1,1), i, null));
			}
			try {
				lds.add(new Datum(new Time(1,1,1), 1, null));
				assert false; // out of order
			} catch (Exception e) {
				// good
			}
		}
	}

	@Test
	public void testSerialisation() {
		{
			ListDataStream lds = new ListDataStream(2);
			lds.add(new Datum(new double[] { 1, 2 }));
			lds.add(new Datum(new double[] { 2, 3 }));
			lds.add(new Datum(new double[] { 3, 3 }));
			// save
			String xml = XStreamUtils.serialiseToXml(lds);
			ListDataStream lds2 = XStreamUtils.serialiseFromXml(xml);
			assert lds.getDim() == 2;
			assert lds2.getDim() == 2 : lds2.getDim() + " " + lds2;
			assert lds2.size() == 3;
			for (int i = 0; i < 3; i++) {
				assert Datum.equals(lds2.get(i), lds.get(i)) : lds2.get(i)
						+ " vs " + lds.get(i);
			}
		}
		if (false) { // bug from egan
			String s = "H4sIAAAAAAAAAH2PsU7DMBCGz0m6MCHYEAxMSAw2e6ZKLEWWGEAMbBfiFldOiewLMWw8AiMDb8B7"
					+ "sPMeTIws9aUDGVA9nGSf/+///49vmAQPp71dkfG9cU42SPdBkm1MMN6aILUNdI6EV+QNNrA5IoNs"
					+ "BsJqKOq0I9jVS3xE1ZF1ihVl9HCyDTv9jylmkNe2iW26ZvyWwu0zWDJYTr3HJ6bHl6+jt098z1lR"
					+ "BPtsBonoC55JdLzNOjl3I9P8Fnaah9rOLVbOaJg4rIwj2NtUcrhaqMtqae6oTHWZQ3Co/wyG0kHx"
					+ "Ql2nUcZWtCnDwSjD8GXIIPnLqPEFZB1xenH28/p7E9fxqasVkwEAAA==";
			s = "<winterwell.maths.timeseries.ListDataStream>" + s
					+ "</winterwell.maths.timeseries.ListDataStream>";
			ListDataStream lds = XStreamUtils.serialiseFromXml(s);
			Printer.out(lds);
		}
	}
}
