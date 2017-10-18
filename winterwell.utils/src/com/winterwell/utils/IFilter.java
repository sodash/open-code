package com.winterwell.utils;

import com.winterwell.utils.containers.Containers;

/**
 * A filter, because they are useful and it saves having to define this kind of
 * interface in many places.
 * 
 * @see Containers#filter(java.util.Collection, IFilter)
 * @see Containers#first(java.util.Collection, IFilter)
 * @author daniel
 */
public interface IFilter<X> {

	public static <X> IFilter<X> byClass(Class klass) {
		return new ClassFilter(klass);		
	}
	
	/**
	 * Filter returning true if x is not null.
	 */
	public static final IFilter NOT_NULL = new NotNullFilter();

	/**
	 * Filter which returns true for everything.
	 */
	public static final IFilter TRUE = new TrueFilter();
	

	/**
	 * Filter which returns false for everything.
	 */
	public static final IFilter FALSE = new FalseFilter();


	/**
	 * @param x
	 * @return true if x passes the filter, false if it is rejected.
	 *         <p>
	 *         Can be used with
	 *         {@link Containers#filter(java.util.Collection, IFilter)} and
	 *         {@link Containers#first(java.util.Collection, IFilter)}
	 */
	public abstract boolean accept(X x);

}


final class TrueFilter implements IFilter {
	@Override
	public boolean accept(Object x) {
		return true;
	}	
}
final class FalseFilter implements IFilter {
	@Override
	public boolean accept(Object x) {
		return false;
	}	
}


class NotNullFilter implements IFilter {
	@Override
	public boolean accept(Object x) {
		return x!=null;
	}	
}

final class ClassFilter implements IFilter {
	private final Class klass;

	public ClassFilter(Class klass) {
		this.klass = klass;
	}

	@Override
	public boolean accept(Object x) {
		return x!=null && ReflectionUtils.isa(x.getClass(), klass);
	}	
}
