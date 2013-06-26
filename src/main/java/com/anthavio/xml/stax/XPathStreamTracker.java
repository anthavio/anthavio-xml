package com.anthavio.xml.stax;

import java.util.Stack;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.anthavio.xml.XPathTracker;

/**
 * @author vanek
 * 
 * Stax Stream reader/writer tracking xpath of processed stax stream
 * {@link #getXPath()} {@link #getLocation()}
 * 
 * JAXB with validation handler:
 * 
 * XPathStreamTracker tracker = new XPathStreamTracker(xmlStreamReader);
 * SilentErrorHandler handler = new SilentErrorHandler(tracker);
 * unmarshaller.setEventHandler(handler);
 * unmarshaller.unmarshal(tracker);
 */
public class XPathStreamTracker implements XMLStreamReader, XMLStreamWriter, XPathTracker {

	private final Stack<XpathHolder> histograms = new Stack<XpathHolder>();

	private XMLStreamReader parentStreamReader;

	private XMLStreamWriter parentStreamWriter;

	private Location location;

	public XPathStreamTracker(XMLStreamReader parentStreamReader) {
		this.parentStreamReader = parentStreamReader;
		histograms.push(new XpathHolder());
	}

	public XPathStreamTracker(XMLStreamWriter parentStreamWriter) {
		this.parentStreamWriter = parentStreamWriter;
		histograms.push(new XpathHolder());
	}

	public void close() throws XMLStreamException {
		parentStreamReader.close();
	}

	public String getElementText() throws XMLStreamException {
		return parentStreamReader.getElementText();
	}

	public Object getProperty(String s) throws IllegalArgumentException {
		if (parentStreamReader != null) {
			return parentStreamReader.getProperty(s);
		} else {
			return parentStreamWriter.getProperty(s);
		}
	}

	public boolean hasNext() throws XMLStreamException {
		return parentStreamReader.hasNext();
	}

	public String getXPath() {
		StringBuilder buf = new StringBuilder();
		for (XpathHolder h : histograms) {
			h.appendPath(buf);
		}
		return buf.toString();
	}

	public Location getLocation() {
		return parentStreamReader.getLocation();
	}

	public int next() throws XMLStreamException {
		int next = parentStreamReader.next();
		if (next == XMLStreamConstants.START_DOCUMENT) {
			histograms.clear();
			histograms.push(new XpathHolder());
		} else if (next == XMLStreamConstants.START_ELEMENT) {
			histograms.peek().update(parentStreamReader.getNamespaceURI(), parentStreamReader.getLocalName(),
					parentStreamReader.getPrefix());
			histograms.push(new XpathHolder());
		} else if (next == XMLStreamConstants.END_ELEMENT) {
			histograms.pop();
		}
		return next;
	}

	public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
		parentStreamReader.require(type, namespaceURI, localName);
	}

	public int nextTag() throws XMLStreamException {
		return parentStreamReader.nextTag();
	}

	public boolean isStartElement() {
		return parentStreamReader.isStartElement();
	}

	public boolean isEndElement() {
		return parentStreamReader.isEndElement();
	}

	public boolean isCharacters() {
		return parentStreamReader.isCharacters();
	}

	public boolean isWhiteSpace() {
		return parentStreamReader.isWhiteSpace();
	}

	public String getAttributeValue(String namespaceURI, String localName) {
		return parentStreamReader.getAttributeValue(namespaceURI, localName);
	}

	public int getAttributeCount() {
		return parentStreamReader.getAttributeCount();
	}

	public QName getAttributeName(int index) {
		return parentStreamReader.getAttributeName(index);
	}

	public String getAttributeNamespace(int index) {
		return parentStreamReader.getAttributeNamespace(index);
	}

	public String getAttributeLocalName(int index) {
		return parentStreamReader.getAttributeLocalName(index);
	}

	public String getAttributePrefix(int index) {
		return parentStreamReader.getAttributePrefix(index);
	}

	public String getAttributeType(int index) {
		return parentStreamReader.getAttributeType(index);
	}

	public String getAttributeValue(int index) {
		return parentStreamReader.getAttributeValue(index);
	}

	public boolean isAttributeSpecified(int index) {
		return parentStreamReader.isAttributeSpecified(index);
	}

	public int getNamespaceCount() {
		return parentStreamReader.getNamespaceCount();
	}

	public String getNamespacePrefix(int index) {
		return parentStreamReader.getNamespacePrefix(index);
	}

	public String getNamespaceURI() {
		return parentStreamReader.getNamespaceURI();
	}

	public String getNamespaceURI(String prefix) {
		return parentStreamReader.getNamespaceURI();
	}

	public String getNamespaceURI(int index) {
		return parentStreamReader.getNamespaceURI(index);
	}

	public NamespaceContext getNamespaceContext() {
		if (parentStreamWriter != null) {
			return parentStreamWriter.getNamespaceContext();
		} else {
			return parentStreamReader.getNamespaceContext();
		}
	}

	public int getEventType() {
		return parentStreamReader.getEventType();
	}

	public String getText() {
		return parentStreamReader.getText();
	}

	public char[] getTextCharacters() {
		return parentStreamReader.getTextCharacters();
	}

	public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
		return parentStreamReader.getTextCharacters(sourceStart, target, targetStart, length);
	}

	public int getTextStart() {
		return parentStreamReader.getTextStart();
	}

	public int getTextLength() {
		return parentStreamReader.getTextLength();
	}

	public boolean hasText() {
		return parentStreamReader.hasText();
	}

	public QName getName() {
		return parentStreamReader.getName();
	}

	public String getLocalName() {
		return parentStreamReader.getLocalName();
	}

	public boolean hasName() {
		return parentStreamReader.hasName();
	}

	public String getPrefix() {
		return parentStreamReader.getPrefix();
	}

	public String getPITarget() {
		return parentStreamReader.getPITarget();
	}

	public String getPIData() {
		return parentStreamReader.getPIData();
	}

	public String getEncoding() {
		return parentStreamReader.getEncoding();
	}

	public String getVersion() {
		return parentStreamReader.getVersion();
	}

	public boolean isStandalone() {
		return parentStreamReader.isStandalone();
	}

	public boolean standaloneSet() {
		return parentStreamReader.standaloneSet();
	}

	public String getCharacterEncodingScheme() {
		return parentStreamReader.getCharacterEncodingScheme();
	}

	//////////////////////////////////////
	// javax.xml.stream.XMLStreamWriter //
	//////////////////////////////////////

	public void writeStartDocument() throws XMLStreamException {
		parentStreamWriter.writeStartDocument();
		histograms.clear();
		histograms.push(new XpathHolder());
	}

	public void writeStartDocument(String version) throws XMLStreamException {
		parentStreamWriter.writeStartDocument(version);
		histograms.clear();
		histograms.push(new XpathHolder());
	}

	public void writeStartDocument(String encoding, String version) throws XMLStreamException {
		parentStreamWriter.writeStartDocument(encoding, version);
		histograms.clear();
		histograms.push(new XpathHolder());
	}

	public void writeStartElement(String localName) throws XMLStreamException {
		parentStreamWriter.writeStartElement(localName);
		histograms.peek().update(null, localName, null);
		histograms.push(new XpathHolder());
	}

	public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
		parentStreamWriter.writeStartElement(namespaceURI, localName);
		histograms.peek().update(namespaceURI, localName, null);
		histograms.push(new XpathHolder());
	}

	public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		parentStreamWriter.writeStartElement(prefix, localName, namespaceURI);
		histograms.peek().update(namespaceURI, localName, prefix);
		histograms.push(new XpathHolder());
	}

	public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
		parentStreamWriter.writeEmptyElement(namespaceURI, localName);
	}

	public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		parentStreamWriter.writeEmptyElement(prefix, localName, namespaceURI);
	}

	public void writeEmptyElement(String localName) throws XMLStreamException {
		parentStreamWriter.writeEmptyElement(localName);
	}

	public void writeEndElement() throws XMLStreamException {
		parentStreamWriter.writeEndElement();
		histograms.pop();
	}

	public void writeEndDocument() throws XMLStreamException {
		parentStreamWriter.writeEndDocument();
	}

	public void writeAttribute(String localName, String value) throws XMLStreamException {
		parentStreamWriter.writeAttribute(localName, value);
	}

	public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
			throws XMLStreamException {
		parentStreamWriter.writeAttribute(prefix, namespaceURI, localName, value);
	}

	public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
		parentStreamWriter.writeAttribute(namespaceURI, localName, value);
	}

	public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
		parentStreamWriter.writeNamespace(prefix, namespaceURI);
	}

	public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
		parentStreamWriter.writeDefaultNamespace(namespaceURI);
	}

	public void writeDTD(String dtd) throws XMLStreamException {
		parentStreamWriter.writeDTD(dtd);
	}

	public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
		parentStreamWriter.setNamespaceContext(context);
	}

	public void setDefaultNamespace(String uri) throws XMLStreamException {
		parentStreamWriter.setDefaultNamespace(uri);
	}

	public void writeProcessingInstruction(String target) throws XMLStreamException {
		parentStreamWriter.writeProcessingInstruction(target);
	}

	public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
		parentStreamWriter.writeProcessingInstruction(target, data);
	}

	public String getPrefix(String uri) throws XMLStreamException {
		return parentStreamWriter.getPrefix(uri);
	}

	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		parentStreamWriter.setPrefix(prefix, uri);
	}

	public void writeCData(String data) throws XMLStreamException {
		parentStreamWriter.writeCData(data);
	}

	public void writeCharacters(String text) throws XMLStreamException {
		parentStreamWriter.writeCharacters(text);
	}

	public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
		parentStreamWriter.writeCharacters(text, start, len);
	}

	public void writeComment(String data) throws XMLStreamException {
		parentStreamWriter.writeComment(data);
	}

	public void writeEntityRef(String name) throws XMLStreamException {
		parentStreamWriter.writeEntityRef(name);
	}

	public void flush() throws XMLStreamException {
		parentStreamWriter.flush();
	}

}