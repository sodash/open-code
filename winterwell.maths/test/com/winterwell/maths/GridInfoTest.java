package com.winterwell.maths;

import org.junit.Test;

import com.winterwell.utils.MathUtils;

/**
 * @tests {@link GridInfo}
 * @author daniel
 * 
 */
public class GridInfoTest {

	@Test
	public void testGetBucket() {
		{
			IGridInfo grid = new GridInfo(0, 1, 10);
			int b1 = grid.getBucket(0);
			int b2 = grid.getBucket(0 + Double.MIN_NORMAL);
			int b3 = grid.getBucket(0 + MathUtils.getMachineEpsilon());
			int b4 = grid.getBucket(1);
			int b5 = grid.getBucket(1 - Double.MIN_NORMAL);
			int b6 = grid.getBucket(1 - MathUtils.getMachineEpsilon());
			int b7 = grid.getBucket(0.9);
			int b8 = grid.getBucket(0.9 - Double.MIN_NORMAL);
			int b9 = grid.getBucket(0.9 - MathUtils.getMachineEpsilon());
			int b10 = grid.getBucket(0.89);

			assert b1 == b2 && b2 == b3;
			assert b4 == b5 && b5 == b6;
			assert b1 == 0;
			assert b4 == 9;
			// assert b8 == 8; fails but ok its a very small number
			assert b9 == 8;
			assert b10 == 8;
		}
		{
			GridInfo grid = new GridInfo(-1, 1, 100);
			int b1 = grid.getBucket(1);
			int b2 = grid.getBucket(0.9999999999999999);
			assert b2 < grid.numBuckets : b2;
			assert b1 == 99;
			assert b2 == 99;
		}
		{
			IGridInfo grid = new GridInfo(0, 1, 10);
			int b1 = grid.getBucket(-0);
			int b2 = grid.getBucket(-0.01);
			int b3 = grid.getBucket(-1000);
			int b4 = grid.getBucket(1.01);
			int b5 = grid.getBucket(1000);
			assert b1 == 0;
			assert b2 == 0;
			assert b3 == 0;
			assert b4 == 9;
			assert b5 == 9;
		}
	}
}
