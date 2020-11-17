package com.winterwell.maths.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.winterwell.maths.datastorage.Index;
import com.winterwell.utils.IFn;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.web.SimpleJson;

/**
 * TODO Use InfoVis http://thejit.org/demos/ to render the graph
 * http://thejit.org/static/v20/Docs/files/Options/Options-Node-js.html
 * 
 * @author daniel
 * @testedby  RenderGraph_InfoVisJsTest}
 */
public class RenderGraph_InfoVisJs {

	protected int height = 400;
	protected int width = 600;

	public IFn<Object,String> nodeNamer = new IFn<Object, String>() {
		@Override
		public String apply(Object node) {
			String name = "" + node;
			if (node instanceof DiNode) {
				Object val = ((DiNode) node).getValue();
				if (val != null) {
					name = "" + val;
				}
			}
			name = StrUtils.ellipsize(name, 24);
			return name;
		}		
	};
	
	public IFn<Object,String> nodeTipper = new IFn<Object, String>() {
		@Override
		public String apply(Object value) {
			return null;
		}		
	};

	
	public void appendDependencies(StringBuilder html) {
		// TODO A local copy of InfoVis!
		html.append("<script src='http://thejit.org/static/v20/Jit/jit-yc.js'></script>\n");
		// JQuery
		html.append("<script src='https://ajax.googleapis.com/ajax/libs/jquery/1.6.3/jquery.min.js'></script>\n");
		// extra WW graph rendering code
		html.append("<script>"
				+ FileUtils.read(RenderGraph_InfoVisJs.class
						.getResourceAsStream("RenderGraph_InfoVis.js"))
				+ "</script>\n");
	}

	void appendJson(IGraph graph, StringBuilder out) throws Exception {
		Collection nodes = graph.getNodes();
		ArrayList g2 = new ArrayList();
		Index nodeIndex = new Index();
		for (Object node : nodes) {			
			Map nodeMap = appendJson2_node(graph, nodeIndex, node);
			g2.add(nodeMap);
		}
		// convert it
		SimpleJson json = new SimpleJson();
		json.appendJson(out, g2);
	}

	Map appendJson2_node(IGraph graph, Index nodeIndex, Object node) throws Exception {
		int i = nodeIndex.indexOfWithAdd(node);
		String id = "n" + i;
		i++;
		
		// links
		ArrayList adjacencies = new ArrayList();
		Collection<IEdge> edges = graph.getEdges(node);
		for (IEdge edge : edges) {
			Object to = edge.getOtherEnd(node);
			if (to == null) {
				continue; // TODO a loop
			}
			ArrayMap data = new ArrayMap();

			// weighted?
			double w = -1;
			if (edge instanceof IEdge.Weighted) {
				w = ((IEdge.Weighted) edge).getWeight();
			} else if (edge instanceof AEdge) {
				Object val = ((AEdge) edge).getValue();
				if (val instanceof Number) {
					w = ((Number) val).doubleValue();					
				}
			}
			if (MathUtils.isProb(w)) {
				data.put("weight", w);
			}
			
			adjacencies.add(new ArrayMap("nodeTo", "n"
					+ nodeIndex.indexOfWithAdd(to), "nodeFrom", id, "data",
					data));
		}

		// name
		String name = nodeNamer.apply(node);		

		// TODO on-hover tip -- how do we pass this to InfoVis??
		String tip = nodeTipper.apply(node);

		Map nodeMap = new ArrayMap(
				"id", id, 
				"name", name,
		// "data", new ArrayMap("$color","$type")
				"adjacencies", adjacencies);
		return nodeMap;
	}

	public void renderToHtml(IGraph graph, StringBuilder html) {
		// Build json
		html.append("<div id='myGraphDiv' class='InfoVisGraph' style='width:"
				+ width + "; height:" + height + ";background:black;'>");
		html.append("<script>var graphJson = ");
		try {
			appendJson(graph, html);
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
		html.append("</script>");
		html.append("</div>");

		html.append("<script>$(function(){createSpaceTree('myGraphDiv',graphJson);});</script>");
	}

	/**
	 * 600x400 by default
	 * 
	 * @param width
	 * @param height
	 */
	public final void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}
}
