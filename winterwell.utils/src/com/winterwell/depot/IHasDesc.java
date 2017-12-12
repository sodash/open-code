package com.winterwell.depot;


/**
 * For artifacts which can create a Desc description of themselves.
 * <p>
 * NB: Descs can provide a convenient way to define equals & hashcode, since
 * objects with equals() Descs are probably equivalent.
 * 
 * @author daniel
 *         <p>
 *         <b>Copyright & license</b>: (c) Winterwell Associates Ltd, all rights
 *         reserved. This class is NOT formally a part of the com.winterwell.utils
 *         library. In particular, licenses for the com.winterwell.utils library do
 *         not apply to this file.
 */
public interface IHasDesc {

	/**
	 * @return a description for this artifact. This must not use
	 *         {@link Desc#getDescription(Object)} as that would generate an
	 *         infinite loop.
	 *         <p>
	 *         Should this bind the object to it's description? No - keep
	 *         binding as something done by depot, or explicitly.
	 */
	Desc getDesc();

}
