/**
 * 
 */
package com.anthavio.xml;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;

/**
 * @author vanek
 *
 * http://jaxp.java.net/1.4/JAXP-Compatibility.html#dom
 * 
 * DocumentBuilderFactory instance CAN be cached and reused in thread safe manner,
 * but after creation it MUST NOT be modified via setters (setSchema() for example)  
 */
public class JaxpDomFactory extends JaxpAbstractFactory<DocumentBuilderFactory> {

	public enum DomImplementation {
		JDK(DOM_FACTORY_JAXP), XERCES(DOM_FACTORY_XERCES);
		private final String factoryClassName;

		private DomImplementation(String factoryClassName) {
			this.factoryClassName = factoryClassName;
		}

		public String getFactoryClassName() {
			return factoryClassName;
		}
	}

	public static final String DOM_FACTORY_JAXP = "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl";
	public static final String DOM_FACTORY_XERCES = "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl";

	//private DomImplementation implementation = DomImplementation.JDK;

	private boolean coalescing = false;

	private boolean expandEntityReferences = true;

	private boolean ignoringComments = false;

	private boolean ignoringElementContentWhitespace = false;

	private boolean namespaceAware = true;

	private boolean validating = false;

	private boolean xIncludeAware = false;

	private Schema schema;

	private HashMap<String, Boolean> factoryFeatures;

	private HashMap<String, Object> factoryAttributes;

	public JaxpDomFactory() {
		super();
	}

	public JaxpDomFactory(String factoryClassName) {
		super(factoryClassName);
	}

	public JaxpDomFactory(DomImplementation implementation) {
		super(implementation.getFactoryClassName());
	}

	public DocumentBuilder newDocumentBuilder(ErrorHandler errorHandler) {
		DocumentBuilder builder = newDocumentBuilder();
		builder.setErrorHandler(errorHandler);
		return builder;
	}

	public DocumentBuilder newDocumentBuilder() {
		DocumentBuilder builder;
		try {
			builder = getFactory().newDocumentBuilder();
		} catch (ParserConfigurationException pcx) {
			throw new JaxpConfigException(pcx);
		}
		return builder;
	}

	public Document newDocument() {
		return newDocumentBuilder().newDocument();
	}

	@Override
	protected DocumentBuilderFactory buildDefaultFactory() {
		return DocumentBuilderFactory.newInstance();
	}

	@Override
	protected void configureFactoryInstance(DocumentBuilderFactory factory) {
		try {
			factory.setCoalescing(coalescing);
			factory.setExpandEntityReferences(expandEntityReferences);
			factory.setIgnoringComments(ignoringComments);
			factory.setIgnoringElementContentWhitespace(ignoringElementContentWhitespace);
			factory.setNamespaceAware(namespaceAware);
			factory.setValidating(validating);
			factory.setXIncludeAware(xIncludeAware);

			if (schema != null) {
				factory.setSchema(schema);
			}

			if (factoryFeatures != null) {
				//XXX http://bdoughan.blogspot.com/2011/03/preventing-entity-expansion-attacks-in.html
				//documentFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
				Set<Entry<String, Boolean>> entrySet = factoryFeatures.entrySet();
				for (Entry<String, Boolean> entry : entrySet) {
					factory.setFeature(entry.getKey(), entry.getValue());
				}
			}

			if (factoryAttributes != null) {
				Set<Entry<String, Object>> entrySet = factoryAttributes.entrySet();
				for (Entry<String, Object> entry : entrySet) {
					factory.setAttribute(entry.getKey(), entry.getValue());
				}
			}

		} catch (ParserConfigurationException pcx) {
			throw new JaxpConfigException(pcx);
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

	public boolean isCoalescing() {
		return coalescing;
	}

	public void setCoalescing(boolean coalescing) {
		checkState();
		this.coalescing = coalescing;
	}

	public boolean isExpandEntityReferences() {
		return expandEntityReferences;
	}

	public void setExpandEntityReferences(boolean expandEntityReferences) {
		checkState();
		this.expandEntityReferences = expandEntityReferences;
	}

	public boolean isIgnoringComments() {
		return ignoringComments;
	}

	public void setIgnoringComments(boolean ignoringComments) {
		checkState();
		this.ignoringComments = ignoringComments;
	}

	public boolean isIgnoringElementContentWhitespace() {
		return ignoringElementContentWhitespace;
	}

	public void setIgnoringElementContentWhitespace(boolean ignoringElementContentWhitespace) {
		checkState();
		this.ignoringElementContentWhitespace = ignoringElementContentWhitespace;
	}

	public boolean isNamespaceAware() {
		return namespaceAware;
	}

	public void setNamespaceAware(boolean namespaceAware) {
		checkState();
		this.namespaceAware = namespaceAware;
	}

	public boolean isxIncludeAware() {
		return xIncludeAware;
	}

	public void setxIncludeAware(boolean xIncludeAware) {
		checkState();
		this.xIncludeAware = xIncludeAware;
	}

	public HashMap<String, Boolean> getFactoryFeatures() {
		return factoryFeatures;
	}

	public void setFactoryFeatures(HashMap<String, Boolean> factoryFeatures) {
		checkState();
		this.factoryFeatures = factoryFeatures;
	}

	public HashMap<String, Object> getFactoryAttributes() {
		return factoryAttributes;
	}

	public void setFactoryAttributes(HashMap<String, Object> factoryAttributes) {
		checkState();
		this.factoryAttributes = factoryAttributes;
	}

	public boolean isValidating() {
		return validating;
	}

	public void setValidating(boolean validating) {
		checkState();
		this.validating = validating;
	}

}
