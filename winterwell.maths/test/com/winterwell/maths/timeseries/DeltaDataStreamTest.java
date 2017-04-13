package com.winterwell.maths.timeseries;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.time.Time;

public class DeltaDataStreamTest {
	
	private static final double EPS = MathUtils.getMachineEpsilon()*2;
	
	private ListDataStream exampleElementList() {
		double[] data = {
				0, 3,
				1, 23.5,
				2, -14,
				3, 0,
				4, 8.1,
				7.5, 12,
				20, 1,
				21, 2,
				22, 3
			};
		List<Datum> data_list = new ArrayList<Datum>();
		for(int i=0; i<data.length; i+=2) {
			data_list.add(new Datum(new Time((long)1000*data[i]),data[i+1],null));
		}
		return new ListDataStream(data_list);		
	}
	
	@Test
	public void testDeltaDataStream() {
		long[] _expected_time = {
			0L, 1000L, 2000L, 3000L, 4000L, 7500L, 20000L, 21000L, 22000L
		};
		double[] _expected = {
			0.0, 20.5, -37.5, 14.0, 8.1, 3.9, -11.0, 1.0, 1.0
		};
		IDataStream example = exampleElementList();
		IDataStream test_out = new DeltaDataStream(example);
		AbstractIterator<Datum> iter = test_out.iterator();
		for (Integer idx=0; idx<_expected.length; idx++) {
			Datum result = iter.next();
			assertEquals(_expected_time[idx],result.time.getTime());
			assertEquals(_expected[idx],result.get(0),EPS);
		}
	}
}
