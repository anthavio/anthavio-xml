package net.anthavio.xml;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.validation.Schema;

import net.anthavio.xml.validation.JaxpSchemaFactory;
import net.anthavio.xml.validation.XmlSchemaLoader;
import net.anthavio.xml.validation.XmlValidationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;


/**
 * 
 * @author vanek
 *
 */
public class XmlParser extends XmlAbstractParser {

	private final Schema schema;

	//Not used when parsing Stax and/or with ValidationHandler
	private JaxpSaxFactory saxFactory;

	//We might not need this at all
	private JaxpDomFactory domFactory;

	/**
	 * JaxpSaxFactory with must be set!
	 */
	public XmlParser(JaxpSaxFactory saxFactory) {
		this.saxFactory = saxFactory;
		this.schema = saxFactory.getSchema();
		if (this.schema == null) {
			throw new IllegalArgumentException("JaxpSaxFactory schema is null");
		}
	}

	public XmlParser(Schema schema) {
		if (schema == null) {
			throw new IllegalArgumentException("Schema must not be null");
		}
		this.schema = schema;
	}

	public XmlParser(String schemaResource) {
		this(schemaResource, JaxpSchemaFactory.SCHEMA_FACTORY_JAXP);
	}

	public XmlParser(String schemaResource, JaxpSchemaFactory schemaFactory) {
		URL schemaUrl;
		if (schemaResource.startsWith("file:")) {
			try {
				schemaUrl = new URL(schemaResource);
			} catch (MalformedURLException mux) {
				throw new XmlParseException(mux);
			}
		} else {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			if (classLoader == null) {
				classLoader = getClass().getClassLoader();
			}
			schemaUrl = classLoader.getResource(schemaResource);
		}
		if (schemaUrl == null) {
			throw new IllegalArgumentException("Schema resource is invalid " + schemaResource);
		}
		this.schema = XmlSchemaLoader.load(schemaUrl, schemaFactory);
	}

	public XmlParser(URL schemaUrl, JaxpSchemaFactory schemaFactory) {
		if (schemaUrl == null) {
			throw new IllegalArgumentException("schemaUrl parameter must not be null");
		}
		this.schema = XmlSchemaLoader.load(schemaUrl, schemaFactory);
	}

	public Schema getSchema() {
		return schema;
	}

	public void setSaxFactory(JaxpSaxFactory saxFactory) {
		if (saxFactory.getSchema() == null) {
			throw new IllegalArgumentException("Factory does not have Xml Schema set");
		}
		this.saxFactory = saxFactory;
	}

	public JaxpSaxFactory getSaxFactory() {
		if (saxFactory == null) {
			if (schema == null) {
				throw new IllegalStateException("Neither Schema nor JaxpSaxFactory is configured");
			}
			saxFactory = new JaxpSaxFactory();
			saxFactory.setFactoryCached(true);
			saxFactory.setSchema(schema);
		}
		return saxFactory;
	}

	public void setDomFactory(JaxpDomFactory domFactory) {
		if (domFactory.getSchema() == null) {
			throw new IllegalArgumentException("Factory does not have Xml Schema set");
		}
		this.domFactory = domFactory;
	}

	public JaxpDomFactory getDomFactory() {
		if (domFactory == null) {
			if (schema == null) {
				throw new IllegalStateException("Neither Schema nor JaxpSaxFactory is configured");
			}
			domFactory = new JaxpDomFactory();
			domFactory.setFactoryCached(true);
			domFactory.setSchema(schema);
		}
		return domFactory;
	}

	public Document parse(Reader reader) throws XmlValidationException {
		return parseStax(reader, schema);
		//return parseSax(reader, getSaxFactory().getXMLReader());
	}

	public Document parse(String xmlString) throws XmlValidationException {
		return parseStax(new StringReader(xmlString), schema);
		//return parseSax(new StringReader(xmlString), getSaxFactory().getXMLReader());
	}

	public void validate(Node node) throws XmlValidationException {
		validate(node, schema);
	}

	public void write(Node node, Writer writer) throws XmlValidationException {
		write(node, writer, schema);
	}

	public String write(Node node) throws XmlValidationException {
		StringWriter writer = new StringWriter();
		write(node, writer);
		return writer.toString();
	}

}
