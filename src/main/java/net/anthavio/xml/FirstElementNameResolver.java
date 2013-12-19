/**
 * 
 */
package net.anthavio.xml;

import java.io.IOException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMSource;

import net.anthavio.io.ResetableInputStream;
import net.anthavio.io.ResetableReader;
import net.anthavio.xml.stax.ResetableStaxEventReader;

/**
 * Sometimes first element name in the xml is needed for finding xml schema required for validation of that xml.
 * This collides with streaming xml processing so Resetable stream/reader is needed to "unread" 
 * 
 * @author vanek
 *
 */
public class FirstElementNameResolver implements XmlSchemaNameResolver {

	public static final FirstElementNameResolver INSTANCE = new FirstElementNameResolver();

	private FirstElementNameResolver() {
		//
	}

	public String getSchemaName(DOMSource source) {
		return source.getNode().getNodeName();
	}

	public String getSchemaName(ResetableStreamSource resetableSource, XMLInputFactory staxInputFactory) {
		try {

			XMLEventReader staxEventReader = staxInputFactory.createXMLEventReader(resetableSource);
			String schemaName = getSchemaName(staxEventReader);
			resetableSource.reset();
			return schemaName;

		} catch (XMLStreamException x) {
			throw new XmlParseException(x);
		} catch (IOException iox) {
			throw new XmlParseException(iox);
		}
	}

	public String getSchemaName(ResetableInputStream resetableStream, XMLInputFactory staxInputFactory) {
		try {

			XMLEventReader staxEventReader = staxInputFactory.createXMLEventReader(resetableStream);
			String schemaName = getSchemaName(staxEventReader);
			resetableStream.reset();
			return schemaName;

		} catch (XMLStreamException x) {
			throw new XmlParseException(x);
		} catch (IOException iox) {
			throw new XmlParseException(iox);
		}
	}

	public String getSchemaName(ResetableReader resetableReader, XMLInputFactory staxInputFactory) {
		try {

			XMLEventReader staxEventReader = staxInputFactory.createXMLEventReader(resetableReader);
			String schemaName = getSchemaName(staxEventReader);
			resetableReader.reset();
			return schemaName;

		} catch (XMLStreamException x) {
			throw new XmlParseException(x);
		} catch (IOException iox) {
			throw new XmlParseException(iox);
		}
	}

	public String getSchemaName(ResetableStaxEventReader resetableStaxReader) {
		String schemaName = getSchemaName((XMLEventReader) resetableStaxReader);
		resetableStaxReader.reset();
		return schemaName;
	}

	private String getSchemaName(XMLEventReader staxEventReader) {
		String schemaName = null;
		try {
			while (staxEventReader.hasNext()) {
				XMLEvent event = staxEventReader.peek();
				if (event.getEventType() == XMLStreamConstants.START_ELEMENT) {
					schemaName = event.asStartElement().getName().getLocalPart();
					break;
				} else {
					XMLEvent nextEvent = staxEventReader.nextEvent();
					//log.debug("Skipping Event "+nextEvent);
				}
			}
		} catch (XMLStreamException x) {
			throw new XmlParseException(x);
		}
		if (schemaName == null) {
			throw new IllegalStateException("Schema name is null");
		}
		return schemaName;
	}

}
