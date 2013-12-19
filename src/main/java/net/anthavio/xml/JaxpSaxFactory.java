/**
 * 
 */
package net.anthavio.xml;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;

import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

/**
 * @author vanek
 * 
 * http://jaxp.java.net/1.4/JAXP-Compatibility.html#SAX
 * 
 * SAXParserFactory instance CAN be cached and reused in thread safe manner,
 * but after creation it MUST NOT be modified via setters (setSchema() for example)
 */
public class JaxpSaxFactory extends JaxpAbstractFactory<SAXParserFactory> {

	public static final String SAX_FACTORY_JAXP = "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl";
	public static final String SAX_FACTORY_XERCES = "org.apache.xerces.jaxp.SAXParserFactoryImpl";
	public static final String SAX_FACTORY_WOODSTOX = "com.ctc.wstx.sax.WstxSAXParserFactory";

	public enum SaxImplementation {

		JDK(SAX_FACTORY_JAXP), XERCES(SAX_FACTORY_XERCES), WOODSTOX(SAX_FACTORY_WOODSTOX);

		private final String factoryClassName;

		private SaxImplementation(String factoryClassName) {
			this.factoryClassName = factoryClassName;
		}

		public String getFactoryClassName() {
			return factoryClassName;
		}

	}

	private boolean namespaceAware = true;

	private boolean xIncludeAware = false;

	private boolean validating = false; //DTD validation

	private Schema schema; //XML Schema validation

	private HashMap<String, Boolean> factoryFeatures;

	private HashMap<String, Object> parserProperties;

	private EntityResolver entityResolver;

	public JaxpSaxFactory() {
		super();
	}

	public JaxpSaxFactory(String factoryClassName) {
		super(factoryClassName);
	}

	public JaxpSaxFactory(SaxImplementation implementation) {
		super(implementation.getFactoryClassName());
	}

	public XMLReader getXMLReader() {
		XMLReader xmlReader;
		try {
			xmlReader = newSAXParser().getXMLReader();
		} catch (SAXException sx) {
			throw new JaxpConfigException(sx);
		}
		if (entityResolver != null) {
			xmlReader.setEntityResolver(entityResolver);
		}
		return xmlReader;
	}

	public XMLReader getXMLReader(ContentHandler contentHandler) {
		if (contentHandler == null) {
			throw new IllegalArgumentException("Null ContentHandler");
		}
		XMLReader xmlReader = getXMLReader();
		xmlReader.setContentHandler(contentHandler);
		return xmlReader;
	}

	public XMLReader getXMLReader(ContentHandler contentHandler, ErrorHandler errorHandler) {
		if (errorHandler == null) {
			throw new IllegalArgumentException("Null ErrorHandler");
		}
		XMLReader xmlReader = getXMLReader(contentHandler);
		return xmlReader;
	}

	public SAXParser newSAXParser() {
		SAXParser parser;
		try {
			parser = getFactory().newSAXParser();
		} catch (SAXNotRecognizedException snrx) {
			throw new JaxpConfigException(snrx);
		} catch (SAXNotSupportedException snsx) {
			throw new JaxpConfigException(snsx);
		} catch (ParserConfigurationException pcx) {
			throw new JaxpConfigException(pcx);
		} catch (SAXException sx) {
			throw new JaxpConfigException(sx);
		}

		if (parserProperties != null) {
			Set<Entry<String, Object>> entrySet = parserProperties.entrySet();
			for (Entry<String, Object> entry : entrySet) {
				try {
					parser.setProperty(entry.getKey(), entry.getValue());
				} catch (SAXNotRecognizedException snrx) {
					throw new JaxpConfigException(snrx);
				} catch (SAXNotSupportedException snsx) {
					throw new JaxpConfigException(snsx);
				}
			}
		}
		return parser;
	}

	@Override
	protected SAXParserFactory buildDefaultFactory() {
		return SAXParserFactory.newInstance();
	}

	@Override
	protected void configureFactoryInstance(SAXParserFactory factory) {
		try {
			factory.setNamespaceAware(namespaceAware);
			factory.setValidating(validating);
			factory.setSchema(schema);
			factory.setXIncludeAware(xIncludeAware);

			if (factoryFeatures != null) {
				//XXX http://bdoughan.blogspot.com/2011/03/preventing-entity-expansion-attacks-in.html
				//documentFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
				Set<Entry<String, Boolean>> entrySet = factoryFeatures.entrySet();
				for (Entry<String, Boolean> entry : entrySet) {
					factory.setFeature(entry.getKey(), entry.getValue());
				}
			}
		} catch (ParserConfigurationException pcx) {
			throw new JaxpConfigException(pcx);
		} catch (SAXNotRecognizedException snrx) {
			throw new JaxpConfigException(snrx);
		} catch (SAXNotSupportedException snsx) {
			throw new JaxpConfigException(snsx);
		}
	}

	//checked factory setters

	/**
	 * 1. set target schema
	 * 2. turn on namespaceAware
	 * 3. turn off validating (xml schemaLocation)
	 */
	public void setSchema(Schema schema) {
		checkState();
		this.schema = schema;
		if (schema != null) {
			this.namespaceAware = true;
			this.validating = false;
		}
	}

	public Schema getSchema() {
		return schema;
	}

	public HashMap<String, Boolean> getFactoryFeatures() {
		return factoryFeatures;
	}

	public void setFactoryFeatures(HashMap<String, Boolean> factoryFeatures) {
		checkState();
		this.factoryFeatures = factoryFeatures;
	}

	public void addFactoryFeature(String name, boolean value) {
		checkState();
		if (factoryFeatures == null) {
			factoryFeatures = new HashMap<String, Boolean>();
		}
		factoryFeatures.put(name, value);
	}

	public boolean getxIncludeAware() {
		return xIncludeAware;
	}

	public void setxIncludeAware(boolean xIncludeAware) {
		checkState();
		this.xIncludeAware = xIncludeAware;
	}

	public boolean getNamespaceAware() {
		return namespaceAware;
	}

	public void setNamespaceAware(boolean namespaceAware) {
		checkState();
		this.namespaceAware = namespaceAware;
	}

	public HashMap<String, Object> getParserProperties() {
		return parserProperties;
	}

	public void setParserProperties(HashMap<String, Object> parserProperties) {
		this.parserProperties = parserProperties;
	}

	public boolean isValidating() {
		return validating;
	}

	public void setValidating(boolean validating) {
		this.validating = validating;
	}

	public void setEntityResolver(EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
	}

}
