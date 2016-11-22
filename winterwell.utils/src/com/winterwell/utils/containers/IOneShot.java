/**
 * 
 */
package com.winterwell.utils.containers;

/**
 * Marker interface for thinsg (such as {@link Iterable}s) that should only be used <i>once</i>.
 * 
 * E.g. where we're using the Iterable interface just to get for-each support and you cannot repeatedly iterate.
 * 
 * @author daniel
 */
public interface IOneShot {

}
