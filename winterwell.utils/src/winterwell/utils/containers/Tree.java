package winterwell.utils.containers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import winterwell.utils.StrUtils;
import winterwell.utils.Utils;

/**
 * A simple tree data structure. TODO iteration lets you step through all the
 * values.
 * 
 * @author daniel
 * @testedby {@link TreeTest}
 */
public class Tree<X> implements Iterable<X>, ITree<X> {

	private final List<ITree<X>> children = new ArrayList<ITree<X>>();

	private ITree<X> parent;

	private X x;

	/**
	 * Create a value-less tree root.
	 */
	public Tree() {
	}

	public Tree(ITree<X> parent, X value) {
		setParent(parent);
		setValue(value);
	}

	/**
	 * Create a tree root.
	 * 
	 * @param value
	 */
	public Tree(X value) {
		setValue(value);
	}

	@Override
	@Deprecated
	public void addChild(ITree<X> childNode) {
		assert !children.contains(childNode);
		children.add(childNode);
	}

	/**
	 * TODO should this be a method?
	 * 
	 * @return all tree nodes from this node downwards (inc this node).
	 *         flattened so that a parent comes before its children.
	 */
	public List<Tree<X>> flatten() {
		List<Tree<X>> flat = new ArrayList<Tree<X>>();
		flatten2(flat);
		return flat;
	}

	private void flatten2(List<Tree<X>> flat) {
		flat.add(this);
		for (ITree<X> kid : children) {
			((Tree<X>) kid).flatten2(flat);
		}
	}

	public List<X> flattenToValues() {
		List<Tree<X>> f = flatten();
		List<X> vs = new ArrayList<X>(f.size());
		for (Tree<X> n : f) {
			if (n.getValue() == null) {
				continue;
			}
			vs.add(n.getValue());
		}
		return vs;
	}

	@Override
	public List<? extends ITree<X>> getChildren() {
		return Collections.unmodifiableList(children);
	}

	/**
	 * Convenience for getting the node-values of the child nodes.
	 * 
	 * @return
	 * @see #getChildren()
	 */
	public List<X> getChildValues() {
		ArrayList<X> vs = new ArrayList<X>(children.size());
		for (ITree<X> kid : getChildren()) {
			vs.add(kid.getValue());
		}
		return vs;
	}

	@Override
	public int getDepth() {
		int max = 0;
		for (ITree k : children) {
			max = Math.max(max, k.getDepth());
		}
		return max + 1;
	}

	public List<Tree<X>> getLeaves() {
		// inefficient - a tree walker would be better
		List<Tree<X>> all = flatten();
		List<Tree<X>> leaves = new ArrayList<Tree<X>>();
		for (Tree<X> n : all) {
			if (n.isLeaf()) {
				leaves.add(n);
			}
		}
		return leaves;
	}

	/**
	 * Convenience for drilling down through a tree.
	 * 
	 * @param childIndices
	 *            E.g. 0,1,2 indicates the 2nd child (zero-indexed) of the 1st
	 *            child of the 0th child of this node.
	 * @return
	 */
	public ITree<X> getNode(int... childIndices) {
		ITree<X> node = this;
		for (int i : childIndices) {
			node = node.getChildren().get(i);
		}
		return node;
	}

	/**
	 * Search through this node & sub-nodes for one with the right value
	 * 
	 * @param v
	 *            Can be null
	 * @return node with value equals to v, or null
	 */
	public ITree<X> getNodeByValue(X v) {
		for (ITree<X> n : flatten()) {
			if (Utils.equals(v, n.getValue()))
				return n;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.utils.containers.ITree#getOnlyChild()
	 */
	@Override
	public ITree<X> getOnlyChild() {
		assert children.size() == 1;
		return children.get(0);
	}

	@Override
	public ITree<X> getParent() {
		return parent;
	}

	@Override
	public X getValue() {
		return x;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.utils.containers.ITree#isLeaf()
	 */
	@Override
	public boolean isLeaf() {
		return children.size() == 0;
	}

	@Override
	public Iterator<X> iterator() {
		List<X> flat = flattenToValues();
		return flat.iterator();
	}

	@Override
	@Deprecated
	public void removeChild(ITree<X> childNode) {
		boolean ok = children.remove(childNode);
		assert ok : this;
	}

	@Override
	@SuppressWarnings("deprecation")
	public synchronized void setParent(ITree<X> parent) {
		if (this.parent != null) {
			this.parent.removeChild(this);
		}
		this.parent = parent;
		if (parent != null) {
			parent.addChild(this);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.utils.containers.ITree#setValue(X)
	 */
	@Override
	public void setValue(X value) {
		x = value;
	}

	@Override
	public String toString() {
		return toString2(0, 5);
	}

	/**
	 * @param maxDepth
	 *            max-depth
	 * @return
	 */
	public String toString2(int depth, final int maxDepth) {
		assert depth <= maxDepth;
		assert maxDepth > 0;
		String s = toString3();
		if (isLeaf())
			return s;
		depth++;
		if (maxDepth == depth)
			return s + " ...";
		for (ITree<X> t : children) {
			s += "\n" + StrUtils.repeat('-', depth)
					+ ((Tree<X>) t).toString2(depth, maxDepth);
		}
		return s;
	}

	protected String toString3() {
		return toString4_nodeName() + (x == null ? "" : ":" + x.toString());
	}

	protected String toString4_nodeName() {
		return getClass().getSimpleName();
	}

	

	public static final class DepthFirst<X> implements Iterable<ITree<X>> {

		private final ITree<X> tree;

		public DepthFirst(ITree<? extends X> tree) {
			this.tree = (ITree<X>) tree;		
		}

		@Override
		public Iterator<ITree<X>> iterator() {
			return new DepthFirstIterator<X>(tree);
		}
	}
	
}


final class DepthFirstIterator<X> extends AbstractIterator<ITree<X>> {

	private final ITree<X> tree;

	public DepthFirstIterator(ITree<X> tree) {
		this.tree = tree;
		it = (Iterator) tree.getChildren().iterator();
	}
	
	private boolean rootSent;

	private Iterator<ITree<X>> it;
	private DepthFirstIterator<X> dfit;
	
	
	@Override
	protected ITree<X> next2() throws Exception {
		if ( ! rootSent) {
			rootSent = true;
			return tree;
		}
		if (dfit!=null && dfit.hasNext()) {
			return dfit.next();
		}
		if (it.hasNext()) {
			ITree<X> kid = it.next();
			dfit = new DepthFirstIterator(kid);
			return dfit.next();
		}
		return null;
	}
	
}

