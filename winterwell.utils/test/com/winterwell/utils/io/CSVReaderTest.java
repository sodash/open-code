package com.winterwell.utils.io;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.winterwell.utils.TimeOut;
import com.winterwell.utils.time.StopWatch;

import junit.framework.TestCase;
import winterwell.utils.StrUtils;
import winterwell.utils.containers.Containers;

public class CSVReaderTest extends TestCase {

	public void testComments() {
		StringReader input = new StringReader("# hello\na|b|c\n1|2|3");
		CSVReader r = new CSVReader(input, '|', '"', '#');
		String[] line = r.next();
		assert line[0].equals("a");
	}

	public void testEasy() {
		{
			File file = new File("test/test-easy.csv");
			CSVReader r = new CSVReader(file, '|', '"');
			assert r.hasNext();
			String[] row1 = r.next();
			assertEquals(0, r.getRowNumber());
			assertEquals(1, r.getLineNumber());
			String[] row2 = r.next();
			assertEquals(1, r.getRowNumber());
			assertEquals(2, r.getLineNumber());
			assert r.hasNext();
			String[] row3 = r.next();
			assert r.getRowNumber() == 2;
			assert r.getLineNumber() == 3;
			assert !r.hasNext();
			assert row2[0].equals("Daniel Winterstein") : row2[0];
			assert row2[1].equals("Overrated");
			assert row2[2].equals("yes") : row2[2];
			assert row2.length == 3;
		}
	}

	public void testEscapes() {
		{
			StringReader in = new StringReader(
					"\"col 0 | \\\"| col 1 | col 2\n");
			CSVReader r = new CSVReader(in, '|', '"', '#');
			assert r.hasNext();
			String[] row1 = r.next();
			assert !r.hasNext();
			assert row1[0].equals("col 0 | \\") : row1[0];
			assert row1[1].equals(" col 1 ");
			assert row1.length == 3;
		}
		{
			// StringReader in = new StringReader("Ugh computers make me all
			// kinds of confused. :( i just wanna copy right and make mp3's! :(
			// I'll try again tomorrow. :-\|colleenwild|Fri Aug 28 05:29:46 BST
			// 2009|sad");
		}
	}

	public void testHard() {
		{
			File file = new File("test/test-hard.csv");
			CSVReader r = new CSVReader(file, '|', '"');
			assert r.hasNext();
			String[] row1 = r.next();
			String[] row2 = r.next();
			assert row2[0].equals("Daniel \n Winterstein") : row2[0];
			assert row2[1].equals("Overrated");
			assert row2[2].equals("yes") : row2[2];
			assert row2.length == 3;
			assertEquals(1, r.getLineNumber());
			assert r.hasNext();
			assert r.hasNext();
			assert r.hasNext();
			String[] row3 = r.next();
			assertEquals(3, r.getLineNumber());
			assertEquals(2, r.getRowNumber());
			String[] row4 = r.next();
			assert row4[0].equals("Jim Read") : row4[0];
			assert row4[1].equals("");
			assert row4[2].equals("yes") : row4[2];
			assertEquals(6, r.getLineNumber());
			assertEquals(3, r.getRowNumber());
			assert !r.hasNext();
		}
	}

	public void testQuotes() {
		StringReader in = new StringReader(
				"Jun-09,36,HN,11,\"International Exchange & Cooperation Centre, Mingde College\",320\n");
		CSVReader r = new CSVReader(in, ',', '"', '#');
		List<String[]> ls = Containers.getList(r);
		assertEquals(
				"International Exchange & Cooperation Centre, Mingde College",
				ls.get(0)[4]);
	}

	public void testQuoting() {
		StringReader in = new StringReader("a|b|\"\"\"scare quotes\"\"\"");
		CSVReader r = new CSVReader(in, '|', '"', '#');
		List<String[]> ls = Containers.getList(r);
		assertEquals("\"scare quotes\"", ls.get(0)[2]);
	}

	/**
	 * 
	 * <p>
	 * Nov 5th 2012: Comparing StringBuilder/StringBuffer Builder: 7.88, 7.89
	 * Buffer: 8.99, 9.1 StringBuilder wins as expected.
	 * 
	 */
	public void testPerformance() {
		List<String> fields = new ArrayList<String>(300);
		for (int i = 0; i < 300; i++) {
			fields.add("field" + i);
		}
		String line = StrUtils.join(fields, ",");
		StopWatch sw = new StopWatch();
		for (int i = 0; i < 100000; i++) {
			CSVReader reader = new CSVReader(new StringReader(line), ',', '"',
					'#');
			reader.next();
		}
		sw.print();
	}

	/*
	 * Reproduces a hang seen in RFM (with incorrectly formatted files)
	 * @throws InterruptedException
	 */
	public void testRfmHang() throws InterruptedException {
		// JH: I couldn't get the timeouter to work properly, but think that's just some quirk of
		// my setup that I haven't time to debug just now...
		TimeOut timeout = new TimeOut(1000);
		try {
			timeout.canThrow();
			StringReader in = new StringReader("#headerline");
			CSVReader r = new CSVReader(in, ',', '"', '#');
			r.next();
		}
		finally {
			timeout.cancel();
		}

	}

}
