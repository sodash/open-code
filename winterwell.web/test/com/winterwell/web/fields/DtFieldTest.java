package com.winterwell.web.fields;

import org.junit.Test;

import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

public class DtFieldTest {

	@Test
	public void testDtField() throws Exception {
		DtField dtf = new DtField("dt");
		Dt s10 = dtf.fromString("10 seconds");
		assert new Dt(10, TUnit.SECOND).equals(s10) : s10;
	}
}
