package com.winterwell.depot;

/**
 * 
 * @author daniel
 *         <p>
 *         <b>Copyright & license</b>: (c) Winterwell Associates Ltd, all rights
 *         reserved. This class is NOT formally a part of the winterwell.utils
 *         library. In particular, licenses for the winterwell.utils library do
 *         not apply to this file.
 */
public interface IDescCache {

	/**
	 * Mechanism for getting at descriptions, e.g to do dependency stuff.<br>
	 * 
	 * 1. If there is a sharedDescription from {@link #bind(Object)}, this is
	 * returned.<br>
	 * 2. Failing which, if the artifact implements IHasDesc this will be used.
	 * 
	 * @param x
	 * @return description or null
	 */
	<X> Desc<X> getDescription(X artifact);

	/**
	 * 
	 * @param artifact
	 * @param desc
	 * @return true if an edit to bindings is made. false if no edit is made, or no binding happens.
	 */
	<X> boolean bind(X artifact, Desc<X> desc);

	int getSharedDescriptionsSize();

	/**
	 * Remove cache-bindings and the inside-desc binding
	 * 
	 * @param desc
	 *            Can be null
	 * @param artifact
	 *            Can be null
	 */
	<X> void unbind(X artifact, Desc<X> desc);

	/**
	 * @param desc
	 * @return object bound to desc, or null
	 * 
	 */
	Object getArtifact(Desc desc);

}
