package com.winterwell.maths.datastorage;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.vector.XY;
import com.winterwell.utils.BestOne;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.TopNList;

/**
 * An efficient 2D point map.
 * 
 * @author daniel
 * Adapted from the quad tree at http://algs4.cs.princeton.edu/92search/QuadTree.java.html
 *
 * @param <Value> the values stored at leaf nodes.
 */
public class QuadTree<Value>  {
    private static final int SPLIT_SIZE = 500;
	private Node root = new Node(-1000,-1000,1000,1000);

	public void setBounds(Rectangle bounds) {
		this.root = new Node(bounds.getMinX(),bounds.getMinY(),bounds.getMaxX(),bounds.getMaxY());
	}
	
    // helper node data type
    private static class Node {
        double x, y;              // x- and y- coordinates
        double x2, y2;              // x- and y- coordinates
        Node NW, NE, SE, SW;   // four subtrees
        Map<XY, Object> values = new HashMap();           // associated data

        Node(double x, double y, double x2, double y2) {
            this.x = x;
            this.y = y;
            this.x2 = x2;
            this.y2 = y2;
            assert x2-x > 0 : this;
            assert y2-y > 0 : this;
        }

        /**
         * 
         * @param xy
         * @return the smallest node holding xy
         */
		Node choose(XY xy) {
			assert isIn(xy) : this+" v "+xy;
//	    	assert h.x1 >= xy.x && h.x2 <= xy.x;
//	    	assert h.y1 >= xy.y && h.y2 <= xy.y;
	        if (NE == null) return this;	        
        	if (NW.x2 > xy.x) {	// west
        		assert NW.x <= xy.x : this+" "+NW+" "+xy;
        		return NW.y2 > xy.y? NW.choose(xy) : SW.choose(xy);
        	} else { // east
        		assert NE.y2 == SE.y;
        		return NE.y2 > xy.y? NE.choose(xy) : SE.choose(xy);
        	}	        
		}

		void split() {
			double mx = (x+x2)/2;
			double my = (y+y2)/2;
//			// handle splitting infinity
//			if (Double.isNaN(mx)) mx = 0;
			assert ! Double.isNaN(mx);
			assert ! Double.isNaN(my);
//			if (Double.isNaN(my)) my = 0;
			NW = new Node(x, y, mx, my);
			NE = new Node(mx, y, x2, my);
			SW = new Node(x, my, mx, y2);
			SE = new Node(mx, my, x2, y2);
			
			assert NW.x2 == NE.x : this;
			assert NW.x == x : this;
			assert NE.x2 == x2 : this;
			
			Set<Entry<XY, Object>> es = values.entrySet();
			for (Entry<XY, Object> entry : es) {
				Node node = choose(entry.getKey());
				node.values.put(entry.getKey(), entry.getValue());
			}
			values = null;
		}

		boolean isIn(XY xy) {
			return xy.x >= x && xy.x <= x2
					&& xy.y >= y && xy.y <= y2;
		}

		/**
		 * @return null if mixed
		 */
		Object value() {
			if (values == null) return null;
			HashSet vs = new HashSet(values.values());			
			return vs.size()==1? 
					vs.iterator().next() 
					: null;
		}
		
		@Override
	    public String toString() {
	    	return "Node["+x+" "+x2+", "+y+" "+y2+":"+value()+"]";
	    }
		
		Entry<XY, Object> closest(XY xy) {
			BestOne<Entry<XY, Object>> b = new BestOne();
			for(Entry<XY, Object> e : values.entrySet()) {
				double d = DataUtils.dist(e.getKey(), xy);
				b.maybeSet(e, -d);
			}
			return b.getBest();
		}
		
		List<Object> closest(XY xy, int n) {
			TopNList<Object> b = new TopNList(n);
			for(Entry<XY, Object> e : values.entrySet()) {
				double d = DataUtils.dist(e.getKey(), xy);
				b.maybeAdd(e.getValue(), -d);
			}
			return b;
		}

		/**
		 * Reduce the depth of the quadtree by pruning branches with identical
		 * values. Calls itself recursively on its child nodes.
		 * @return the single value for this Node, or null if mixed
		 */
		Object simplify() {
			XY m = new XY((x+x2)*0.5, (y+y2)*0.5);
			if (NE!=null) {
				ArraySet v = new ArraySet();
				for(Node n : new Node[]{NE,NW,SE,SW}) {
					Object s = n.simplify();
					v.addAll(n.values());
				}
				if (v.size() < 2) {
					NE = null; SW = null; NW = null; SE = null;
					Object vs = v.size()==0? null : v.get(0);
					values = vs==null? Collections.EMPTY_MAP : new ArrayMap(m, vs);
					return vs;
				}
				return null;
			}
			
			Object v = value();
			if (v != null) {
				values = new ArrayMap(m, v);
				return v;
			}
			
			return null;
		}

		private Collection values() {
			if (values!=null) return values.values();
			HashSet vs = new HashSet();
			vs.addAll(NE.values());
			vs.addAll(NW.values());
			vs.addAll(SE.values());
			vs.addAll(SW.values());
			return vs;
		}
    }


  /***********************************************************************
    *  Insert (x, y) into appropriate quadrant
    ***********************************************************************/
    public void insert(double x, double y, Value value) {
        insert(root, new XY(x, y), value);
    }

    /**
     * Reduce the number of nodes: if all the nodes in a branch have the same value,
     * then prune the branch.
     * Call this only after all your data has been put in!
     */
    public void simplify() {
    	root.simplify();
    }
    
    private Node insert(Node h, XY xy, Value value) {
    	h = h.choose(xy);
    	assert h.values != null;
        if (h.values.size() < SPLIT_SIZE) {
        	assert h.isIn(xy) : h+" vs "+xy;
        	h.values.put(xy, value);
        	return h;
        }
        // insert anyway if one value
        if (value.equals(h.value())) {
        	assert h.isIn(xy) : h+" vs "+xy;
        	h.values.put(xy, value);
        	return h;
        }
        // spit the node
        h.split();
        Node h2 = h.choose(xy);
        assert h2 != h : xy;
        return insert(h2, xy, value);
    }

    /**
     * @param x
     * @param y
     * @return the closest value to (x,y)
     */
    public Value get(double x, double y) {
    	XY xy = new XY(x,y);
    	if (root==null) return null;
    	Node node = root.choose(xy);
    	Map.Entry c = node.closest(xy);    	
    	return c==null? null : (Value) c.getValue();
    }

    /**
     * @param x
     * @param y
     * @return the closest value to (x,y)
     */
    public List<Value> get(double x, double y, int n) {
    	XY xy = new XY(x,y);
    	if (root==null) return null;
    	Node node = root.choose(xy);
    	List c = node.closest(xy, n);    	
    	return c;
    }
    



   /*************************************************************************
    *  test client
    *************************************************************************/
    public static void main(String[] args) {
        int M = 10; //Integer.parseInt(args[0]);   // queries
        int N = 10000; //Integer.parseInt(args[1]);   // points

        QuadTree<String> st = new QuadTree<String>();

        // insert N random points in the unit square
        for (int i = 0; i < N; i++) {
            double x = 100 * Math.random();
            double y = 100 * Math.random();
            // System.out.println("(" + x + ", " + y + ")");
            int band = (int)(x/50);
            st.insert(x, y, "P" + (i%2)+"_"+band);
        }
        System.out.println(st.size());
        st.simplify();
        System.out.println("Done preprocessing " + N + " points");
        System.out.println(st.size());
        
        // do some range searches
        for (int i = 0; i < M; i++) {
            double x = 100 * Math.random();
            double y = 100 * Math.random();            
            System.out.println(x+","+y+" : "+st.get(x, y));
        }
    }

    /**
     * @return number of point->value mappings.
     */
	public int size() {
		return size2(root);
	}

	private int size2(Node node) {
		if (node.NE==null) return node.values.size();
		return size2(node.NE) + size2(node.NW) + size2(node.SE) + size2(node.SW);
	}

}
