package com.winterwell.maths.stats.distributions.d1;

import org.junit.Test;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.XStreamUtils;

public class MeanVar1DTest {

	@Test
	public void testOddToString() {
        String xml = "<com.winterwell.maths.stats.distributions.d1.MeanVar1D><normalised>false</normalised><count>4291</count>"
+"<lossFactor>0.0</lossFactor>       <max>3.778E7</max><mean>346828.71125611744</mean>"
+"<mean2>1.5846530706128696E12</mean2><min>0.0</min></com.winterwell.maths.stats.distributions.d1.MeanVar1D>";
        MeanVar1D rt = XStreamUtils.serialiseFromXml(xml);
        Dt stdDev = new Dt(rt.getStdDev(), TUnit.MILLISECOND);
		Dt max = new Dt(rt.getMax(), TUnit.MILLISECOND);
		Dt mean = new Dt(rt.getMean(), TUnit.MILLISECOND);
		String s = TimeUtils.toString(mean, TUnit.MINUTE)						
				+" ± "+ TimeUtils.toString(stdDev, TUnit.MINUTE);
		
		System.out.println(s);
	}
	
	@Test
	public void testTrain1() {
		MeanVar1D stats = new MeanVar1D();
		for (int i = 0; i < 100; i++) {
			stats.train1(i % 2 == 0 ? 0.25 : 0.75);
		}
		assert stats.getCount() == 100;
		assert MathUtils.equalish(stats.getMean(), 0.5) : stats;
		assert MathUtils.equalish(stats.getVariance(), 0.25 * 0.25) : stats;
	}

}
