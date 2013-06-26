/**
 * 
 */
package com.anthavio.xml.stax;

import java.util.Stack;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author vanek
 *
 */
public class StaxIndentingStreamWriter implements XMLStreamWriter {

	private enum State {
		SEEN_NOTHING, SEEN_ELEMENT, SEEN_DATA;
	}

	private State state = State.SEEN_NOTHING;
	private Stack<State> stateStack = new Stack<State>();

	private String indentStep = "  ";
	private int depth = 0;

	private XMLStreamWriter writer;

	public StaxIndentingStreamWriter(XMLStreamWriter writer) {
		this.writer = writer;
	}

	public void setIndentStep(String s) {
		this.indentStep = s;
	}

	private void onStartElement() throws XMLStreamException {
		stateStack.push(State.SEEN_ELEMENT);
		state = State.SEEN_NOTHING;
		if (depth > 0) {
			writer.writeCharacters("\n");
		}
		doIndent();
		depth++;
	}

	private void onEndElement() throws XMLStreamException {
		depth--;
		if (state == State.SEEN_ELEMENT) {
			writer.writeCharacters("\n");
			doIndent();
		}
		state = stateStack.pop();
	}

	private void onEmptyElement() throws XMLStreamException {
		state = State.SEEN_ELEMENT;
		if (depth > 0) {
			writer.writeCharacters("\n");
		}
		doIndent();
	}

	/**
	 * Print indentation for the current level.
	 *
	 * @exception org.xml.sax.SAXException If there is an error
	 *            writing the indentation characters, or if a filter
	 *            further down the chain raises an exception.
	 */
	private void doIndent() throws XMLStreamException {
		if (depth > 0) {
			for (int i = 0; i < depth; i++) {
				writer.writeCharacters(indentStep);
			}
		}
	}

	public void writeStartDocument() throws XMLStreamException {
		writer.writeStartDocument();
		writer.writeCharacters("\n");
	}

	public void writeStartDocument(String version) throws XMLStreamException {
		writer.writeStartDocument(version);
		writer.writeCharacters("\n");
	}

	public void writeStartDocument(String encoding, String version) throws XMLStreamException {
		writer.writeStartDocument(encoding, version);
		writer.writeCharacters("\n");
	}

	public void writeStartElement(String localName) throws XMLStreamException {
		onStartElement();
		writer.writeStartElement(localName);
	}

	public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
		onStartElement();
		writer.writeStartElement(namespaceURI, localName);
	}

	public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		onStartElement();
		writer.writeStartElement(prefix, localName, namespaceURI);
	}

	public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
		onEmptyElement();
		writer.writeEmptyElement(namespaceURI, localName);
	}

	public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
		onEmptyElement();
		writer.writeEmptyElement(prefix, localName, namespaceURI);
	}

	public void writeEmptyElement(String localName) throws XMLStreamException {
		onEmptyElement();
		writer.writeEmptyElement(localName);
	}

	public void writeEndElement() throws XMLStreamException {
		onEndElement();
		writer.writeEndElement();
	}

	public void writeCharacters(String text) throws XMLStreamException {
		state = State.SEEN_DATA;
		writer.writeCharacters(text);
	}

	public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
		state = State.SEEN_DATA;
		writer.writeCharacters(text, start, len);
	}

	public void writeCData(String data) throws XMLStreamException {
		state = State.SEEN_DATA;
		writer.writeCData(data);
	}

	public void writeEndDocument() throws XMLStreamException {
		writer.writeEndDocument();
	}

	public void close() throws XMLStreamException {
		writer.close();
	}

	public void flush() throws XMLStreamException {
		writer.flush();
	}

	public void writeAttribute(String localName, String value) throws XMLStreamException {
		writer.writeAttribute(localName, value);
	}

	public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
			throws XMLStreamException {
		writer.writeAttribute(prefix, namespaceURI, localName, value);

	}

	public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
		writer.writeAttribute(namespaceURI, localName, value);
	}

	public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
		writer.writeNamespace(prefix, namespaceURI);
	}

	public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
		writer.writeDefaultNamespace(namespaceURI);
	}

	public void writeComment(String data) throws XMLStreamException {
		writer.writeComment(data);
	}

	public void writeProcessingInstruction(String target) throws XMLStreamException {
		writer.writeProcessingInstruction(target);
	}

	public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
		writer.writeProcessingInstruction(target, data);

	}

	public void writeDTD(String dtd) throws XMLStreamException {
		writer.writeDTD(dtd);
	}

	public void writeEntityRef(String name) throws XMLStreamException {
		writer.writeEntityRef(name);
	}

	public String getPrefix(String uri) throws XMLStreamException {
		return writer.getPrefix(uri);
	}

	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		writer.setPrefix(prefix, uri);
	}

	public void setDefaultNamespace(String uri) throws XMLStreamException {
		writer.setDefaultNamespace(uri);
	}

	public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
		writer.setNamespaceContext(context);
	}

	public NamespaceContext getNamespaceContext() {
		return writer.getNamespaceContext();
	}

	public Object getProperty(String name) throws IllegalArgumentException {
		return writer.getProperty(name);
	}
}
