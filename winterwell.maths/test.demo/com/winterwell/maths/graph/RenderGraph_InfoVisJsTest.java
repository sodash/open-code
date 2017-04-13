package com.winterwell.maths.graph;

import org.junit.Test;

import com.winterwell.utils.web.WebUtils;

public class RenderGraph_InfoVisJsTest {

	@Test
	public void testTree() {
		DiGraph<String> g = new DiGraph<String>();
		DiNode<String> a = g.addNode("A");
		DiNode<String> b = g.addNode("B");
		DiNode<String> c = g.addNode("C");
		DiNode<String> d = g.addNode("D");
		DiEdge<String> ab = g.addEdge(a, b, null); // will have weight zero!
		DiEdge<String> bc = g.addEdge(b, c, null);
		DiEdge<String> bd = g.addEdge(b, d, null);
//		ab.setValue(1.0);
		bc.setValue(0.2);
		bd.setValue(0.8);

		RenderGraph_InfoVisJs render = new RenderGraph_InfoVisJs();

		StringBuilder sb = new StringBuilder();
		sb.append("<html><head>\n");
		render.appendDependencies(sb);
		sb.append("</head><body>\n");
		render.renderToHtml(g, sb);
		sb.append("</body></html>");

		WebUtils.display(sb.toString());
	}
	
	@Test
	public void testDAG() {
		DiGraph<String> g = new DiGraph<String>();
		DiNode<String> a = g.addNode("A");
		DiNode<String> b = g.addNode("B");
		DiNode<String> c = g.addNode("C");
		DiNode<String> d = g.addNode("D");
		DiEdge<String> ab = g.addEdge(a, b, null);
		DiEdge<String> ac = g.addEdge(a, c, null);
		DiEdge<String> bd = g.addEdge(b, d, null);
		DiEdge<String> cd = g.addEdge(c, d, null);
//		ab.setValue(1.0);
		ab.setValue(0.2);
		ac.setValue(0.8);
		bd.setValue(0.2);
		cd.setValue(0.8);

		RenderGraph_InfoVisJs render = new RenderGraph_InfoVisJs();

		StringBuilder sb = new StringBuilder();
		sb.append("<html><head>\n");
		render.appendDependencies(sb);
		sb.append("</head><body><h3>DAG</h3>\n");
		render.renderToHtml(g, sb);
		sb.append("</body></html>");

		WebUtils.display(sb.toString());
	}
	
//	@Test fails with TreeMap
	public void testDAG2() {
		DiGraph<String> g = new DiGraph<String>();
		DiNode<String> a = g.addNode("A");
		DiNode<String> b = g.addNode("B");
		DiNode<String> c = g.addNode("C");
		DiNode<String> d = g.addNode("D");
		DiEdge<String> ab = g.addEdge(a, b, null);
		DiEdge<String> ac = g.addEdge(a, c, null);
		DiEdge<String> bd = g.addEdge(b, d, null);
		DiEdge<String> cb = g.addEdge(c, b, null);
//		ab.setValue(1.0);
		ab.setValue(0.2);
		ac.setValue(0.8);
		bd.setValue(0.2);
		cb.setValue(0.8);

		RenderGraph_InfoVisJs render = new RenderGraph_InfoVisJs();

		StringBuilder sb = new StringBuilder();
		sb.append("<html><head>\n");
		render.appendDependencies(sb);
		sb.append("</head><body><h3>DAG 2: with a link within the level (c-&gt;b)!</h3>\n");
		render.renderToHtml(g, sb);
		sb.append("</body></html>");

		WebUtils.display(sb.toString());
	}

	
//	@Test Fails with TreeMap
	public void testDAG3() {
		DiGraph<String> g = new DiGraph<String>();
		DiNode<String> a = g.addNode("A");
		DiNode<String> b1 = g.addNode("B1");
		DiNode<String> b2 = g.addNode("B2");
		DiNode<String> c = g.addNode("C");
		DiNode<String> d = g.addNode("D");
		DiEdge<String> ab = g.addEdge(a, b1, null);
		DiEdge<String> ac = g.addEdge(a, c, null);
		DiEdge<String> bb = g.addEdge(b1, b2, null);
		DiEdge<String> b2d = g.addEdge(b2, d, null);
		DiEdge<String> cd = g.addEdge(c, d, null);

		RenderGraph_InfoVisJs render = new RenderGraph_InfoVisJs();

		StringBuilder sb = new StringBuilder();
		sb.append("<html><head>\n");
		render.appendDependencies(sb);
		sb.append("</head><body><h3>DAG 3: with different chain lengths to D!</h3>\n");
		render.renderToHtml(g, sb);
		sb.append("</body></html>");

		WebUtils.display(sb.toString());
	}
}
