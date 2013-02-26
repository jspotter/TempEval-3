package tempeval;

import java.io.File;
import java.util.Vector;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

public class XMLParser {
	
	private static final String testfile = 
		"data/INPUT-DATA-SAMPLE/ABC19980120.1830.0957.tml.TE3input.tml";
	
	private static class MyErrorHandler implements ErrorHandler {

		public void warning(SAXParseException exception)
		throws SAXException {
			fatalError(exception);
		}

		public void error(SAXParseException exception)
		throws SAXException {
			fatalError(exception);
		}

		public void fatalError(SAXParseException exception)
		throws SAXException {
			exception.printStackTrace();
			System.exit(3);
		}
	}

	/* Non-recursive (NR) version of Node.getElementsByTagName(...) */
	public static Element[] getElementsByTagNameNR(Element e, String tagName) {
		Vector< Element > elements = new Vector< Element >();
		Node child = e.getFirstChild();
		while (child != null) {
			if (child instanceof Element && child.getNodeName().equals(tagName))
			{
				elements.add( (Element)child );
			}
			child = child.getNextSibling();
		}
		Element[] result = new Element[elements.size()];
		elements.copyInto(result);
		return result;
	}

	/* Returns the first subelement of e matching the given tagName, or
	 * null if one does not exist. */
	public static Element getElementByTagNameNR(Element e, String tagName) {
		Node child = e.getFirstChild();
		while (child != null) {
			if (child instanceof Element && child.getNodeName().equals(tagName))
				return (Element) child;
			child = child.getNextSibling();
		}
		return null;
	}

	/* Returns the text associated with the given element (which must have
	 * type #PCDATA) as child, or "" if it contains no text. */
	public static String getElementText(Element e) {
		if (e.getChildNodes().getLength() == 1) {
			Text elementText = (Text) e.getFirstChild();
			return elementText.getNodeValue();
		} else {
			return e.getTextContent();
		}
	}

	/* Returns the text (#PCDATA) associated with the first subelement X
	 * of e with the given tagName. If no such X exists or X contains no
	 * text, "" is returned. */
	public static String getElementTextByTagNameNR(Element e, String tagName) {
		Element elem = getElementByTagNameNR(e, tagName);
		if (elem != null)
			return getElementText(elem);
		else
			return "";
	}
	
	/**
	 * Parses file.
	 */
	public static Document parse(String filename) throws Exception {
		return parse(new File(filename));
	}
	
	public static Document parse(File file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringElementContentWhitespace(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		builder.setErrorHandler(new MyErrorHandler());
		return builder.parse(file);
	}
	
	//assumes user enters full tagname including < > brackets
	public static String getRawTextByTagName(String text, String startTagName, String endTagName){
		String result = text.substring(text.indexOf(startTagName) + startTagName.length(),
				text.indexOf(endTagName));
		return result;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Document doc = parse(testfile);
		Element root = doc.getDocumentElement();
		String text = getElementTextByTagNameNR(root, "TEXT");
		System.out.println(text);
	}

}
