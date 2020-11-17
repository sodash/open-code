package com.winterwell.utils.containers;

import java.util.List;

import com.winterwell.utils.Utils;

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
	@Deprecated 	// Use setParent instead
	abstract void addChild(ITree<X> childNode);

	public abstract List<? extends ITree<X>> getChildren();

	/**
	 * @return the depth of this tree, ie. the longest chain from here to a leaf
	 *         node. 1 if this is a leaf node. Will get stuck if some numpty has
	 *         made a loop.
	 */
	public default int getMaxDepthToLeaf() {
		int max = 0;
		for (ITree k : getChildren()) {
			max = Math.max(max, k.getMaxDepthToLeaf());
		}
		return max + 1;
	}

	/**
	 * Convenience for getting the child node if there is one and only one. It
	 * is an error to call this is there are multiple child nodes.
	 */
	public default ITree<X> getOnlyChild() {
		List<? extends ITree<X>> kids = getChildren();
		if (kids.size() != 1) throw new IllegalStateException("Multiple kids "+this);
		return kids.get(0);	
	}

	public abstract ITree<X> getParent();

	public abstract X getValue();

	public default boolean isLeaf() {
		return Utils.isEmpty(getChildren());
	}

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