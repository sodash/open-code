package com.winterwell.maths.graph;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.winterwell.utils.Printer;
import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.web.WebUtils;

public class DotPrinterTest {

	@Test
	
	public void testCreatePng() throws IOException {
		DiGraph g = new DiGraph();
		DiNode a = g.addNode("A");
		DiNode b = g.addNode("B");
		g.addEdge(a, b, null);
		DotPrinter dp = new DotPrinter(g);
		
		File dotFile = File.createTempFile("dot", ".dot");
		FileUtils.write(dotFile, dp.out());
		Printer.out(dotFile);

		File imgFile = File.createTempFile("dot", ".png");
		DotPrinter.create(dotFile, "png", imgFile);
		Printer.out(imgFile);

		GuiUtils.popupAndBlock(GuiUtils.load(imgFile));
	}
	
	
	@Test	
	public void testSoGive() throws IOException {
		DiGraph g = new DiGraph();
		DiNode a = g.addNode("Data Collection");
		DiNode b = g.addNode("Economic Modelling");
		DiNode c = g.addNode("Donation System");
		DiNode d = g.addNode("Giving History");
		DiNode e = g.addNode("User App");
		g.addEdge(a, b, null);
		g.addEdge(b, e, null);
		g.addEdge(d, e, null);
		g.addEdge(e, c, null);
		DotPrinter dp = new DotPrinter(g);
		
		File dotFile = File.createTempFile("dot", ".dot");
		FileUtils.write(dotFile, dp.out());
		Printer.out(dotFile);

		File imgFile = File.createTempFile("dot", ".png");
		DotPrinter.create(dotFile, "png", imgFile);
		Printer.out(imgFile);

		GuiUtils.popupAndBlock(GuiUtils.load(imgFile));
	}
	
	@Test
	
	public void testCreateSvg() throws IOException {
		DiGraph g = new DiGraph();
		DiNode a = g.addNode("A");
		DiNode b = g.addNode("B");
		g.addEdge(a, b, null);
		DotPrinter dp = new DotPrinter(g);
		
		File dotFile = File.createTempFile("dot", ".dot");
		FileUtils.write(dotFile, dp.out());
		Printer.out(dotFile);

		File imgFile = File.createTempFile("dot", ".svg");
		DotPrinter.create(dotFile, "svg", imgFile);
		Printer.out(imgFile);
		WebUtils.display(imgFile);
	}
	
	@Test
	
	public void testWeighted() throws IOException {
		DiGraph g = new DiGraph();
		DiNode a = g.addNode("A");
		DiNode b = g.addNode("B");
		DiNode c = g.addNode("C");
		g.addEdge(a, b, null);
		DiEdge ac = g.addEdge(a, c, null);
		ac.setWeight(5);
		
		DotPrinter dp = new DotPrinter(g);
		
		File dotFile = File.createTempFile("dot", ".dot");
		FileUtils.write(dotFile, dp.out());
		Printer.out(dotFile);

		File imgFile = File.createTempFile("dot", ".png");
		DotPrinter.create(dotFile, "png", imgFile);
		Printer.out(imgFile);

		GuiUtils.popupAndBlock(GuiUtils.load(imgFile));
	}

	@Test
	public void testFindDot() throws IOException {
		DotPrinter.findDot();
	}

}
