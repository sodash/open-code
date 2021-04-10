package com.winterwell.maths.datastorage;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.winterwell.utils.Printer;
import com.winterwell.utils.io.CSVSpec;
import com.winterwell.utils.io.CSVWriter;

public class DataTableTest {


	@Test
	public void testMerge() {
		List d1 = Arrays.asList(
			Arrays.asList("", "2020-11-30", "2020-12-31"),
			Arrays.asList("Income", 100, 200),
			Arrays.asList("Costs", 50, 100)
		);
		DataTable dt1 = new DataTable<>(d1);
		
		List d2 = Arrays.asList(
				Arrays.asList("", "2020-10-31", "2020-11-30"),			
			Arrays.asList("Costs", 25, 50),	// NB: swap the row order to test row-name matching
			Arrays.asList("Income", 50, 100)
		);
		DataTable dt2 = new DataTable<>(d2);
		DataTable dtm = DataTable.merge(dt1, dt2);
		
		StringWriter sw = new StringWriter();
		CSVWriter w = new CSVWriter(sw, new CSVSpec());
		dtm.save(w);
		
		String csv = sw.toString();
		System.out.println(csv);
		assert csv.equals(",2020-10-31,2020-11-30,2020-12-31\n"
				+ "Income,50,100,200\n"
				+ "Costs,25,50,100\n") : csv;
	}

	
	@Test
	public void testAddStrings() {
		DataTable<String> dt = new DataTable(null);
		dt.add("A", "B");
		String[] row = new String[]{"C", "D"};
		dt.add(row);
		System.out.println(dt);
		for (Object[] r : dt) {
			Printer.out(r);
		}
		Object[] row2 = dt.get("C");
		assert row2 != null;
		assert row2[0] == "C";
		assert row2[1] == "D";
	}

}
