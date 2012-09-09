/**
 * 
 */
package com.anthavio.xml.stax;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;

/**
 * @author vanek
 *
 */
public class ResetableStaxEventReader implements XMLEventReader {

	//private static final Logger log = LoggerFactory.getLogger(ResetStaxEventReader.class);

	private XMLEventReader parent;

	private List<XMLEvent> events;

	private int maxSize;

	private int rereadIdx = -1;

	private boolean owerflow = false;

	public ResetableStaxEventReader(Source source, int size) throws XMLStreamException {
		this(XMLInputFactory.newFactory().createXMLEventReader(source), size);
	}

	public ResetableStaxEventReader(Reader reader, int size) throws XMLStreamException {
		this(XMLInputFactory.newFactory().createXMLEventReader(reader), size);
	}

	public ResetableStaxEventReader(InputStream stream, String encoding, int size) throws XMLStreamException {
		this(XMLInputFactory.newFactory().createXMLEventReader(stream, encoding), size);
	}

	public ResetableStaxEventReader(InputStream stream, int size) throws XMLStreamException {
		this(XMLInputFactory.newFactory().createXMLEventReader(stream), size);
	}

	public ResetableStaxEventReader(XMLEventReader parent, int size) {
		if (parent == null) {
			throw new IllegalArgumentException("Null parent XMLEventReader");
		}
		this.parent = parent;
		if (size <= 0) {
			throw new IllegalArgumentException("Size must be positive number");
		}
		this.maxSize = size;
		events = new ArrayList<XMLEvent>(size);
	}

	public XMLEvent nextEvent() throws XMLStreamException {
		if (rereadIdx == -1) {
			XMLEvent event = parent.nextEvent();
			if (events.size() < maxSize) {
				events.add(event);
			} else {
				owerflow = true;
				//log.debug("Maximal event count reached. Unread will fail");
			}
			return event;
		} else {
			if (rereadIdx < events.size()) {
				return events.get(rereadIdx++);
			} else {
				return parent.nextEvent();
			}
		}
	}

	public void reset() {
		if (owerflow) {
			throw new IllegalStateException("Cannot reset. Maximal event count " + maxSize + " reached");
		}
		rereadIdx = 0;
	}

	//Iterator & XMLEventReader
	public boolean hasNext() {
		return parent.hasNext();
	}

	public XMLEvent peek() throws XMLStreamException {
		return parent.peek();
	}

	//Iterator
	public Object next() {
		return parent.next();
	}

	public String getElementText() throws XMLStreamException {
		return parent.getElementText();
	}

	//Iterator
	public void remove() {
		parent.remove();
	}

	public XMLEvent nextTag() throws XMLStreamException {
		return parent.nextTag();
	}

	public Object getProperty(String name) throws IllegalArgumentException {
		return parent.getProperty(name);
	}

	public void close() throws XMLStreamException {
		parent.close();
	}
}
