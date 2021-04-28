//package com.winterwell.utils.web;
//
//import org.xml.sax.Attributes;
//import org.xml.sax.SAXException;
//import org.xml.sax.helpers.DefaultHandler;
//
//import com.winterwell.utils.Utils;
//import com.winterwell.utils.containers.ITree;
//import com.winterwell.utils.containers.Tree;
//
///**
// * Used by {@link WebUtils#parseXml(String)} to build DOM-like trees. Rationale:
// * Document & associated classes suck.
// *
// * <p>
// * TODO should we add a filter on nodes, to pre-ignore boring ones?
// *
// * @author daniel
// */
//final class XmlTreeBuilder extends DefaultHandler {
//
//	ITree<XMLNode> activeTree;
//
//	private boolean endFlag;
//
//	private Tree<XMLNode> root;
//
//	StringBuilder text = new StringBuilder();
//
//	public XmlTreeBuilder() {
//	}
//
//	@Override
//	public void characters(char[] ch, int start, int length)
//			throws SAXException {
//		text.append(ch, start, length);
//	}
//
//	@Override
//	public void endDocument() throws SAXException {
//		endFlag = true;
//	}
//
//	@Override
//	public void endElement(String uri, String localName, String name)
//			throws SAXException {
//		// Create a text node?
//		processTextBuffer();
//		// Pop node
//		activeTree = activeTree.getParent();
//	}
//
//	/**
//	 * The root node has no XMLNode (it is the document super-node).
//	 *
//	 * @return
//	 */
//	public Tree<XMLNode> getTree() {
//		assert endFlag;
//		return root;
//	}
//
//	private void processTextBuffer() {
//		if (text.length() == 0)
//			return;
//		XMLNode textNode = new XMLNode(text.toString(), true);
//		Tree<XMLNode> textTreeNode = new Tree<XMLNode>(activeTree, textNode);
//		text = new StringBuilder();
//	}
//
//	@Override
//	public void skippedEntity(String name) throws SAXException {
//		// do nothing
//	}
//
//	@Override
//	public void startDocument() throws SAXException {
//		root = new Tree<XMLNode>();
//		activeTree = root;
//	}
//
//	@Override
//	public void startElement(String uri, String localName, String name,
//			Attributes atts) throws SAXException {
//		// Create a text node?
//		processTextBuffer();
//		// Build node
//		String tag = Utils.isBlank(localName) ? name : localName;
//		XMLNode node = new XMLNode(tag);
//		for (int i = 0, n = atts.getLength(); i < n; i++) {
//			String aName = atts.getQName(i);
//			String value = atts.getValue(i);
//			node.getAttributes().put(aName, value);
//		}
//		// New active tree-node
//		Tree<XMLNode> tree = new Tree<XMLNode>(activeTree, node);
//		activeTree = tree;
//	}
//	
//
//}
