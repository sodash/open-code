package com.winterwell.maths.chart;

import java.util.Arrays;

import org.junit.Test;

import com.winterwell.maths.vector.XY;

public class GetAxisTest {

	@Test
	public void testGetAxis() {
		XYChart a = new XYChart("a");
		a.setData(Arrays.asList(new XY(1, 100), new XY(2, 150), new XY(3, 175)));
		XYChart b = new XYChart("b");
		b.setData(Arrays.asList(new XY(2, 120), new XY(3, 150), new XY(4, 185)));
		CombinationChart cc = new CombinationChart(a, b);
		NumericalAxis xa = (NumericalAxis) cc.getAxis(0);
		NumericalAxis ya = (NumericalAxis) cc.getAxis(1);
		assert xa.getRange().low == 1 && xa.getRange().high == 4 : xa;
		assert ya.getRange().low == 0 && ya.getRange().high == 200 : ya;
	}

}
