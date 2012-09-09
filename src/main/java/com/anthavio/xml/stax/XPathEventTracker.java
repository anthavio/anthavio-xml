package com.anthavio.xml.stax;

import java.util.Stack;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.EventFilter;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.anthavio.xml.XPathTracker;


/**
 * @author vanek
 * 
 * Sax filter and Stax reader/writer tracking xpath of processed xml
 * {@link #getXPath()} {@link #getLocation()}
 */
public class XPathEventTracker implements XMLEventReader, XMLEventWriter, EventFilter, XPathTracker {

	private final Stack<XpathHolder> histograms = new Stack<XpathHolder>();

	private XMLEventReader parentEventReader;

	private XMLEventWriter parentEventWriter;

	private Location location;

	/**
	 * Empty contructor for javax.xml.stream.EventFilter usage
	 */
	public XPathEventTracker() {

	}

	public XPathEventTracker(XMLEventWriter parentEvenWriter) {
		this.parentEventWriter = parentEvenWriter;
	}

	public XPathEventTracker(XMLEventReader parentEventReader) {
		this.parentEventReader = parentEventReader;
	}

	//javax.xml.stream.EventFilter

	public boolean accept(XMLEvent event) {
		if (event.isStartDocument()) {
			histograms.clear();
			histograms.push(new XpathHolder());
		} else if (event.isStartElement()) {
			StartElement element = event.asStartElement();
			QName qName = element.getName();
			histograms.peek().update(qName.getNamespaceURI(), qName.getLocalPart(), qName.getPrefix());
			histograms.push(new XpathHolder());
			this.location = event.getLocation();

		} else if (event.isEndElement()) {
			histograms.pop();
		}
		return true;
	}

	// javax.xml.stream.XMLEventReader

	public void close() throws XMLStreamException {
		parentEventReader.close();
	}

	public String getElementText() throws XMLStreamException {
		return parentEventReader.getElementText();
	}

	public Object getProperty(String s) {
		return parentEventReader.getProperty(s);
	}

	public boolean hasNext() {
		return parentEventReader.hasNext();
	}

	public XMLEvent nextEvent() throws XMLStreamException {
		XMLEvent event = parentEventReader.nextEvent();
		if (event.isStartDocument()) {
			histograms.clear();
			histograms.push(new XpathHolder());
		} else if (event.isStartElement()) {
			StartElement element = event.asStartElement();
			QName qName = element.getName();
			histograms.peek().update(qName.getNamespaceURI(), qName.getLocalPart(), qName.getPrefix());
			histograms.push(new XpathHolder());
			this.location = event.getLocation();

		} else if (event.isEndElement()) {
			histograms.pop();
		}
		return event;
	}

	public XMLEvent nextTag() throws XMLStreamException {
		return parentEventReader.nextTag();
	}

	public XMLEvent peek() throws XMLStreamException {
		return parentEventReader.peek();
	}

	public Object next() {
		return parentEventReader.next();
	}

	public void remove() {
		parentEventReader.remove();
	}

	public String getXPath() {
		StringBuilder buf = new StringBuilder();
		for (XpathHolder h : histograms) {
			h.appendPath(buf);
		}
		return buf.toString();
	}

	public Location getLocation() {
		return this.location;
	}

	//Stax XMLEventWriter

	public void add(XMLEvent event) throws XMLStreamException {
		if (event.isStartDocument()) {
			histograms.clear();
			histograms.push(new XpathHolder());
		} else if (event.isStartElement()) {
			StartElement element = event.asStartElement();
			QName qName = element.getName();
			histograms.peek().update(qName.getNamespaceURI(), qName.getLocalPart(), qName.getPrefix());
			histograms.push(new XpathHolder());
			this.location = event.getLocation();

		} else if (event.isEndElement()) {
			histograms.pop();
		}
		parentEventWriter.add(event);
	}

	public void add(XMLEventReader reader) throws XMLStreamException {
		parentEventWriter.add(reader);
	}

	public void flush() throws XMLStreamException {
		parentEventWriter.flush();
	}

	public NamespaceContext getNamespaceContext() {
		return parentEventWriter.getNamespaceContext();
	}

	public String getPrefix(String uri) throws XMLStreamException {
		return parentEventWriter.getPrefix(uri);
	}

	public void setDefaultNamespace(String uri) throws XMLStreamException {
		parentEventWriter.setDefaultNamespace(uri);
	}

	public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
		parentEventWriter.setNamespaceContext(context);
	}

	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		parentEventWriter.setPrefix(prefix, uri);
	}

}
