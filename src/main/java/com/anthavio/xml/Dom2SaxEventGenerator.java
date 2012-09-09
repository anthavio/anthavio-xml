/**
 * 
 */
package com.anthavio.xml;

import java.util.Enumeration;

import javax.xml.bind.ValidationEventLocator;
import javax.xml.bind.helpers.ValidationEventLocatorImpl;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * @author vanek
 * 
 * Traverses all DOM Nodes generating SAX2 Events
 * Events are sent to the specified SAX2 ContentHandler
 *
 * Stolen
 * com.sun.xml.bind.unmarshaller.DOMScanner
 */
public class Dom2SaxEventGenerator {

	/** reference to the current node being scanned - used for determining
	 *  location info for validation events */
	private Node currentNode = null;

	/** To save memory, only one instance of AttributesImpl will be used. */
	private final AttributesImpl attrs = new AttributesImpl();

	/** This handler will receive SAX2 events. */
	private ContentHandler handler = null;

	public Dom2SaxEventGenerator(ContentHandler handler) {
		if (handler == null) {
			throw new IllegalArgumentException("ContentHandler must not be null");
		}
		this.handler = handler;
	}

	public void scan(Node node) throws SAXException {
		if (node instanceof Document) {
			scan((Document) node);
		} else {
			scan((Element) node);
		}
	}

	public void scan(Document doc) throws SAXException {
		scan(doc.getDocumentElement());
	}

	public void scan(Element e) throws SAXException {
		setCurrentLocation(e);

		//receiver.setDocumentLocator(locator);
		handler.startDocument();

		NamespaceSupport nss = new NamespaceSupport();
		buildNamespaceSupport(nss, e.getParentNode());

		for (Enumeration<?> en = nss.getPrefixes(); en.hasMoreElements();) {
			String prefix = (String) en.nextElement();
			handler.startPrefixMapping(prefix, nss.getURI(prefix));
		}

		visit(e);

		for (Enumeration<?> en = nss.getPrefixes(); en.hasMoreElements();) {
			String prefix = (String) en.nextElement();
			handler.endPrefixMapping(prefix);
		}

		setCurrentLocation(e);
		handler.endDocument();
	}

	/**
	 * Parses a subtree starting from the element e and
	 * reports SAX2 events to the specified handler.
	 * 
	 * @deprecated in JAXB 2.0
	 *      Use {@link #scan(Element)}
	 *
	@Deprecated
	public void parse(Element e, ContentHandler handler) throws SAXException {
		// it might be better to set receiver at the constructor.
		this.handler = handler;

		setCurrentLocation(e);
		handler.startDocument();

		//receiver.setDocumentLocator(locator);
		visit(e);

		setCurrentLocation(e);
		handler.endDocument();
	}
	*/

	/**
	 * Similar to the parse method but it visits the ancestor nodes
	 * and properly emulate the all in-scope namespace declarations.
	 * 
	 * @deprecated in JAXB 2.0
	 *      Use {@link #scan(Element)}
	 * 
	@Deprecated
	public void parseWithContext(Element e, ContentHandler handler) throws SAXException {
		setContentHandler(handler);
		scan(e);
	}
	*/

	/**
	 * Recursively visit ancestors and build up {@link NamespaceSupport} oject.
	 */
	private void buildNamespaceSupport(NamespaceSupport nss, Node node) {
		if (node == null || node.getNodeType() != Node.ELEMENT_NODE) {
			return;
		}

		buildNamespaceSupport(nss, node.getParentNode());

		nss.pushContext();
		NamedNodeMap atts = node.getAttributes();
		for (int i = 0; i < atts.getLength(); i++) {
			Attr a = (Attr) atts.item(i);
			if ("xmlns".equals(a.getPrefix())) {
				nss.declarePrefix(a.getLocalName(), a.getValue());
				continue;
			}
			if ("xmlns".equals(a.getName())) {
				nss.declarePrefix("", a.getValue());
				continue;
			}
		}
	}

	/**
	 * Visits an element and its subtree.
	 */
	public void visit(Element e) throws SAXException {
		setCurrentLocation(e);
		final NamedNodeMap attributes = e.getAttributes();

		attrs.clear();
		int len = attributes == null ? 0 : attributes.getLength();

		for (int i = len - 1; i >= 0; i--) {
			Attr a = (Attr) attributes.item(i);
			String name = a.getName();
			// start namespace binding
			if (name.startsWith("xmlns")) {
				if (name.length() == 5) {
					handler.startPrefixMapping("", a.getValue());
				} else {
					String localName = a.getLocalName();
					if (localName == null) {
						// DOM built without namespace support has this problem
						localName = name.substring(6);
					}
					handler.startPrefixMapping(localName, a.getValue());
				}
				continue;
			}

			String uri = a.getNamespaceURI();
			if (uri == null) {
				uri = "";
			}

			String local = a.getLocalName();
			if (local == null) {
				local = a.getName();
			}
			// add other attributes to the attribute list
			// that we will pass to the ContentHandler
			attrs.addAttribute(uri, local, a.getName(), "CDATA", a.getValue());
		}

		String uri = e.getNamespaceURI();
		if (uri == null) {
			uri = "";
		}
		String local = e.getLocalName();
		String qname = e.getTagName();
		if (local == null) {
			local = qname;
		}
		handler.startElement(uri, local, qname, attrs);

		// visit its children
		NodeList children = e.getChildNodes();
		int clen = children.getLength();
		for (int i = 0; i < clen; i++) {
			visit(children.item(i));
		}

		setCurrentLocation(e);
		handler.endElement(uri, local, qname);

		// call the endPrefixMapping method
		for (int i = len - 1; i >= 0; i--) {
			Attr a = (Attr) attributes.item(i);
			String name = a.getName();
			if (name.startsWith("xmlns")) {
				if (name.length() == 5) {
					handler.endPrefixMapping("");
				} else {
					handler.endPrefixMapping(a.getLocalName());
				}
			}
		}
	}

	private void visit(Node n) throws SAXException {
		setCurrentLocation(n);

		// if a case statement gets too big, it should be made into a separate method.
		switch (n.getNodeType()) {
		case Node.CDATA_SECTION_NODE:
		case Node.TEXT_NODE:
			String value = n.getNodeValue();
			handler.characters(value.toCharArray(), 0, value.length());
			break;
		case Node.ELEMENT_NODE:
			visit((Element) n);
			break;
		case Node.ENTITY_REFERENCE_NODE:
			handler.skippedEntity(n.getNodeName());
			break;
		case Node.PROCESSING_INSTRUCTION_NODE:
			ProcessingInstruction pi = (ProcessingInstruction) n;
			handler.processingInstruction(pi.getTarget(), pi.getData());
			break;
		}
	}

	private void setCurrentLocation(Node currNode) {
		currentNode = currNode;
	}

	/**
	 * The same as {@link #getCurrentElement()} but
	 * better typed.
	 */
	public Node getCurrentLocation() {
		return currentNode;
	}

	public Object getCurrentElement() {
		return currentNode;
	}

	public void setContentHandler(ContentHandler handler) {
		this.handler = handler;
	}

	public ContentHandler getContentHandler() {
		return this.handler;
	}

	// LocatorEx implementation
	public String getPublicId() {
		return null;
	}

	public String getSystemId() {
		return null;
	}

	public int getLineNumber() {
		return -1;
	}

	public int getColumnNumber() {
		return -1;
	}

	public ValidationEventLocator getLocation() {
		return new ValidationEventLocatorImpl(getCurrentLocation());
	}

}
