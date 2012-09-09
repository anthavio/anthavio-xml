/**
 * 
 */
package com.anthavio.xml;

import java.util.ArrayList;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;

import com.anthavio.util.ResourceUtil;

/**
 * @author vanek
 * 
 * SAX2 ContentHandler building DOM from SAX2 Events
 * 
 * Stolen 
 * com.sun.xml.bind.marshaller.SAX2DOMEx
 */
public class Sax2DomContentHandler implements ContentHandler {

	private Node node = null;
	private final Stack<Node> nodeStack = new Stack<Node>();
	private final ArrayList<String> unprocessedNamespaces = new ArrayList<String>();

	/**
	 * Document object that owns the specified node.
	 */
	private final Document document;

	/**
	 * @param   node
	 *      Nodes will be created and added under this object.
	 */
	public Sax2DomContentHandler(Node node) {
		this.node = node;
		nodeStack.push(this.node);

		if (node instanceof Document) {
			this.document = (Document) node;
		} else {
			this.document = node.getOwnerDocument();
		}
	}

	/**
	 * Creates a fresh empty DOM document and adds nodes under this document.
	 */
	public Sax2DomContentHandler() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(false);

		document = factory.newDocumentBuilder().newDocument();
		node = document;
		nodeStack.push(document);
	}

	public final Element getCurrentElement() {
		return (Element) nodeStack.peek();
	}

	public Node getDOM() {
		return node;
	}

	public void startDocument() {
	}

	public void endDocument() {
	}

	public void startElement(String namespace, String localName, String qName, Attributes attrs) {
		Node parent = nodeStack.peek();

		// some broken DOM implementatino (we confirmed it with SAXON)
		// return null from this method.
		Element element = document.createElementNS(namespace, qName);

		if (element == null) {
			// if so, report an user-friendly error message,
			// rather than dying mysteriously with NPE.
			throw new AssertionError("Dom implementation " + document.getClass().getName()
					+ " does not support createElementNS(namespace, qName)" + ResourceUtil.which(document.getClass()));
		}

		// process namespace bindings
		for (int i = 0; i < unprocessedNamespaces.size(); i += 2) {
			String prefix = unprocessedNamespaces.get(i + 0);
			String uri = unprocessedNamespaces.get(i + 1);

			String qname;
			if ("".equals(prefix) || prefix == null) {
				qname = "xmlns";
			} else {
				qname = "xmlns:" + prefix;
			}

			// older version of Xerces (I confirmed that the bug is gone with Xerces 2.4.0)
			// have a problem of re-setting the same namespace attribute twice.
			// work around this bug removing it first.
			if (element.hasAttributeNS("http://www.w3.org/2000/xmlns/", qname)) {
				// further workaround for an old Crimson bug where the removeAttribtueNS
				// method throws NPE when the element doesn't have any attribute.
				// to be on the safe side, check the existence of attributes before
				// attempting to remove it.
				// for details about this bug, see org.apache.crimson.tree.ElementNode2
				// line 540 or the following message:
				// https://jaxb.dev.java.net/servlets/ReadMsg?list=users&msgNo=2767
				element.removeAttributeNS("http://www.w3.org/2000/xmlns/", qname);
			}
			// workaround until here

			element.setAttributeNS("http://www.w3.org/2000/xmlns/", qname, uri);
		}
		unprocessedNamespaces.clear();

		int length = attrs.getLength();
		for (int i = 0; i < length; i++) {
			String namespaceuri = attrs.getURI(i);
			String value = attrs.getValue(i);
			String qname = attrs.getQName(i);
			element.setAttributeNS(namespaceuri, qname, value);
		}
		// append this new node onto current stack node
		parent.appendChild(element);
		// push this node onto stack
		nodeStack.push(element);
	}

	public void endElement(String namespace, String localName, String qName) {
		nodeStack.pop();
	}

	public void characters(char[] ch, int start, int length) {
		Node parent = nodeStack.peek();
		Text text = document.createTextNode(new String(ch, start, length));
		parent.appendChild(text);
	}

	public void ignorableWhitespace(char[] ch, int start, int length) {
	}

	public void processingInstruction(String target, String data) throws org.xml.sax.SAXException {
		Node parent = nodeStack.peek();
		Node node = document.createProcessingInstruction(target, data);
		parent.appendChild(node);
	}

	public void setDocumentLocator(Locator locator) {
	}

	public void skippedEntity(String name) {
	}

	public void startPrefixMapping(String prefix, String uri) {
		unprocessedNamespaces.add(prefix);
		unprocessedNamespaces.add(uri);
	}

	public void endPrefixMapping(String prefix) {
	}
}
