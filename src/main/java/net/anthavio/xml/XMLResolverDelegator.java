package net.anthavio.xml;

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

	/**
	 * Stax uses relative systemID (for anything imported from DTD) but also provides baseURI (original DTD)
	 * SAX uses absolute systemID (because it does not have baseURI parameter)
	 * 
	 * From Stax parameters we need to compute absolute systemID for underlying SAX EntityResolver
	 * 
	 * For initial DTD file parameters looks like
	 * publicID: -//NPG//DTD XML Article//EN'
	 * systemID: NPG_XML_Article.dtd
	 * baseURI: file:/Devel/projects/macmillan/mlexperiment/
	 * namespace: null
	 * 
	 * For imported Entity file parameters looks like
	 * publicID: ISO 8879:1986//ENTITIES Numeric and Special Graphic//EN
	 * systemID: XML_entities/ISOnum.ent
	 * baseURI: file:/mnt/fs/web/NPG/dtd/npg_xml_dtd/NPG_XML_Article.dtd
	 * namespace: ISOnum
	 * 
	 * TODO make it work this for XSD schemas
	 */
	@Override
	public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace)
			throws XMLStreamException {
		try {

			InputSource source = delegate.resolveEntity(publicID, systemID);
			if (source == null) {
				String fileExtension = baseURI.substring(baseURI.length() - 4).toLowerCase();
				//System.out.println(fileExtension);
				if (fileExtension.equals(".xsd") || fileExtension.equals(".dtd")) {
					systemID = baseURI.substring(0, baseURI.lastIndexOf('/') + 1) + systemID;
				}
				//System.out.println(systemID);
				URL url = new URL(systemID);
				return url.getContent();
				//throw new XMLStreamException("Cannot resolve '" + publicID + "' '" + systemID + "' '" + baseURI + "' '"
				//		+ namespace + "'");
				//return null;
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
