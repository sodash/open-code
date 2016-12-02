package com.winterwell.utils.io;

import org.junit.Test;

import com.winterwell.utils.Printer;

public class SqlUtilsLiteTest {

	@Test
	public void testDebugInfo() {
		int[] info = SqlUtils.getPostgresThreadInfo("sodash");
		Printer.out(info);
	}
	


}
