package com.winterwell.depot.merge;

import java.util.Objects;

import com.winterwell.utils.Printer;
import com.winterwell.utils.ReflectionUtils;

/**
 * Wrap a diff, so we can recognise that it is a diff.
 * For recursive diffs.
 * @author daniel
 * @param DType the type of the diff -- this may be different from the objects diff-ed.
 */
public final class Diff<DType> {

	public String toString() {
		return "D["+mergerClass.getSimpleName()+" "+Printer.str(diff)+"]";
	};
	public final Class<? extends IMerger> mergerClass;
	public final DType diff;

	public Diff(Class<? extends IMerger> mergerClass, DType diff) {
		this.mergerClass = mergerClass;
		assert mergerClass==NullMerger.class || mergerClass.getName().equals(ReflectionUtils.getCaller().getClassName());
		this.diff = diff;
	}

	@Override
	public int hashCode() {
		return Objects.hash(diff, mergerClass);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Diff other = (Diff) obj;
		return Objects.equals(diff, other.diff) && Objects.equals(mergerClass, other.mergerClass);
	}

}
