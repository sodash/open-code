package com.winterwell.depot.merge;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.winterwell.utils.TodoException;

public class NumMerger extends AMerger<Number> {

	@Override
	public Diff diff(Number before, Number after) {
		if (before==null) return new Diff(NumMerger.class, after);
		if (before.equals(after)) return null;
		// subtract numbers		
		Object dx = after.doubleValue() - before.doubleValue();		
		return new Diff(NumMerger.class, dx);
	}

	@Override
	public Number applyDiff(Number a, Diff diff) {
		Number dx = (Number) diff.diff;
		if (a==null) {
			// treat null as zero
			return dx;
		}
		// TODO
		if (a instanceof BigInteger || a instanceof BigDecimal) {
			throw new TodoException();
		}
		// add numbers
		if (a instanceof Integer) {
			int iv = a.intValue(); // assumes we don't do 1 (int) + 0.5 (double) !!!
			return iv + dx.intValue();
		}
		return a.doubleValue() + dx.doubleValue();
	}

	@Override
	public Number stripDiffs(Number v) {
		return v;
	}

}
