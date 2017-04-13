package com.winterwell.maths.datastorage;

import org.junit.Test;

import com.winterwell.utils.Printer;

public class DataTableTest {

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
