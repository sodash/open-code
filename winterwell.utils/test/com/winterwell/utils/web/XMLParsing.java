//package winterwell.utils.web;
//
//import java.io.FileInputStream;
//import java.io.Reader;
//import java.io.StringReader;
//import java.util.List;
//
//import javax.xml.stream.XMLInputFactory;
//import javax.xml.stream.events.XMLEvent;
//
//import org.codehaus.stax2.XMLInputFactory2;
//import org.codehaus.stax2.XMLStreamReader2;
//import org.w3c.dom.Node;
//import org.xml.sax.InputSource;
//
//import com.ctc.wstx.stax.WstxInputFactory;
//
///**
// * experiments in using WoodStox library to replace the awful Xerces
// * @author daniel
// *
// */
//public class XMLParsing {
//	
//	/*XMLInputFactory2*/ WstxInputFactory xmlif; 
//	
//	XMLInputFactory2 getFactory() {
//		if (xmlif != null) return xmlif;
//		xmlif = new WstxInputFactory();
//		xmlif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
//		xmlif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,Boolean.FALSE);
//		xmlif.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
//		xmlif.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
////		xmlif.configureForSpeed();
//		xmlif.configureForConvenience();
//		return xmlif;
//	}
//	
//	List<Node> parse(Reader xml, String nodeType) {
//		XMLStreamReader2 xmlr = (XMLStreamReader2) xmlif.createXMLStreamReader(xml);
//		while(xmlr.hasNext()){
//			int eventType = xmlr.next();
//			switch (eventType) {
//				case XMLEvent.START_ELEMENT:
//					curElement = xmlr.getName().toString();
//					break;
//				case XMLEvent.CHARACTERS:
//					break;
//				case XMLEvent.END_ELEMENT:
//					break;
//				case XMLEvent.END_DOCUMENT:
//			}				
//		}
//	}
// }
