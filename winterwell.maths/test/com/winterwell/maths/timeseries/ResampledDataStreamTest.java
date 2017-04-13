package com.winterwell.maths.timeseries;

import java.util.ArrayList;

import com.winterwell.maths.stats.distributions.d1.Gaussian1D;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.time.TUnit;

import junit.framework.TestCase;

public class ResampledDataStreamTest extends TestCase {

	public void testFractions() {
		RandomDataStream a = new RandomDataStream(new Gaussian1D(0, 1), null,
				TUnit.MINUTE.getDt());
		a.setLabel("A");
		RandomDataStream b = new RandomDataStream(new Gaussian1D(2, 1), null,
				TUnit.MINUTE.getDt());
		b.setLabel("B");
		IDataStream mixed = new MixedDataStream(a, b);
		ResampledDataStream boosted = new ResampledDataStream(mixed,
				new ArrayMap("A", 1.5, "B", 0.5));
		ArrayList<Datum> data = DataUtils.sample(boosted, 4000);
		int as = 0, bs = 0, wtf = 0;
		for (Datum datum : data) {
			if (datum.getLabel() == "A") {
				as++;
			} else if (datum.getLabel() == "B") {
				bs++;
			} else {
				wtf++;
			}
		}
		assert wtf == 0 : wtf;
		assert Math.abs(as - 3000) < 100 : as;
		assert Math.abs(bs - 1000) < 100 : bs;
	}

	public void testFractions2() {
		// high/low fractions to check rounding
		RandomDataStream a = new RandomDataStream(new Gaussian1D(0, 1), null,
				TUnit.MINUTE.getDt());
		a.setLabel("A");
		RandomDataStream b = new RandomDataStream(new Gaussian1D(2, 1), null,
				TUnit.MINUTE.getDt());
		b.setLabel("B");
		IDataStream mixed = new MixedDataStream(a, b);
		ResampledDataStream boosted = new ResampledDataStream(mixed,
				new ArrayMap("A", 1.9, "B", 0.1));
		ArrayList<Datum> data = DataUtils.sample(boosted, 2000);
		int as = 0, bs = 0, wtf = 0;
		for (Datum datum : data) {
			if (datum.getLabel() == "A") {
				as++;
			} else if (datum.getLabel() == "B") {
				bs++;
			} else {
				wtf++;
			}
		}
		assert wtf == 0 : wtf;
		assert Math.abs(as - 1900) < 100 : as;
		assert Math.abs(bs - 100) < 100 : bs;

	}

	public void testRemoveClass() {
		RandomDataStream a = new RandomDataStream(new Gaussian1D(0, 1), null,
				TUnit.MINUTE.getDt());
		a.setLabel("A");
		RandomDataStream b = new RandomDataStream(new Gaussian1D(2, 1), null,
				TUnit.MINUTE.getDt());
		IDataStream mixed = new MixedDataStream(a, b);
		ResampledDataStream boosted = new ResampledDataStream(mixed,
				new ArrayMap("A", 2, null, 0));
		ArrayList<Datum> data = DataUtils.sample(boosted, 30);
		int as = 0, bs = 0, wtf = 0;
		for (Datum datum : data) {
			if (datum.getLabel() == "A") {
				as++;
			} else if (datum.getLabel() == null) {
				bs++;
			} else {
				wtf++;
			}
		}
		assert wtf == 0 : wtf;
		assert as == 30 : as;
		assert bs == 0 : bs;

	}

	public void testSimple() {
		RandomDataStream a = new RandomDataStream(new Gaussian1D(0, 1), null,
				TUnit.MINUTE.getDt());
		a.setLabel("A");
		RandomDataStream b = new RandomDataStream(new Gaussian1D(2, 1), null,
				TUnit.MINUTE.getDt());
		b.setLabel("B");
		IDataStream mixed = new MixedDataStream(a, b);
		ResampledDataStream boosted = new ResampledDataStream(mixed,
				new ArrayMap("A", 2));
		ArrayList<Datum> data = DataUtils.sample(boosted, 30);
		int as = 0, bs = 0, wtf = 0;
		for (Datum datum : data) {
			if (datum.isLabelled("A")) {
				as++;
			} else if (datum.isLabelled("B")) {
				bs++;
			} else {
				wtf++;
			}
		}
		assert wtf == 0 : wtf;
		assert as == 20 : as + " vs " + bs;
		assert bs == 10 : as + " vs " + bs;

	}
}
