package com.winterwell.utils.containers;

import java.util.List;

/**
 * A generic tree data structure. See the default implementation: {@link Tree}.
 * 
 * @author Daniel
 */
public interface ITree<X> {

	/**
	 * Add a child node.
	 * 
	 * @param childNode
	 *            This must not have a parent. Use setParent(null) if necessary
	 *            to first detach the child.
	 * @Deprecated Use {@link #setParent(ITree)} instead. This method should
	 *             only be called by setParent()!
	 */
	@Deprecated
	// Use setParent instead
	abstract void addChild(ITree<X> childNode);

	public abstract List<? extends ITree<X>> getChildren();

	/**
	 * @return the depth of this tree, ie. the longest chain from here to a leaf
	 *         node. 1 if this is a leaf node. Will get stuck if some numpty has
	 *         made a loop.
	 */
	int getDepth();

	/**
	 * Convenience for getting the child node if there is one and only one. It
	 * is an error to call this is there are multiple child nodes.
	 */
	public abstract ITree<X> getOnlyChild();

	public abstract ITree<X> getParent();

	public abstract X getValue();

	public abstract boolean isLeaf();

	/**
	 * Remove a child node.
	 * 
	 * @param childNode
	 *            This must be a child node.
	 * @Deprecated Use {@link #setParent(ITree)} with null instead. This method
	 *             should only be called by setParent().
	 */
	@Deprecated
	abstract void removeChild(ITree<X> childNode);

	/**
	 * Set the parent node for this node. Remove the previous parent if
	 * necessary. Manage the child links.
	 * 
	 * @param parent
	 */
	public abstract void setParent(ITree<X> parent);

	public abstract void setValue(X value);

}