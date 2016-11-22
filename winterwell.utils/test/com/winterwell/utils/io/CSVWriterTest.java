package com.winterwell.utils.io;

import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Containers;

import junit.framework.TestCase;
import com.winterwell.utils.io.FileUtils;

public class CSVWriterTest extends TestCase {

	public void testCSVReaderLinkup() {
		File f = new File("test/csvwriter.txt");
		f.getParentFile().mkdirs();
		CSVWriter w = new CSVWriter(f, '|');
		w.write(new Object[] { "a", "b", "c" });
		w.write(new Object[] { "a|a2", "b\\|b2", "c" });
		w.close();

		CSVReader r = new CSVReader(f, '|');
		List<String[]> list = Containers.getList(r);
		Printer.out(list);

		assert list.size() == 2 : Printer.toString(list.toArray());
		List<String> row0 = Arrays.asList(list.get(0));
		List<String> row1 = Arrays.asList(list.get(1));
		assert Arrays.asList("a", "b", "c").equals(row0) : row0;
		assert Arrays.asList("a|a2", "b\\|b2", "c").equals(row1) : row1;
		FileUtils.delete(f);
	}

	public void testCSVWriterFileChar() {
		File f = new File("test/csvwriter.txt");
		CSVWriter w = new CSVWriter(f, '|');
		w.write(new Object[] { "a|a2", "b|b2", "c" });
		w.close();

		String txt = FileUtils.read(f);
		assertEquals("\"a|a2\"|\"b|b2\"|c\n", txt);

		FileUtils.delete(f);
	}

	public void testMethodRouting() {
		StringWriter sw = new StringWriter();
		CSVWriter w = new CSVWriter(sw, '|', '"');
		Object strings = new String[] { "a", "b" };
		w.write(strings);
		w.close();
		assert sw.toString().trim().equals("a|b") : sw.toString();
	}

	// amended to setAppend characteristic as option during opening of
	// newCSVWriter
	//
	public void testSetAppend() {
		File f = new File("test/csvwriter.txt");
		FileUtils.write(f, "# Hello\n");
		CSVWriter w = new CSVWriter(f, '|', true);
		w.write(new Object[] { "a", "b", "c" });
		w.close();

		String txt = FileUtils.read(f);
		assertEquals("# Hello\na|b|c\n", txt);

		FileUtils.delete(f);
	}

	public void testNumericalWrite() {
		File f = new File("test/csvnumwriter.txt");

		CSVWriter w = new CSVWriter(f, ',');
		w.write(new Object[] { 1L, 2, (float) 3.0, (double) 4.0 });
		w.close();

		String txt = FileUtils.read(f);
		assertEquals("1,2,3.0,4.0\n", txt);

		FileUtils.delete(f);
	}

}
