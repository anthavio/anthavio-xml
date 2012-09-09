/**
 * 
 */
package com.anthavio.xml.jaxb;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.validation.Schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anthavio.NotSupportedException;
import com.anthavio.util.ResourceUtil;
import com.anthavio.xml.JaxpConfigException;

/**
 * @author vanek
 *
 * http://jaxp.java.net/1.4/JAXP-Compatibility.html#dom
 */
public class JaxbContextFactory {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public static final String JAXB_FACTORY_JDK_RI = "com.sun.xml.internal.bind.v2.runtime.JAXBContextImpl";

	public static final String JAXB_FACTORY_JAXB_RI = "com.sun.xml.bind.v2.runtime.JAXBContextImpl";

	public static final String JAXB_FACTORY_MOXY = "org.eclipse.persistence.jaxb.JAXBContextFactory";

	public enum JaxbImplementation {
		JDK_RI(JAXB_FACTORY_JDK_RI), JAXB_RI(JAXB_FACTORY_JAXB_RI), MOXY(JAXB_FACTORY_MOXY);
		private final String factoryClassName;

		private JaxbImplementation(String factoryClassName) {
			this.factoryClassName = factoryClassName;
		}

		public String getFactoryClassName() {
			return factoryClassName;
		}
	}

	private final JaxbImplementation implementation;

	private final String contextClassName;

	private Schema schema;

	private HashMap<String, ?> contextProperties;

	private String contextPath;

	private Class<?>[] classesToBeBound;

	private HashMap<String, Object> marshallerProperties;

	private HashMap<String, Object> unmarshallerProperties;

	private XmlAdapter<?, ?> xmlAdapter;

	private ValidationEventHandler errorHandler;

	//cached

	private JAXBContext context;

	public JaxbContextFactory() {
		this.implementation = JaxbImplementation.JDK_RI;
		this.contextClassName = JAXB_FACTORY_JDK_RI;
	}

	public JaxbContextFactory(JaxbImplementation implementation) {
		this.implementation = implementation;
		switch (implementation) {
		case JDK_RI:
			this.contextClassName = JAXB_FACTORY_JDK_RI;
			break;
		case JAXB_RI:
			this.contextClassName = JAXB_FACTORY_JAXB_RI;
			break;
		case MOXY:
			this.contextClassName = JAXB_FACTORY_MOXY;
			break;
		default:
			throw new NotSupportedException(implementation);
		}
	}

	public Unmarshaller createUnmarshaller(ValidationEventHandler errorHandler) {
		return createUnmarshaller(errorHandler, this.schema);
	}

	public Unmarshaller createUnmarshaller() {
		return createUnmarshaller(this.errorHandler, this.schema);
	}

	public Unmarshaller createUnmarshaller(ValidationEventHandler errorHandler, Schema schema) {
		Unmarshaller unmarshaller;
		try {
			unmarshaller = getJaxbContext().createUnmarshaller();
			unmarshaller.setSchema(schema);
			unmarshaller.setEventHandler(errorHandler);
			if (unmarshallerProperties != null) {
				Set<Entry<String, Object>> entrySet = unmarshallerProperties.entrySet();
				for (Entry<String, Object> entry : entrySet) {
					unmarshaller.setProperty(entry.getKey(), entry.getValue());
				}
			}
			if (xmlAdapter != null) {
				unmarshaller.setAdapter(xmlAdapter);
			}
		} catch (JAXBException jaxbx) {
			throw new JaxpConfigException(jaxbx);
		}
		return unmarshaller;
	}

	public Marshaller createMarshaller() {
		return createMarshaller(this.errorHandler, this.schema);
	}

	public Marshaller createMarshaller(ValidationEventHandler errorHandler) {
		return createMarshaller(errorHandler, this.schema);
	}

	public Marshaller createMarshaller(ValidationEventHandler errorHandler, Schema schema) {
		Marshaller marshaller;
		try {
			marshaller = getJaxbContext().createMarshaller();
			marshaller.setSchema(schema);
			marshaller.setEventHandler(errorHandler);
			if (marshallerProperties != null) {
				Set<Entry<String, Object>> entrySet = marshallerProperties.entrySet();
				for (Entry<String, Object> entry : entrySet) {
					marshaller.setProperty(entry.getKey(), entry.getValue());
				}
			}
			if (xmlAdapter != null) {
				marshaller.setAdapter(xmlAdapter);
			}
		} catch (JAXBException jaxbx) {
			throw new JaxpConfigException(jaxbx);
		}
		return marshaller;
	}

	public JAXBContext getJaxbContext() {
		if (this.context != null) {
			return context;
		}

		JAXBContext context = newContextInstance();

		if (this.context == null) {
			this.context = context;
		}
		return context;
	}

	private JAXBContext newContextInstance() {

		JAXBContext jaxbContext;
		try {
			if (contextPath != null && contextPath.length() != 0) {
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				jaxbContext = JAXBContext.newInstance(contextPath, classLoader, contextProperties);
			} else if (classesToBeBound != null && classesToBeBound.length != 0) {
				jaxbContext = JAXBContext.newInstance(classesToBeBound, contextProperties);
			} else {
				throw new JaxpConfigException("One of contextPath or classesToBeBound must be specified");
			}
			log.debug("Created JAXB context " + jaxbContext.getClass().getName() + " from "
					+ ResourceUtil.which(jaxbContext.getClass()));
		} catch (JAXBException jaxbx) {
			if (jaxbx.getLinkedException() != null) {
				throw new JaxpConfigException(jaxbx.getLinkedException());
			} else {
				throw new JaxpConfigException(jaxbx);
			}

		}
		return jaxbContext;
	}

	public String getContextClassName() {
		return contextClassName;
	}

	public void setSchema(Schema schema) {
		this.schema = schema;
	}

	public JaxbImplementation getImplementation() {
		return implementation;
	}

	public HashMap<String, ?> getContextProperties() {
		return contextProperties;
	}

	public void setContextProperties(HashMap<String, ?> contextProperties) {
		this.contextProperties = contextProperties;
	}

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public Class<?>[] getClassesToBeBound() {
		return classesToBeBound;
	}

	public void setClassesToBeBound(Class<?>... classesToBeBound) {
		this.classesToBeBound = classesToBeBound;
	}

	public HashMap<String, Object> getMarshallerProperties() {
		return marshallerProperties;
	}

	public void setMarshallerProperties(HashMap<String, Object> marshallerProperties) {
		this.marshallerProperties = marshallerProperties;
	}

	public HashMap<String, Object> getUnmarshallerProperties() {
		return unmarshallerProperties;
	}

	public void setUnmarshallerProperties(HashMap<String, Object> unmarshallerProperties) {
		this.unmarshallerProperties = unmarshallerProperties;
	}

	public XmlAdapter<?, ?> getXmlAdapter() {
		return xmlAdapter;
	}

	public void setXmlAdapter(XmlAdapter<?, ?> xmlAdapter) {
		this.xmlAdapter = xmlAdapter;
	}

	public Schema getSchema() {
		return schema;
	}

	public ValidationEventHandler getErrorHandler() {
		return errorHandler;
	}

	public void setErrorHandler(ValidationEventHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

}
