package com.winterwell.depot;

/**
 * Use case: to provide an extra layer of defence for objects which
 * have a life-cycle.
 * @see IInit
 * 
 * @author daniel
 *
 */
public interface ILifeCycle {

	/**
	 * This must access a non-transient volatile boolean (or similar threadsafe flag)
	 * @return true if the object is OK
	 */
	boolean isLive();
	
	/**
	 * Mark this object as dead. The object should not then be reused.
	 * <p>
	 * NB: This must set a non-transient volatile boolean (or similar threadsafe flag)
	 */
	void kill();
}
