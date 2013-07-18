package com.anthavio.xml;

import java.io.IOException;
import java.net.URL;

import javax.xml.stream.XMLStreamException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * There are nice libraries capable using OASIS catalogs but implementing only SAX org.xml.sax.EntityResolver
 * For example Xerces XMLCatalogResolver or xml-resolver CatalogResolver 
 * This timy wrapper allows using those in Stax XMLReader
 * 
 * XMLInputFactory factory = XMLInputFactory.newInstance();
 * and
 * factory.setProperty(XMLInputFactory.RESOLVER, new XMLResolverDelegator(entityResolver));
 * or 
 * factory.setXMLResolver(new XMLResolverDelegator(entityResolver));
 * 
 * @author martin.vanek
 *
 */
public class XMLResolverDelegator implements javax.xml.stream.XMLResolver {

	private final org.xml.sax.EntityResolver delegate;

	public XMLResolverDelegator(EntityResolver delegate) {
		if (delegate == null) {
			throw new IllegalArgumentException("EntityResolver is null");
		}
		this.delegate = delegate;
	}

	public org.xml.sax.EntityResolver getDelegate() {
		return delegate;
	}

	@Override
	public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace)
			throws XMLStreamException {
		try {
			//System.out.println("resolve " + systemID);
			InputSource source = delegate.resolveEntity(publicID, systemID);
			if (source == null) {
				return null;
			} else if (source.getByteStream() != null) {
				return source.getByteStream();
			} else if (source.getCharacterStream() != null) {
				return source.getCharacterStream();
			} else if (source.getSystemId() != null) {
				return new URL(source.getSystemId());
			} else {
				throw new XMLStreamException("EntityResolver delegate returned empty InputSource " + source);
			}
		} catch (SAXException sax) {
			throw new XMLStreamException(sax);
		} catch (IOException iox) {
			throw new XMLStreamException(iox);
		}
	}
}
