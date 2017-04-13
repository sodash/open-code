package com.winterwell.maths;

/**
 * Interface for things you can do scalar arithmetic with.
 * 
 * @author daniel
 * 
 * @param <In>
 */
public interface IScalarArithmetic {

	/**
	 * @param x
	 * @return a new object which is this + x
	 */
	IScalarArithmetic plus(double x);

	/**
	 * @param x
	 * @return a new object which is this * x
	 */
	IScalarArithmetic times(double x);

}
