package com.winterwell.depot;

/**
 * This gormlessly named interface is for objects which have transient wiring or
 * resources that should be setup when they are created/loaded. init() should be
 * called:
 * 
 * - By constructors - On de-serialisation
 * 
 * @author daniel
 * 
 */
public interface IInit {

	/**
	 * Setup transient resources (which need to be setup afresh if the object is
	 * serialised & de-serialised).
	 * <p>
	 * It should be safe to call this multiple times. The 2nd and subsequent
	 * calls should have no effect.
	 * <p>
	 * This should usually be called in the final sub-class constructor, or if
	 * this is not possible ('cos we wish to allow further sub-classing), by
	 * anyone creating objects. The constructor javadoc should make it clear
	 * whether or not init() is called.
	 * <p>
	 * Best practice: Do not rely on this being called externally. Have a
	 * transient boolean initFlag, & use this to call init() in methods.
	 * @return this
	 */
	void init();
}
