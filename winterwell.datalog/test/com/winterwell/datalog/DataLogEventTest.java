package com.winterwell.datalog;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.utils.Utils;
import com.winterwell.utils.web.SimpleJson;

public class DataLogEventTest {

	@Test
	public void testToJson2_simple() {
		DataLogEvent de = new DataLogEvent("testTag", 2);
		Map<String, ?> json = de.toJson2();
		System.out.println(json);
	}

	@Test
	public void testToJson2_meanVar() {
		ESStorage ess = new ESStorage();
		MeanVar1D mv = new MeanVar1D();
		mv.train1(1.0);
		mv.train1(2.0);
		mv.train1(3.0);
		DataLogEvent de = ess.event4distro("testDistro", mv);
		Map<String, ?> json = de.toJson2();
		// check it has the extra distro info in it
		assert json.containsKey("xtra") : json;
		String xtra = (String) json.get("xtra");
		assert xtra.contains("mean");
		System.out.println(json);
	}
}
