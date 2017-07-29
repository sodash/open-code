package com.winterwell.web.fields;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import com.winterwell.utils.Constant;
import com.winterwell.utils.Utils;
import com.winterwell.utils.threads.ICallable;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

/**
 * TODO Like DateField, adding relative-time answers such as "last week"
 * 
 * Enter dates (and times). TODO a (popup) javascript calendar widget. TODO
 * handle time zones configurably
 * 
 * @author daniel
 * @testedby {@link TimeFieldTest}
 */
public class TimeField extends AField<ICallable<Time>> {

	private static final long serialVersionUID = 1L;

	public TimeField(String name) {
		super(name, "text");
		// The html5 "date" type is not really supported yet.
		// What it does do on Firefox is block non-numerical text entry, which
		// we want to support
		cssClass = "DateField";
	}

	/**
	 * First tries the "canonical" "HH:mm dd/MM/yyyy", then the other formats,
	 * finally {@link TimeUtils#parseExperimental(String)}.
	 */
	@Override
	public ICallable<Time> fromString(String v) {
		AtomicBoolean isRel = new AtomicBoolean();
		// HACK fixing bugs elsewhere really. Handle "5+days+ago" from a query
		v = v.replace('+', ' ');		
		
		Time t = DateField.parse(v, isRel);
		if ( ! isRel.get()) {
			return new Constant(t);
		}
		// ??Relative but future (eg. "tomorrow")... make it absolute? No -- let the caller make that decision.		
		// e.g. "1 day ago" or "tomorrow"
		return new RelTime(v); 
	}

	@Override
	public String toString(ICallable<Time> _time) {
		// relative?
		if (_time instanceof RelTime) {
			return ((RelTime) _time).v;
		}
		try {
			Time time = _time.call();
			return toString(time);
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	public String toString(Time time) {
		return DateField.toString2(time);
	}

	@Override
	public Class<ICallable<Time>> getValueClass() {
		return (Class) Callable.class;
	}
}


final class RelTime implements ICallable<Time>, Serializable {
	private static final long serialVersionUID = 1L;
	final String v;
	
	public RelTime(String v) {
		this.v = v;
	}
	
	@Override
	public String toString() {
		return "RelTime["+v+"]";
	}

	@Override
	public Time call() {
		return TimeUtils.parseExperimental(v);
	}
}
