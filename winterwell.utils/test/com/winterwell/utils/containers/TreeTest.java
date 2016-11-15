package winterwell.utils.containers;

import junit.framework.TestCase;
import winterwell.utils.containers.Tree.DepthFirst;

public class TreeTest extends TestCase {

	public void testGetChildren1() {
		Tree<String> parent = new Tree<String>("parent");
		Tree<String> child1 = new Tree<String>("child1");
		child1.setParent(parent);
		assert parent.getChildren().get(0) == child1;
	}

	public void testGetChildren2() {
		Tree<String> parent = new Tree<String>("parent");
		Tree<String> child1 = new Tree<String>(parent, "child1");
		Tree<String> child2 = new Tree<String>(parent, "child2");
		assert parent.getChildren().get(0) == child1;
		assert parent.getChildren().get(1) == child2;
	}

	public void testGetDepth1() {
		Tree<String> tree = new Tree<String>(null);
		assert tree.getDepth() == 1;
	}

	public void testGetDepth2() {
		Tree<String> parent = new Tree<String>("parent");
		Tree<String> child1 = new Tree<String>(parent, "child1");
		assert parent.getDepth() == 2;
	}

	public void testGetDepth3() {
		// two branches hanging from the root node (one leaf(depth=1) and one
		// tree with depth=2)
		Tree<String> root = new Tree<String>("root");
		// level1
		Tree<String> leftLeaf = new Tree<String>(root, "leftLeaf");
		Tree<String> rightNode = new Tree<String>(root, "rightNode");
		// level0
		Tree<String> finalLeaf1 = new Tree<String>(rightNode, "final leaf 1");
		Tree<String> finalLeaf2 = new Tree<String>(rightNode, "final leaf 2");
		assert root.getDepth() == 3;
	}

	public void testGetParent1() {
		Tree<String> parent = new Tree<String>("parent");
		Tree<String> child1 = new Tree<String>("child1");
		child1.setParent(parent);
		assert child1.getParent() == parent;
		assert parent.getParent() == null;
	}

	public void testGetParent2() {
		Tree<String> parent = new Tree<String>("parent");
		Tree<String> child = new Tree<String>(parent, "child");
		Tree<String> grandChild = new Tree<String>(child, "grandChild");
		assert child.getParent() == parent;
		assert grandChild.getParent() == child;
		assert grandChild.getParent().getParent() == parent;
	}

	public void testGetValue1() {
		Tree<String> tree = new Tree<String>();
		tree.setValue("string");
		assert tree.getValue().equals("string");
	}

	public void testGetValue2() {
		Tree<String> tree = new Tree<String>("string");
		assert tree.getValue().equals("string");
	}

	public void testGetValue3() {
		Tree<String> tree = new Tree<String>(null);
		assert tree.getValue() == null;
	}

	public void testToString() {
		{ // two branches hanging from the root node (one leaf(depth=1) and one
			// tree with depth=2)
			Tree<String> root = new Tree<String>("root");
			// level1
			Tree<String> leftLeaf = new Tree<String>(root, "leftLeaf");
			Tree<String> rightNode = new Tree<String>(root, "rightNode");
			// level0
			Tree<String> finalLeaf1 = new Tree<String>(rightNode,
					"final leaf 1");
			Tree<String> finalLeaf2 = new Tree<String>(rightNode,
					"final leaf 2");
			String s = root.toString();
			System.out.println(s);
		}
		{ // three branches, two branches, 1 leaf
			Tree<String> root = new Tree<String>("root");
			// level1
			Tree<String> a1 = new Tree<String>(root, "a1");
			Tree<String> a2 = new Tree<String>(root, "a2");
			Tree<String> a3 = new Tree<String>(root, "a3");
			// level2
			Tree<String> b11 = new Tree<String>(a1, "b11");
			Tree<String> b12 = new Tree<String>(a1, "b12");
			Tree<String> b21 = new Tree<String>(a2, "b21");
			Tree<String> b22 = new Tree<String>(a2, "b22");
			// leaves
			Tree<String> c111 = new Tree<String>(b11, "c111");
			Tree<String> c121 = new Tree<String>(b12, "c121");
			Tree<String> c211 = new Tree<String>(b21, "c211");
			String s = root.toString();
			System.out.println(s);
			// cut short -- just root & first level
			String s2 = root.toString2(0, 2);
			System.out.println(s2);
		}
	}

	public void testToString1() {
		Tree<String> tree = new Tree<String>(null);
		assert tree.toString().equals("Tree");
	}

	public void testToString2() {
		Tree<String> tree = new Tree<String>("string");
		assert tree.toString().equals("Tree:string");
	}
	

	public void testDF() {
		Tree<String> parent = new Tree<String>("parent");
		Tree<String> child1 = new Tree<String>(parent, "child1");
		Tree<String> child2 = new Tree<String>(parent, "child2");
		Tree<String> schild1 = new Tree<String>(child1, "grand-child1");
		Tree<String> schild2 = new Tree<String>(child1, "grand-child2");
		Tree<String> schild3 = new Tree<String>(child1, "grand-child3");
		DepthFirst<String> it = new Tree.DepthFirst(parent);
		for (ITree<String> iTree : it) {
			System.out.println(iTree.getValue());
		}
	}

}
