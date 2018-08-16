package com.winterwell.gson.internal.bind;

import java.lang.reflect.Field;

/**
 * ??merge with LateBinding??
 * @author daniel
 *
 */
public final class LBRow {
	
	public final LateBinding lb;
	public final int index;
	public final Field f;
	public final Object obj;

	public LBRow(Object obj, Field f, int index, LateBinding lb) {
		this.obj = obj; this.f=f; this.index=index; this.lb=lb;
	}
	
}