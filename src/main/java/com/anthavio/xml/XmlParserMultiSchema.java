package com.anthavio.xml;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.validation.Schema;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.anthavio.io.ResetableReader;
import com.anthavio.xml.validation.XmlSchemaCollection;
import com.anthavio.xml.validation.XmlValidationException;

/**
 * 
 * @author vanek
 *
 */
public class XmlParserMultiSchema extends XmlAbstractParser {

	protected XmlSchemaCollection schemas;

	private JaxpSaxFactory saxFactoryTemplate;

	private Map<String, JaxpSaxFactory> saxFactoryMap = new HashMap<String, JaxpSaxFactory>();

	private XmlSchemaNameResolver xmlSchemaNameResolver = FirstElementNameResolver.INSTANCE;

	public XmlParserMultiSchema(String schemaResource) {
		URL schemaUrl = Thread.currentThread().getContextClassLoader().getResource(schemaResource);
		if (schemaUrl == null) {
			throw new IllegalArgumentException("Schema resource is invalid " + schemaResource);
		}
		schemas = new XmlSchemaCollection(schemaUrl, true);
	}

	public XmlParserMultiSchema(String schemaResource, boolean schemaEagerLoad) {
		URL schemaUrl = Thread.currentThread().getContextClassLoader().getResource(schemaResource);
		if (schemaUrl == null) {
			throw new IllegalArgumentException("Schema resource is invalid " + schemaResource);
		}
		schemas = new XmlSchemaCollection(schemaUrl, schemaEagerLoad);
	}

	public XmlParserMultiSchema(URL schemaUrl) {
		if (schemaUrl == null) {
			throw new IllegalArgumentException("schemaUrl parameter must not be null");
		}
		schemas = new XmlSchemaCollection(schemaUrl, true);
	}

	public XmlParserMultiSchema(URL schemaUrl, boolean schemaEagerLoad) {
		if (schemaUrl == null) {
			throw new IllegalArgumentException("schemaUrl parameter must not be null");
		}
		//if (schemaUrl.getPath().endsWith(".xsd")) {
		schemas = new XmlSchemaCollection(schemaUrl, schemaEagerLoad);
	}

	public XmlSchemaNameResolver getSchemaNameDetector() {
		return xmlSchemaNameResolver;
	}

	public void setSchemaNameDetector(XmlSchemaNameResolver xmlSchemaNameResolver) {
		this.xmlSchemaNameResolver = xmlSchemaNameResolver;
	}

	public void setSaxFactoryTemplate(JaxpSaxFactory saxFactoryTemplate) {
		this.saxFactoryTemplate = saxFactoryTemplate;
	}

	protected JaxpSaxFactory getSaxFactory(String schemaName) {
		JaxpSaxFactory saxFactory = saxFactoryMap.get(schemaName);
		if (saxFactory == null) {
			Schema schema = getSchema(schemaName);
			if (saxFactoryTemplate != null) {
				saxFactory = new JaxpSaxFactory(saxFactoryTemplate.getFactoryClassName());
				saxFactory.setFactoryCached(true);
				saxFactory.setSchema(schema);
				saxFactory.setxIncludeAware(saxFactoryTemplate.getxIncludeAware());
				saxFactory.setFactoryFeatures(saxFactoryTemplate.getFactoryFeatures());
				saxFactory.setParserProperties(saxFactoryTemplate.getParserProperties());
			} else {
				saxFactory = new JaxpSaxFactory();
				saxFactory.setFactoryCached(true);
				saxFactory.setSchema(schema);
			}
		}
		return saxFactory;
	}

	public Schema getSchema(String schemaName) throws XmlValidationException {
		Schema schema = schemas.getSchema(schemaName);
		if (schema == null) {
			throw new IllegalArgumentException("Schema not found: " + schemaName);
		}
		return schema;
	}

	public Document parse(String xml) throws XmlValidationException {
		ResetableReader reader = new ResetableReader(new StringReader(xml), 1024);
		String schemaName = getSchemaNameDetector().getSchemaName(reader, getStaxInputFactory().getFactory());
		Schema schema = getSchema(schemaName);
		return parseStax(reader, schema);

		//XMLReader xmlReader = getSaxFactory(rootElementName).getXMLReader();
		//parseSax(new StringReader(xml), xmlReader, errorHandler);

	}

	public void write(Node node, Writer writer) {
		Schema schema = getSchema(node.getLocalName());
		super.write(node, writer, schema);
	}

	public String toString(Node node) throws XmlValidationException {
		StringWriter writer = new StringWriter();
		write(node, writer);
		return writer.toString();
	}

}
