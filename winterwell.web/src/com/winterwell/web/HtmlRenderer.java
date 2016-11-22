package com.winterwell.web;

import java.io.IOException;

import org.xml.sax.SAXException;

/**
 * Experiments in rendering html.
 * 
 * Cobra 0.98 is OK, but fails for high charts, and some of the winterwell.com
 * css Flying saucer ?? command line tools??
 * 
 * 
 * @author daniel
 * 
 */
public class HtmlRenderer {

	public static void main(String[] args) throws SAXException, IOException {

		// runCobra();
	}

	// private static void runCobra() {
	// HtmlPanel panel = new HtmlPanel();
	// panel.setSize(400, 600);
	// // This panel should be added to a JFrame or
	// // another Swing component.
	// SimpleUserAgentContext ucontext = new SimpleUserAgentContext(){
	// @Override
	// public HttpRequest createHttpRequest() {
	// SimpleHttpRequest hr = new SimpleHttpRequest(this, this.getProxy()){
	// @Override
	// public void open(String method, URL url, boolean asyncFlag,
	// String userName, String password)
	// throws IOException {
	// super.open(method, url, asyncFlag, userName, password);
	// }
	// };
	// return hr;
	// }
	// };
	// SimpleHtmlRendererContext rcontext = new SimpleHtmlRendererContext(panel,
	// ucontext);
	// // Note that document builder should receive both contexts.
	// DocumentBuilderImpl dbi = new DocumentBuilderImpl(ucontext, rcontext);
	// String documentURI =
	// "http://www.highcharts.com/demo/?example=area-stacked-percent&theme=grid";
	// //"http://www.winterwell.com";
	// String page = new FakeBrowser().getPage(documentURI);
	// Reader documentReader = new StringReader(page);
	// // A documentURI should be provided to resolve relative URIs.
	// Document document = dbi.parse(new InputSourceImpl(documentReader,
	// documentURI));
	// // Now set document in panel. This is what causes the document to render.
	// panel.setDocument(document, rcontext);
	// // test!
	// JFrame f = GuiUtils.popup(panel, "winterwell");
	// GuiUtils.blockWhileOpen(f);
	// }

}
