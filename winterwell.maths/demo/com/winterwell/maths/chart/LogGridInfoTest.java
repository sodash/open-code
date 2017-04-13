package com.winterwell.maths.chart;

import org.junit.Test;

import com.winterwell.utils.Printer;

public class LogGridInfoTest {

	@Test
	public void testLogGridInfo_base10() {
		LogGridInfo lgi = new LogGridInfo(1000, 3);		
		assert lgi.getMin() == 0 : lgi;
		assert lgi.getMax() == 1000;
		double[] sps = lgi.getStartPoints();
		Printer.out("Start Points: ", sps);
		Printer.out("Mid Points: ", lgi.getMidPoints());
		Printer.out("End Points: ", lgi.getEndPoints());
		System.out.println(lgi);
	}
	

	@Test
	public void testLogGridInfo_base2() {
		LogGridInfo lgi = new LogGridInfo(8);		
		double[] sps = lgi.getStartPoints();
		Printer.out("Start Points: ", sps);
		Printer.out("Mid Points: ", lgi.getMidPoints());
		Printer.out("End Points: ", lgi.getEndPoints());
		assert lgi.getMin() == 0 : lgi;
		assert lgi.getMax() == 128 : lgi.getMax();
		System.out.println(lgi);
	}


}
