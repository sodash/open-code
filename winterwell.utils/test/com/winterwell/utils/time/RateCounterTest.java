package com.winterwell.utils.time;

import org.junit.Test;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.time.RateCounter;

import com.winterwell.utils.Utils;

/**
 * This is slow!!
 * 
 * @tests {@link RateCounter}
 * @author daniel
 * 
 */
public class RateCounterTest {

	/**
	 * Should output "10 per 10 seconds" after burn-in.
	 * Takes ~ 20 seconds!
	 */
	@Test
	public void testGet() {
		RateCounter ctr = new RateCounter(new Dt(10, TUnit.SECOND));
		for (int i = 0; i < 20; i++) {
			ctr.plus(1);
			Utils.sleep(1000);
			Printer.out(ctr + " " + ctr.oldCount + " " + ctr.count2 + " "
					+ ctr.get2_fraction());
		}
		assert Math.abs(ctr.get() - 10.0) < 0.5;
	}

	/**
	 * Tests how the RateCounter deals with days, both with override on and off.
	 * The override means that RateCounter doesn't multiply up if it's in it's
	 * within the first half of the Dt period.
	 * Takes ~ 20 seconds!
	 */
	@Test
	public void testSetFirstDtOverride() {
		// Normally, this overrides
		RateCounter ctr = new RateCounter(new Dt(1, TUnit.DAY));
		for (int i = 0; i < 20; i++) {
			ctr.plus(1);
			Utils.sleep(1000);
			Printer.out(ctr + " " + ctr.oldCount + " " + ctr.count2 + " "
					+ ctr.get2_fraction());
		}
		assert ctr.get() == 20;
		// Turn the override off
		RateCounter ctr2 = new RateCounter(new Dt(1, TUnit.DAY));
		ctr2.setFirstDtOverride(false);
		for (int i = 0; i < 20; i++) {
			ctr2.plus(1);
			Utils.sleep(1000);
			Printer.out(ctr2 + " " + ctr2.oldCount + " " + ctr2.count2 + " "
					+ ctr2.get2_fraction());
		}
		double intCtr2 = ctr2.get();
		assert (intCtr2 > 86200 && intCtr2 < 86600); // i.e. close to 86400, no
														// of seconds in a day.
	}

	/**
	 * Same as before, but watching the changeover
	 */
	@Test
	public void testSetFirstDtOverride2() {
		RateCounter ctr3 = new RateCounter(new Dt(10, TUnit.SECOND));
		for (int i = 0; i < 20; i++) {
			ctr3.plus(1);
			if (i < 4) {
				// In the first 10 seconds, it's counting up one at a time
				Printer.out("value of i" + i);
				assert (ctr3.get() == i + 1);
			}
			Utils.sleep(1000);
			Printer.out(ctr3 + " " + ctr3.oldCount + " " + ctr3.count2 + " "
					+ ctr3.get2_fraction());
		}
		// at the end, it still gives the right result
		assert Math.abs(ctr3.get() - 10.0) < 0.5;
	}

	@Test
	public void testSetRate() {
		RateCounter ctr = new RateCounter(new Dt(1, TUnit.WEEK));
		Printer.out(ctr.get());
		assert ctr.get() == 0.0 : ctr;
		ctr.setRate(10);
		Printer.out(ctr);
		assert MathUtils.equalish(ctr.get(), 10) : ctr;
	}
}
