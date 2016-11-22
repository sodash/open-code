/**
 * 
 */
package com.winterwell.utils;

/**
 * A generic function. For when you can't be arsed creating a custom interface.
 * 
 * @author daniel
 */
public interface IFn<In, Out> {

	Out apply(In value);

}
