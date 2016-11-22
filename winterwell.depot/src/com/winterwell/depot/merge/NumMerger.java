package com.winterwell.depot.merge;


public class NumMerger extends AMerger<Number> {

	@Override
	public Diff diff(Number before, Number after) {
		if (before.equals(after)) return null;
		Object dx;
		// subtract numbers
		if (before instanceof Integer) {
			dx = (Integer)after - (Integer)before;
		} else {
			dx = ((Number)after).doubleValue() - ((Number)before).doubleValue();
		}
		return new Diff(NumMerger.class, dx);
	}

	@Override
	public Number applyDiff(Number a, Diff diff) {
		if (a==null) {
			// treat null as zero
			return (Number) diff.diff;
		}
		// add numbers
		if (a instanceof Integer) {
			int iv = (Integer) a;
			return iv + (Integer) (diff.diff);
		}
		return a.doubleValue() + ((Number)diff.diff).doubleValue();
	}

	@Override
	public Number stripDiffs(Number v) {
		return v;
	}

}
