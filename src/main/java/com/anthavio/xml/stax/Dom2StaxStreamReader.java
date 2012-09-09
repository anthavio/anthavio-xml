/**
 * 
 */
package com.anthavio.xml.stax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.anthavio.xml.XmlNamespaceContext;


/**
 * @author vanek
 * 
 * Simplified org.codehaus.stax2.ri.dom.DOMWrappingReader
 *
 */
public class Dom2StaxStreamReader implements XMLStreamReader, NamespaceContext, XMLStreamConstants {

	public final static Location NOT_AVAILABLE = new Location() {

		public int getCharacterOffset() {
			return -1;
		}

		public int getColumnNumber() {
			return -1;
		}

		public int getLineNumber() {
			return -1;
		}

		public String getPublicId() {
			return null;
		}

		public String getSystemId() {
			return null;
		}
	};

	protected final static int INT_SPACE = 0x0020;

	// // // Bit masks used for quick type comparisons

	final private static int MASK_GET_TEXT = (1 << CHARACTERS) | (1 << CDATA) | (1 << SPACE) | (1 << COMMENT)
			| (1 << DTD) | (1 << ENTITY_REFERENCE);

	final private static int MASK_GET_TEXT_XXX = (1 << CHARACTERS) | (1 << CDATA) | (1 << SPACE) | (1 << COMMENT);

	final private static int MASK_GET_ELEMENT_TEXT = (1 << CHARACTERS) | (1 << CDATA) | (1 << SPACE)
			| (1 << ENTITY_REFERENCE);

	final protected static int MASK_TYPED_ACCESS_BINARY = (1 << START_ELEMENT) //  note: END_ELEMENT handled separately
			| (1 << CHARACTERS) | (1 << CDATA) | (1 << SPACE);

	// // // Enumerated error case ids

	/**
	* Current state not START_ELEMENT, should be
	*/
	protected final static int ERR_STATE_NOT_START_ELEM = 1;

	/**
	* Current state not START_ELEMENT or END_ELEMENT, should be
	*/
	protected final static int ERR_STATE_NOT_ELEM = 2;

	/**
	* Current state not PROCESSING_INSTRUCTION
	*/
	protected final static int ERR_STATE_NOT_PI = 3;

	/**
	* Current state not one where getText() can be used
	*/
	protected final static int ERR_STATE_NOT_TEXTUAL = 4;

	/**
	* Current state not one where getTextXxx() can be used
	*/
	protected final static int ERR_STATE_NOT_TEXTUAL_XXX = 5;

	protected final static int ERR_STATE_NOT_TEXTUAL_OR_ELEM = 6;

	protected final static int ERR_STATE_NO_LOCALNAME = 7;

	// // // Configuration:

	protected final String _systemId;

	protected final Node _rootNode;

	/**
	* Whether stream reader is to be namespace aware (as per property
	* {@link XMLInputFactory#IS_NAMESPACE_AWARE}) or not
	*/
	protected final boolean _cfgNsAware;

	/**
	* Whether stream reader is to coalesce adjacent textual
	* (CHARACTERS, SPACE, CDATA) events (as per property
	* {@link XMLInputFactory#IS_COALESCING}) or not
	*/
	protected final boolean _coalescing;

	/**
	* By default we do not force interning of names: can be
	* reset by sub-classes.
	*/
	protected boolean _cfgInternNames = false;

	/**
	* By default we do not force interning of namespace URIs: can be
	* reset by sub-classes.
	*/
	protected boolean _cfgInternNsURIs = false;

	// // // State:

	protected int _currEvent = START_DOCUMENT;

	/**
	* Current node is the DOM node that contains information
	* regarding the current event.
	*/
	protected Node _currNode;

	protected int _depth = 0;

	/**
	* In coalescing mode, we may need to combine textual content
	* from multiple adjacent nodes. Since we shouldn't be modifying
	* the underlying DOM tree, need to accumulate it into a temporary
	* variable
	*/
	protected String _coalescedText;

	/**
	* Helper object used for combining segments of text as needed
	*/
	protected TextBuffer _textBuffer = new TextBuffer();

	// // // Attribute/namespace declaration state

	/* DOM, alas, does not distinguish between namespace declarations
	* and attributes (due to its roots prior to XML namespaces?).
	* Because of this, two lists need to be separated. Since this
	* information is often not needed, it will be lazily generated.
	*/

	/**
	* Lazily instantiated List of all actual attributes for the
	* current (start) element, NOT including namespace declarations.
	* As such, elements are {@link org.w3c.dom.Attr} instances.
	*<p>
	*/
	protected List<Node> _attrList = null;

	/**
	* Lazily instantiated String pairs of all namespace declarations for the
	* current (start/end) element. String pair means that for each
	* declarations there are two Strings in the list: first one is prefix
	* (empty String for the default namespace declaration), and second
	* URI it is bound to.
	*/
	protected List<String> _nsDeclList = null;

	/*
	////////////////////////////////////////////////////
	// Construction, configuration
	////////////////////////////////////////////////////
	*/

	public Dom2StaxStreamReader(Node document) throws XMLStreamException {
		this(new DOMSource(document), true, false);
	}

	/**
	* @param src Node that is the tree of the DOM document, or fragment.
	* @param nsAware Whether resulting reader should operate in namespace
	*   aware mode or not. Note that this should be compatible with
	*   settings for the DOM builder that produced DOM tree or fragment
	*   being operated on, otherwise results are not defined.
	* @param coalescing Whether resulting reader should coalesce adjacent
	*    text events or not
	*/
	public Dom2StaxStreamReader(DOMSource src, boolean nsAware, boolean coalescing) throws XMLStreamException {
		Node treeRoot = src.getNode();
		if (treeRoot == null) {
			throw new IllegalArgumentException("Can not pass null Node for constructing a DOM-based XMLStreamReader");
		}
		_cfgNsAware = nsAware;
		_coalescing = coalescing;
		_systemId = src.getSystemId();

		/* Ok; we need a document node; or an element node; or a document
		* fragment node.
		*/
		switch (treeRoot.getNodeType()) {
		case Node.DOCUMENT_NODE: // fine
			/* Should try to find encoding, version and stand-alone
			* settings... but is there a standard way of doing that?
			*/
		case Node.ELEMENT_NODE: // can make sub-tree... ok
			// But should we skip START/END_DOCUMENT? For now, let's not

		case Node.DOCUMENT_FRAGMENT_NODE: // as with element...

			// Above types are fine
			break;

		default: // other Nodes not usable
			throw new XMLStreamException("Can not create an XMLStreamReader for a DOM node of type " + treeRoot.getClass());
		}
		_rootNode = _currNode = treeRoot;
	}

	protected void setInternNames(boolean state) {
		_cfgInternNames = state;
	}

	protected void setInternNsURIs(boolean state) {
		_cfgInternNsURIs = state;
	}

	/*
	////////////////////////////////////////////////////
	// Abstract methods for sub-classes to implement
	////////////////////////////////////////////////////
	*/

	protected void throwStreamException(String msg, Location loc) throws XMLStreamException {
		if (loc == null) {
			throw new XMLStreamException(msg);
		}
		throw new XMLStreamException(msg, loc);
	}

	/*
	////////////////////////////////////////////////////
	// XMLStreamReader, document info
	////////////////////////////////////////////////////
	*/

	/**
	* As per Stax (1.0) specs, needs to return whatever xml declaration
	* claimed encoding is, if any; or null if no xml declaration found.
	*/
	public String getCharacterEncodingScheme() {
		/* No standard way to figure it out from a DOM Document node;
		* have to return null
		*/
		return null;
	}

	/**
	* As per Stax (1.0) specs, needs to return whatever parser determined
	* the encoding was, if it was able to figure it out. If not (there are
	* cases where this can not be found; specifically when being passed a
	* {@link java.io.Reader}), it should return null.
	*/
	public String getEncoding() {
		/* We have no information regarding underlying stream/Reader, so
		* best we can do is to see if we know xml declaration encoding.
		*/
		return getCharacterEncodingScheme();
	}

	public String getVersion() {
		/* No standard way to figure it out from a DOM Document node;
		* have to return null
		*/
		return null;
	}

	public boolean isStandalone() {
		/* No standard way to figure it out from a DOM Document node;
		* have to return false
		*/
		return false;
	}

	public boolean standaloneSet() {
		/* No standard way to figure it out from a DOM Document node;
		* have to return false
		*/
		return false;
	}

	/*
	////////////////////////////////////////////////////
	// Public API, configuration
	////////////////////////////////////////////////////
	*/

	public Object getProperty(String name) {
		return null;
	}

	public boolean isPropertySupported(String name) {
		return false;
	}

	public boolean setProperty(String name, Object value) {
		return false;
	}

	/*
	////////////////////////////////////////////////////
	// XMLStreamReader, current state
	////////////////////////////////////////////////////
	*/

	// // // Attribute access:

	public int getAttributeCount() {
		if (_currEvent != START_ELEMENT) {
			reportWrongState(ERR_STATE_NOT_START_ELEM);
		}
		if (_attrList == null) {
			_calcNsAndAttrLists(true);
		}
		return _attrList.size();
	}

	public String getAttributeLocalName(int index) {
		if (_currEvent != START_ELEMENT) {
			reportWrongState(ERR_STATE_NOT_START_ELEM);
		}
		if (_attrList == null) {
			_calcNsAndAttrLists(true);
		}
		if (index >= _attrList.size() || index < 0) {
			handleIllegalAttrIndex(index);
			return null;
		}
		Attr attr = (Attr) _attrList.get(index);
		return _internName(_safeGetLocalName(attr));
	}

	public QName getAttributeName(int index) {
		if (_currEvent != START_ELEMENT) {
			reportWrongState(ERR_STATE_NOT_START_ELEM);
		}
		if (_attrList == null) {
			_calcNsAndAttrLists(true);
		}
		if (index >= _attrList.size() || index < 0) {
			handleIllegalAttrIndex(index);
			return null;
		}
		Attr attr = (Attr) _attrList.get(index);
		return _constructQName(attr.getNamespaceURI(), _safeGetLocalName(attr), attr.getPrefix());
	}

	public String getAttributeNamespace(int index) {
		if (_currEvent != START_ELEMENT) {
			reportWrongState(ERR_STATE_NOT_START_ELEM);
		}
		if (_attrList == null) {
			_calcNsAndAttrLists(true);
		}
		if (index >= _attrList.size() || index < 0) {
			handleIllegalAttrIndex(index);
			return null;
		}
		Attr attr = (Attr) _attrList.get(index);
		return _internNsURI(attr.getNamespaceURI());
	}

	public String getAttributePrefix(int index) {
		if (_currEvent != START_ELEMENT) {
			reportWrongState(ERR_STATE_NOT_START_ELEM);
		}
		if (_attrList == null) {
			_calcNsAndAttrLists(true);
		}
		if (index >= _attrList.size() || index < 0) {
			handleIllegalAttrIndex(index);
			return null;
		}
		Attr attr = (Attr) _attrList.get(index);
		return _internName(attr.getPrefix());
	}

	public String getAttributeType(int index) {
		if (_currEvent != START_ELEMENT) {
			reportWrongState(ERR_STATE_NOT_START_ELEM);
		}
		if (_attrList == null) {
			_calcNsAndAttrLists(true);
		}
		if (index >= _attrList.size() || index < 0) {
			handleIllegalAttrIndex(index);
			return null;
		}
		//Attr attr = (Attr) _attrList.get(index);
		// First, a special case, ID... since it's potentially most useful
		/* 26-Apr-2006, TSa: Turns out that following methods are
		*    DOM Level3, and as such not available in JDK 1.4 and prior.
		*    Thus, let's not yet use them (could use dynamic discovery
		*    for graceful downgrade)
		*/
		/*
		if (attr.isId()) {
		return "ID";
		}
		TypeInfo schemaType = attr.getSchemaTypeInfo();
		return (schemaType == null) ? "CDATA" : schemaType.getTypeName();
		*/
		return "CDATA";
	}

	public String getAttributeValue(int index) {
		if (_currEvent != START_ELEMENT) {
			reportWrongState(ERR_STATE_NOT_START_ELEM);
		}
		if (_attrList == null) {
			_calcNsAndAttrLists(true);
		}
		if (index >= _attrList.size() || index < 0) {
			handleIllegalAttrIndex(index);
			return null;
		}
		Attr attr = (Attr) _attrList.get(index);
		return attr.getValue();
	}

	public String getAttributeValue(String nsURI, String localName) {
		if (_currEvent != START_ELEMENT) {
			reportWrongState(ERR_STATE_NOT_START_ELEM);
		}
		Element elem = (Element) _currNode;
		NamedNodeMap attrs = elem.getAttributes();
		/* Hmmh. DOM javadocs claim "Per [XML Namespaces], applications
		* must use the value null as the namespaceURI parameter for methods
		* if they wish to have no namespace.".
		* Not sure how true that is, but:
		*/
		if (nsURI != null && nsURI.length() == 0) {
			nsURI = null;
		}
		Attr attr = (Attr) attrs.getNamedItemNS(nsURI, localName);
		return (attr == null) ? null : attr.getValue();
	}

	/**
	* From StAX specs:
	*<blockquote>
	* Reads the content of a text-only element, an exception is thrown if
	* this is not a text-only element.
	* Regardless of value of javax.xml.stream.isCoalescing this method always
	* returns coalesced content.
	*<br/>Precondition: the current event is START_ELEMENT.
	*<br/>Postcondition: the current event is the corresponding END_ELEMENT. 
	*</blockquote>
	*/
	public String getElementText() throws XMLStreamException {
		if (_currEvent != START_ELEMENT) {
			/* Quite illogical: this is not an IllegalStateException
			* like other similar ones, but rather an XMLStreamException.
			* But that's how Stax JavaDocs outline how it should be.
			*/
			reportParseProblem(ERR_STATE_NOT_START_ELEM);
		}
		// As per [WSTX-244], handling of coalescing, regular differ a lot, so:
		if (_coalescing) {
			String text = null;
			// Need to loop to get rid of PIs, comments
			while (true) {
				int type = next();
				if (type == END_ELEMENT) {
					break;
				}
				if (type == COMMENT || type == PROCESSING_INSTRUCTION) {
					continue;
				}
				if (((1 << type) & MASK_GET_ELEMENT_TEXT) == 0) {
					reportParseProblem(ERR_STATE_NOT_TEXTUAL);
				}
				if (text == null) {
					text = getText();
				} else { // uncommon but possible (with comments, PIs):
					text = text + getText();
				}
			}
			return (text == null) ? "" : text;
		}
		_textBuffer.reset();
		// Need to loop to get rid of PIs, comments
		while (true) {
			int type = next();
			if (type == END_ELEMENT) {
				break;
			}
			if (type == COMMENT || type == PROCESSING_INSTRUCTION) {
				continue;
			}
			if (((1 << type) & MASK_GET_ELEMENT_TEXT) == 0) {
				reportParseProblem(ERR_STATE_NOT_TEXTUAL);
			}
			_textBuffer.append(getText());
		}
		return _textBuffer.get();
	}

	/**
	* Returns type of the last event returned; or START_DOCUMENT before
	* any events has been explicitly returned.
	*/
	public int getEventType() {
		return _currEvent;
	}

	public String getLocalName() {
		if (_currEvent == START_ELEMENT || _currEvent == END_ELEMENT) {
			return _internName(_safeGetLocalName(_currNode));
		}
		if (_currEvent != ENTITY_REFERENCE) {
			reportWrongState(ERR_STATE_NO_LOCALNAME);
		}
		return _internName(_currNode.getNodeName());
	}

	public final Location getLocation() {
		return NOT_AVAILABLE;
	}

	public QName getName() {
		if (_currEvent != START_ELEMENT && _currEvent != END_ELEMENT) {
			reportWrongState(ERR_STATE_NOT_START_ELEM);
		}
		return _constructQName(_currNode.getNamespaceURI(), _safeGetLocalName(_currNode), _currNode.getPrefix());
	}

	// // // Namespace access

	public NamespaceContext getNamespaceContext() {
		return this;
	}

	public int getNamespaceCount() {
		if (_currEvent != START_ELEMENT && _currEvent != END_ELEMENT) {
			reportWrongState(ERR_STATE_NOT_ELEM);
		}
		if (_nsDeclList == null) {
			if (!_cfgNsAware) {
				return 0;
			}
			_calcNsAndAttrLists(_currEvent == START_ELEMENT);
		}
		return _nsDeclList.size() / 2;
	}

	/**
	* Alas, DOM does not expose any of information necessary for
	* determining actual declarations. Thus, have to indicate that
	* there are no declarations.
	*/
	public String getNamespacePrefix(int index) {
		if (_currEvent != START_ELEMENT && _currEvent != END_ELEMENT) {
			reportWrongState(ERR_STATE_NOT_ELEM);
		}
		if (_nsDeclList == null) {
			if (!_cfgNsAware) {
				handleIllegalNsIndex(index);
			}
			_calcNsAndAttrLists(_currEvent == START_ELEMENT);
		}
		if (index < 0 || (index + index) >= _nsDeclList.size()) {
			handleIllegalNsIndex(index);
		}
		// Note: _nsDeclList entries have been appropriately intern()ed if need be
		return _nsDeclList.get(index + index);
	}

	public String getNamespaceURI() {
		if (_currEvent != START_ELEMENT && _currEvent != END_ELEMENT) {
			reportWrongState(ERR_STATE_NOT_ELEM);
		}
		return _internNsURI(_currNode.getNamespaceURI());
	}

	public String getNamespaceURI(int index) {
		if (_currEvent != START_ELEMENT && _currEvent != END_ELEMENT) {
			reportWrongState(ERR_STATE_NOT_ELEM);
		}
		if (_nsDeclList == null) {
			if (!_cfgNsAware) {
				handleIllegalNsIndex(index);
			}
			_calcNsAndAttrLists(_currEvent == START_ELEMENT);
		}
		if (index < 0 || (index + index) >= _nsDeclList.size()) {
			handleIllegalNsIndex(index);
		}
		// Note: _nsDeclList entries have been appropriately intern()ed if need be
		return _nsDeclList.get(index + index + 1);
	}

	// Note: implemented as part of NamespaceContext
	//public String getNamespaceURI(String prefix)

	public String getPIData() {
		if (_currEvent != PROCESSING_INSTRUCTION) {
			reportWrongState(ERR_STATE_NOT_PI);
		}
		return _currNode.getNodeValue();
	}

	public String getPITarget() {
		if (_currEvent != PROCESSING_INSTRUCTION) {
			reportWrongState(ERR_STATE_NOT_PI);
		}
		return _internName(_currNode.getNodeName());
	}

	public String getPrefix() {
		if (_currEvent != START_ELEMENT && _currEvent != END_ELEMENT) {
			reportWrongState(ERR_STATE_NOT_ELEM);
		}
		return _internName(_currNode.getPrefix());
	}

	public String getText() {
		if (_coalescedText != null) {
			return _coalescedText;
		}
		if (((1 << _currEvent) & MASK_GET_TEXT) == 0) {
			reportWrongState(ERR_STATE_NOT_TEXTUAL);
		}
		return _currNode.getNodeValue();
	}

	public char[] getTextCharacters() {
		String text = getText();
		return text.toCharArray();
	}

	public int getTextCharacters(int sourceStart, char[] target, int targetStart, int len) {
		if (((1 << _currEvent) & MASK_GET_TEXT_XXX) == 0) {
			reportWrongState(ERR_STATE_NOT_TEXTUAL_XXX);
		}
		String text = getText();
		if (len > text.length()) {
			len = text.length();
		}
		text.getChars(sourceStart, sourceStart + len, target, targetStart);
		return len;
	}

	public int getTextLength() {
		if (((1 << _currEvent) & MASK_GET_TEXT_XXX) == 0) {
			reportWrongState(ERR_STATE_NOT_TEXTUAL_XXX);
		}
		return getText().length();
	}

	public int getTextStart() {
		if (((1 << _currEvent) & MASK_GET_TEXT_XXX) == 0) {
			reportWrongState(ERR_STATE_NOT_TEXTUAL_XXX);
		}
		return 0;
	}

	public boolean hasName() {
		return (_currEvent == START_ELEMENT) || (_currEvent == END_ELEMENT);
	}

	public boolean hasNext() {
		return (_currEvent != END_DOCUMENT);
	}

	public boolean hasText() {
		return (((1 << _currEvent) & MASK_GET_TEXT) != 0);
	}

	public boolean isAttributeSpecified(int index) {
		if (_currEvent != START_ELEMENT) {
			reportWrongState(ERR_STATE_NOT_START_ELEM);
		}
		Element elem = (Element) _currNode;
		Attr attr = (Attr) elem.getAttributes().item(index);
		if (attr == null) {
			handleIllegalAttrIndex(index);
			return false;
		}
		return attr.getSpecified();
	}

	public boolean isCharacters() {
		return (_currEvent == CHARACTERS);
	}

	public boolean isEndElement() {
		return (_currEvent == END_ELEMENT);
	}

	public boolean isStartElement() {
		return (_currEvent == START_ELEMENT);
	}

	public boolean isWhiteSpace() {
		if (_currEvent == CHARACTERS || _currEvent == CDATA) {
			String text = getText();
			for (int i = 0, len = text.length(); i < len; ++i) {
				/* !!! If xml 1.1 was to be handled, should check for
				 *   LSEP and NEL too?
				 */
				if (text.charAt(i) > INT_SPACE) {
					return false;
				}
			}
			return true;
		}
		return (_currEvent == SPACE);
	}

	public void require(int type, String nsUri, String localName) throws XMLStreamException {
		int curr = _currEvent;

		/* There are some special cases; specifically, SPACE and CDATA
		* are sometimes reported as CHARACTERS. Let's be lenient by
		* allowing both 'real' and reported types, for now.
		*/
		if (curr != type) {
			if (curr == CDATA) {
				curr = CHARACTERS;
			} else if (curr == SPACE) {
				curr = CHARACTERS;
			}
		}

		if (type != curr) {
			throwStreamException("Required type " + eventTypeDesc(type) + ", current type " + eventTypeDesc(curr));
		}

		if (localName != null) {
			if (curr != START_ELEMENT && curr != END_ELEMENT && curr != ENTITY_REFERENCE) {
				throwStreamException("Required a non-null local name, but current token not a START_ELEMENT, END_ELEMENT or ENTITY_REFERENCE (was "
						+ eventTypeDesc(_currEvent) + ")");
			}
			String n = getLocalName();
			if (n != localName && !n.equals(localName)) {
				throwStreamException("Required local name '" + localName + "'; current local name '" + n + "'.");
			}
		}
		if (nsUri != null) {
			if (curr != START_ELEMENT && curr != END_ELEMENT) {
				throwStreamException("Required non-null NS URI, but current token not a START_ELEMENT or END_ELEMENT (was "
						+ eventTypeDesc(curr) + ")");
			}

			String uri = getNamespaceURI();
			// No namespace?
			if (nsUri.length() == 0) {
				if (uri != null && uri.length() > 0) {
					throwStreamException("Required empty namespace, instead have '" + uri + "'.");
				}
			} else {
				if ((nsUri != uri) && !nsUri.equals(uri)) {
					throwStreamException("Required namespace '" + nsUri + "'; have '" + uri + "'.");
				}
			}
		}
		// Ok, fine, all's good
	}

	/*
	////////////////////////////////////////////////////
	// XMLStreamReader, iterating
	////////////////////////////////////////////////////
	*/

	public int next() throws XMLStreamException {
		_coalescedText = null;

		/* For most events, we just need to find the next sibling; and
		* that failing, close the parent element. But there are couple
		* of special cases, which are handled first:
		*/
		switch (_currEvent) {

		case START_DOCUMENT: // initial state
			/* What to do here depends on what kind of node we started
			* with...
			*/
			switch (_currNode.getNodeType()) {
			case Node.DOCUMENT_NODE:
			case Node.DOCUMENT_FRAGMENT_NODE:
				// For doc, fragment, need to find first child
				_currNode = _currNode.getFirstChild();
				// as per [WSTX-259], need to handle degenerate case of empty fragment, too
				if (_currNode == null) {
					return (_currEvent = END_DOCUMENT);
				}
				break;

			case Node.ELEMENT_NODE:
				// For element, curr node is fine:
				return (_currEvent = START_ELEMENT);

			default:
				throw new XMLStreamException("Internal error: unexpected DOM root node type " + _currNode.getNodeType()
						+ " for node '" + _currNode + "'");
			}
			break;

		case END_DOCUMENT: // end reached: should not call!
			throw new java.util.NoSuchElementException("Can not call next() after receiving END_DOCUMENT");

		case START_ELEMENT: // element returned, need to traverse children, if any
			++_depth;
			_attrList = null; // so it will not get reused accidentally
			{
				Node firstChild = _currNode.getFirstChild();
				if (firstChild == null) { // empty? need to return virtual END_ELEMENT
					/* Note: need not clear namespace declarations, because
					 * it'll be the same as for the start elem!
					 */
					return (_currEvent = END_ELEMENT);
				}
				_nsDeclList = null;

				/* non-empty is easy: let's just swap curr node, and
				 * fall through to regular handling
				 */
				_currNode = firstChild;
				break;
			}

		case END_ELEMENT:

			--_depth;
			// Need to clear these lists
			_attrList = null;
			_nsDeclList = null;

			/* One special case: if we hit the end of children of
			* the root element (when tree constructed with Element,
			* instead of Document or DocumentFragment). If so, it'll
			* be END_DOCUMENT:
			*/
			if (_currNode == _rootNode) {
				return (_currEvent = END_DOCUMENT);
			}
			// Otherwise need to fall through to default handling:

		default:
		/* For anything else, we can and should just get the
		* following sibling.
		*/
		{
			Node next = _currNode.getNextSibling();
			// If sibling, let's just assign and fall through
			if (next != null) {
				_currNode = next;
				break;
			}
			/* Otherwise, need to climb up _the stack and either
			 * return END_ELEMENT (if parent is element) or
			 * END_DOCUMENT (if not; needs to be root, then)
			 */
			_currNode = _currNode.getParentNode();
			int type = _currNode.getNodeType();
			if (type == Node.ELEMENT_NODE) {
				return (_currEvent = END_ELEMENT);
			}
			// Let's do sanity check; should really be Doc/DocFragment
			if (_currNode != _rootNode || (type != Node.DOCUMENT_NODE && type != Node.DOCUMENT_FRAGMENT_NODE)) {
				throw new XMLStreamException("Internal error: non-element parent node (" + type
						+ ") that is not the initial root node");
			}
			return (_currEvent = END_DOCUMENT);
		}
		}

		// Ok, need to determine current node type:
		switch (_currNode.getNodeType()) {
		case Node.CDATA_SECTION_NODE:
			if (_coalescing) {
				coalesceText(CDATA);
			} else {
				_currEvent = CDATA;
			}
			break;
		case Node.COMMENT_NODE:
			_currEvent = COMMENT;
			break;
		case Node.DOCUMENT_TYPE_NODE:
			_currEvent = DTD;
			break;
		case Node.ELEMENT_NODE:
			_currEvent = START_ELEMENT;
			break;
		case Node.ENTITY_REFERENCE_NODE:
			_currEvent = ENTITY_REFERENCE;
			break;
		case Node.PROCESSING_INSTRUCTION_NODE:
			_currEvent = PROCESSING_INSTRUCTION;
			break;
		case Node.TEXT_NODE:
			if (_coalescing) {
				coalesceText(CHARACTERS);
			} else {
				_currEvent = CHARACTERS;
			}
			break;

		// Should not get other nodes (notation/entity decl., attr)
		case Node.ATTRIBUTE_NODE:
		case Node.ENTITY_NODE:
		case Node.NOTATION_NODE:
			throw new XMLStreamException("Internal error: unexpected DOM node type " + _currNode.getNodeType()
					+ " (attr/entity/notation?), for node '" + _currNode + "'");

		default:
			throw new XMLStreamException("Internal error: unrecognized DOM node type " + _currNode.getNodeType()
					+ ", for node '" + _currNode + "'");
		}

		return _currEvent;
	}

	public int nextTag() throws XMLStreamException {
		while (true) {
			int next = next();

			switch (next) {
			case SPACE:
			case COMMENT:
			case PROCESSING_INSTRUCTION:
				continue;
			case CDATA:
			case CHARACTERS:
				if (isWhiteSpace()) {
					continue;
				}
				throwStreamException("Received non-all-whitespace CHARACTERS or CDATA event in nextTag().");
				break; // never gets here, but jikes complains without
			case START_ELEMENT:
			case END_ELEMENT:
				return next;
			}
			throwStreamException("Received event " + eventTypeDesc(next) + ", instead of START_ELEMENT or END_ELEMENT.");
		}
	}

	/**
	*<p>
	* Note: as per StAX 1.0 specs, this method does NOT close the underlying
	* input reader. That is, unless the new StAX2 property
	* {@link org.codehaus.stax2.XMLInputFactory2#P_AUTO_CLOSE_INPUT} is
	* set to true.
	*/
	public void close() throws XMLStreamException {
		// Since DOM tree has no real input source, nothing to do
	}

	/*
	////////////////////////////////////////////////////
	// NamespaceContext
	////////////////////////////////////////////////////
	*/

	public String getNamespaceURI(String prefix) {
		if (prefix.length() == 0) { // def NS
			return _currNode.lookupNamespaceURI(null);
		}
		return _currNode.lookupNamespaceURI(prefix);
	}

	public String getPrefix(String namespaceURI) {
		String prefix = _currNode.lookupPrefix(namespaceURI);
		if (prefix == null) { // maybe default NS?
			String defURI = _currNode.lookupNamespaceURI(null);
			if (defURI != null && defURI.equals(namespaceURI)) {
				return "";
			}
		}
		return prefix;
	}

	public Iterator<String> getPrefixes(String namespaceURI) {
		String prefix = getPrefix(namespaceURI);
		if (prefix == null) {
			return XmlNamespaceContext.EMPTY_ITERATOR;
		}
		return new XmlNamespaceContext.OneIterator(prefix);
	}

	/*
	////////////////////////////////////////////
	// Internal methods, text gathering
	////////////////////////////////////////////
	*/

	protected void coalesceText(int initialType) {
		_textBuffer.reset();
		_textBuffer.append(_currNode.getNodeValue());

		Node n;
		while ((n = _currNode.getNextSibling()) != null) {
			int type = n.getNodeType();
			if (type != Node.TEXT_NODE && type != Node.CDATA_SECTION_NODE) {
				break;
			}
			_currNode = n;
			_textBuffer.append(_currNode.getNodeValue());
		}
		_coalescedText = _textBuffer.get();

		// Either way, type gets always set to be CHARACTERS
		_currEvent = CHARACTERS;
	}

	/*
	////////////////////////////////////////////
	// Internal methods, namespace support
	////////////////////////////////////////////
	*/

	private QName _constructQName(String uri, String ln, String prefix) {
		// Stupid QName impls barf on nulls...
		return new QName(_internNsURI(uri), _internName(ln), _internName(prefix));
	}

	/**
	* @param attrsToo Whether to include actual attributes too, or
	*   just namespace declarations
	*/
	private void _calcNsAndAttrLists(boolean attrsToo) {
		NamedNodeMap attrsIn = _currNode.getAttributes();

		// A common case: neither attrs nor ns decls, can use short-cut
		int len = attrsIn.getLength();
		if (len == 0) {
			_attrList = Collections.EMPTY_LIST;
			_nsDeclList = Collections.EMPTY_LIST;
			return;
		}

		if (!_cfgNsAware) {
			_attrList = new ArrayList<Node>(len);
			for (int i = 0; i < len; ++i) {
				_attrList.add(attrsIn.item(i));
			}
			_nsDeclList = Collections.EMPTY_LIST;
			return;
		}

		// most should be attributes... and possibly no ns decls:
		ArrayList<Node> attrsOut = null;
		ArrayList<String> nsOut = null;

		for (int i = 0; i < len; ++i) {
			Node attr = attrsIn.item(i);
			String prefix = attr.getPrefix();

			// Prefix?
			if (prefix == null || prefix.length() == 0) { // nope
				// default ns decl?
				if (!"xmlns".equals(attr.getLocalName())) { // nope
					if (attrsToo) {
						if (attrsOut == null) {
							attrsOut = new ArrayList<Node>(len - i);
						}
						attrsOut.add(attr);
					}
					continue;
				}
				prefix = null;
			} else { // explicit ns decl?
				if (!"xmlns".equals(prefix)) { // nope
					if (attrsToo) {
						if (attrsOut == null) {
							attrsOut = new ArrayList(len - i);
						}
						attrsOut.add(attr);
					}
					continue;
				}
				prefix = attr.getLocalName();
			}
			if (nsOut == null) {
				nsOut = new ArrayList<String>((len - i) * 2);
			}
			nsOut.add(_internName(prefix));
			nsOut.add(_internNsURI(attr.getNodeValue()));
		}

		_attrList = (attrsOut == null) ? Collections.EMPTY_LIST : attrsOut;
		_nsDeclList = (nsOut == null) ? Collections.EMPTY_LIST : nsOut;
	}

	private void handleIllegalAttrIndex(int index) {
		Element elem = (Element) _currNode;
		NamedNodeMap attrs = elem.getAttributes();
		int len = attrs.getLength();
		String msg = "Illegal attribute index " + index + "; element <" + elem.getNodeName() + "> has "
				+ ((len == 0) ? "no" : String.valueOf(len)) + " attributes";
		throw new IllegalArgumentException(msg);
	}

	private void handleIllegalNsIndex(int index) {
		String msg = "Illegal namespace declaration index " + index + " (has " + getNamespaceCount() + " ns declarations)";
		throw new IllegalArgumentException(msg);
	}

	/**
	* Due to differences in how namespace-aware and non-namespace modes
	* work in DOM, different methods are needed. We may or may not be
	* able to detect namespace-awareness mode of the source Nodes
	* directly; but at any rate, should contain some logic for handling
	* problem cases.
	*/
	private String _safeGetLocalName(Node n) {
		String ln = n.getLocalName();
		if (ln == null) {
			ln = n.getNodeName();
		}
		return ln;
	}

	/*
	///////////////////////////////////////////////
	// Overridable error reporting methods
	///////////////////////////////////////////////
	*/

	protected void reportWrongState(int errorType) {
		throw new IllegalStateException(findErrorDesc(errorType, _currEvent));
	}

	protected void reportParseProblem(int errorType) throws XMLStreamException {
		throwStreamException(findErrorDesc(errorType, _currEvent));
	}

	protected void throwStreamException(String msg) throws XMLStreamException {
		throwStreamException(msg, getErrorLocation());
	}

	protected Location getErrorLocation() {
		return NOT_AVAILABLE;
	}

	/**
	* Method used to locate error message description to use.
	* Calls sub-classes <code>findErrorDesc()</code> first, and only
	* if no message found, uses default messages defined here.
	*/
	protected String findErrorDesc(int errorType, int currEvent) {
		String evtDesc = eventTypeDesc(currEvent);
		switch (errorType) {
		case ERR_STATE_NOT_START_ELEM:
			return "Current event " + evtDesc + ", needs to be START_ELEMENT";
		case ERR_STATE_NOT_ELEM:
			return "Current event " + evtDesc + ", needs to be START_ELEMENT or END_ELEMENT";
		case ERR_STATE_NO_LOCALNAME:
			return "Current event (" + evtDesc + ") has no local name";
		case ERR_STATE_NOT_PI:
			return "Current event (" + evtDesc + ") needs to be PROCESSING_INSTRUCTION";

		case ERR_STATE_NOT_TEXTUAL:
			return "Current event (" + evtDesc + ") not a textual event";
		case ERR_STATE_NOT_TEXTUAL_OR_ELEM:
			return "Current event (" + evtDesc + " not START_ELEMENT, END_ELEMENT, CHARACTERS or CDATA";
		case ERR_STATE_NOT_TEXTUAL_XXX:
			return "Current event " + evtDesc + ", needs to be one of CHARACTERS, CDATA, SPACE or COMMENT";
		}
		// should never happen, but it'd be bad to throw another exception...
		return "Internal error (unrecognized error type: " + errorType + ")";
	}

	/**
	* Method called to do additional intern()ing for a name, if and as
	* necessary
	*/
	protected String _internName(String name) {
		if (name == null) {
			return "";
		}
		return _cfgInternNames ? name.intern() : name;
	}

	protected String _internNsURI(String uri) {
		if (uri == null) {
			return "";
		}
		return _cfgInternNsURIs ? uri.intern() : uri;
	}

	public static String eventTypeDesc(int type) {
		switch (type) {
		case START_ELEMENT:
			return "START_ELEMENT";
		case END_ELEMENT:
			return "END_ELEMENT";
		case START_DOCUMENT:
			return "START_DOCUMENT";
		case END_DOCUMENT:
			return "END_DOCUMENT";

		case CHARACTERS:
			return "CHARACTERS";
		case CDATA:
			return "CDATA";
		case SPACE:
			return "SPACE";

		case COMMENT:
			return "COMMENT";
		case PROCESSING_INSTRUCTION:
			return "PROCESSING_INSTRUCTION";
		case DTD:
			return "DTD";
		case ENTITY_REFERENCE:
			return "ENTITY_REFERENCE";
		}
		return "[" + type + "]";
	}

	public final static class TextBuffer {
		private String mText = null;

		/* !!! JDK 1.5: when we can upgrade to Java 5, can convert
		 *  to using <code>StringBuilder</code> instead.
		 */
		private StringBuffer mBuilder = null;

		public TextBuffer() {
		}

		public void reset() {
			mText = null;
			mBuilder = null;
		}

		public void append(String text) {
			int len = text.length();
			if (len > 0) {
				// Any prior text?
				if (mText != null) {
					mBuilder = new StringBuffer(mText.length() + len);
					mBuilder.append(mText);
					mText = null;
				}
				if (mBuilder != null) {
					mBuilder.append(text);
				} else {
					mText = text;
				}
			}
		}

		public String get() {
			if (mText != null) {
				return mText;
			}
			if (mBuilder != null) {
				return mBuilder.toString();
			}
			return "";
		}

		public boolean isEmpty() {
			return (mText == null) && (mBuilder == null);
		}
	}

}
