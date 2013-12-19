/**
 * 
 */
package net.anthavio.xml.validation;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import net.anthavio.xml.JaxpAbstractFactory;
import net.anthavio.xml.JaxpConfigException;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;



/**
 * @author vanek
 *
 */
public class JaxpSchemaFactory extends JaxpAbstractFactory<SchemaFactory> {

	public static final String SCHEMA_FACTORY_JAXP_CLASS = "com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory";
	public static final String SCHEMA_FACTORY_XERCES_CLASS = "org.apache.xerces.jaxp.validation.XMLSchemaFactory";

	public static final JaxpSchemaFactory SCHEMA_FACTORY_JAXP = new JaxpSchemaFactory();

	public enum SchemaImplementation {
		JDK(SCHEMA_FACTORY_JAXP_CLASS), XERCES(SCHEMA_FACTORY_XERCES_CLASS);

		private final String factoryClassName;

		private SchemaImplementation(String factoryClassName) {
			this.factoryClassName = factoryClassName;
		}

		public String getFactoryClassName() {
			return factoryClassName;
		}
	}

	private LSResourceResolver resourceResolver;

	private ErrorHandler errorHandler;

	private HashMap<String, Boolean> factoryFeatures;

	private HashMap<String, Object> factoryProperties;

	public JaxpSchemaFactory() {
		super();
	}

	public JaxpSchemaFactory(String factoryClassName) {
		super(factoryClassName);
	}

	public JaxpSchemaFactory(SchemaImplementation implementation) {
		super(implementation.getFactoryClassName());
	}

	public LSResourceResolver getResourceResolver() {
		return resourceResolver;
	}

	public void setResourceResolver(LSResourceResolver resourceResolver) {
		checkState();
		this.resourceResolver = resourceResolver;
	}

	public ErrorHandler getErrorHandler() {
		return errorHandler;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		checkState();
		this.errorHandler = errorHandler;
	}

	public HashMap<String, Boolean> getFactoryFeatures() {
		return factoryFeatures;
	}

	public void setFactoryFeatures(HashMap<String, Boolean> factoryFeatures) {
		checkState();
		this.factoryFeatures = factoryFeatures;
	}

	public HashMap<String, Object> getFactoryProperties() {
		return factoryProperties;
	}

	public void setFactoryProperties(HashMap<String, Object> factoryProperties) {
		checkState();
		this.factoryProperties = factoryProperties;
	}

	@Override
	protected SchemaFactory buildDefaultFactory() {
		return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	}

	@Override
	protected void configureFactoryInstance(SchemaFactory factory) {
		if (resourceResolver != null) {
			factory.setResourceResolver(resourceResolver);
		}

		if (errorHandler != null) {
			factory.setErrorHandler(errorHandler);
		}

		try {
			if (factoryFeatures != null) {
				Set<Entry<String, Boolean>> entrySet = factoryFeatures.entrySet();
				for (Entry<String, Boolean> entry : entrySet) {
					factory.setFeature(entry.getKey(), entry.getValue());
				}
			}

			if (factoryProperties != null) {
				Set<Entry<String, Object>> entrySet = factoryProperties.entrySet();
				for (Entry<String, Object> entry : entrySet) {
					factory.setProperty(entry.getKey(), entry.getValue());
				}
			}

		} catch (SAXNotRecognizedException snrx) {
			throw new JaxpConfigException(snrx);
		} catch (SAXNotSupportedException snsx) {
			throw new JaxpConfigException(snsx);
		}
	}

}
