package winterwell.utils.web;

import java.io.StringReader;

import org.ccil.cowan.tagsoup.HTMLSchema;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import winterwell.utils.Utils;
import winterwell.utils.containers.Tree;

/**
 * Just a wrapper for using TagSoup to parse (potentially broken) html.
 * @author daniel
 *
 */
public class HtmlParser {


	/**
	 * Use TagSoup to parse potentially malformed html.
	 * 
	 * @param html
	 * @return a tree of the document. The root node has no XMLNode (it is the
	 *         document super-node).
	 * @see WebUtils#parseXmlToTree(String)
	 */
	public static Tree<XMLNode> parseHtmlToTree(String html) {
		try {
			XMLReader r = new Parser();
			HTMLSchema theSchema = new HTMLSchema();
			r.setProperty(Parser.schemaProperty, theSchema);
			XmlTreeBuilder treeBuilder = new XmlTreeBuilder();
			r.setContentHandler(treeBuilder);
			r.parse(new InputSource(new StringReader(html)));
			return treeBuilder.getTree();
		} catch (Exception ex) {
			throw Utils.runtime(ex);
		}
	}

}
