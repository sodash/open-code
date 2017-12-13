package com.winterwell.maths.chart;

import org.junit.Test;

import com.winterwell.utils.Printer;

public class LogGridInfoTest {


	@Test
	public void testLogGridInfo_probability() {
		LogGridInfo lgi = new LogGridInfo(1, 10);		
		assert lgi.getMin() == 0 : lgi;
		assert lgi.getMax() == 1;
		double[] sps = lgi.getStartPoints();
		Printer.out("Start Points: ", sps);
		Printer.out("Mid Points: ", lgi.getMidPoints());
		Printer.out("End Points: ", lgi.getEndPoints());
		assert sps[0] == 0;
		assert sps[1] > 0 && sps[1] < 1;
	}

	@Test
	public void testLogGridInfo_probabilityFineGrid() {
		// a fine grid
		LogGridInfo lgi = new LogGridInfo(1, 20, 0, 1.1);		
		assert lgi.getMin() == 0 : lgi;
		assert lgi.getMax() == 1;
		double[] sps = lgi.getStartPoints();
		Printer.out("Start Points: ", sps);
		Printer.out("Mid Points: ", lgi.getMidPoints());
		Printer.out("End Points: ", lgi.getEndPoints());
		assert sps[0] == 0;
		assert sps[1] > 0 && sps[1] < 1;
	}

	@Test
	public void testLogGridInfo_probability2() {
		// a fine grid
		LogGridInfo lgi = new LogGridInfo(1, 4, 0, 2);
		double[] sps = lgi.getStartPoints();
		Printer.out("Start Points: ", sps);
		assert lgi.getMin() == 0 : lgi;
		assert lgi.getMax() == 1;		
		Printer.out("Mid Points: ", lgi.getMidPoints());
		Printer.out("End Points: ", lgi.getEndPoints());
		String s = "";
		for(int i=0; i<lgi.size(); i++) {
			s += "["+lgi.getBucketBottom(i)+", "+lgi.getBucketTop(i)+"], ";
		}
		s = s.substring(0, s.length()-2);
		System.out.println(s);
		assert s.equals("[0.0, 0.125], [0.125, 0.25], [0.25, 0.5], [0.5, 1.0]") : s; 
		assert sps[0] == 0;
		assert sps[1] > 0 && sps[1] < 1;
	}

	
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
