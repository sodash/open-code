package com.winterwell.depot.merge;

import com.winterwell.utils.ReflectionUtils;

/**
 * Wrap a diff, so we can recognise that it is a diff.
 * For recursive diffs.
 * @author daniel
 * @param DType the type of the diff -- this may be different from the objects diff-ed.
 */
public final class Diff<DType> {

	public String toString() {
		return "D["+mergerClass.getSimpleName()+" "+diff+"]";
	};
	public final Class<? extends IMerger> mergerClass;
	public final DType diff;

	public Diff(Class<? extends IMerger> mergerClass, DType diff) {
		this.mergerClass = mergerClass;
		assert mergerClass.getName().equals(ReflectionUtils.getCaller().getClassName());
		this.diff = diff;
	}

}
