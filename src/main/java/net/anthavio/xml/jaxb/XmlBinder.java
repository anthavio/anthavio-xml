package net.anthavio.xml.jaxb;

import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import net.anthavio.xml.StringSource;
import net.anthavio.xml.XmlParseException;
import net.anthavio.xml.XmlParser;
import net.anthavio.xml.stax.StaxIndentingStreamWriter;
import net.anthavio.xml.stax.XPathStreamTracker;
import net.anthavio.xml.validation.JaxpSchemaFactory;
import net.anthavio.xml.validation.XmlErrorHandler;
import net.anthavio.xml.validation.XmlValidationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;



public class XmlBinder extends XmlParser {

	private static final String JAXB_NSMAPPER_PROP = "com.sun.xml.bind.namespacePrefixMapper";

	private final JaxbContextFactory contextFactory;

	public XmlBinder(JaxbContextFactory contextFactory) {
		super(contextFactory.getSchema());
		this.contextFactory = contextFactory;
		//Try to find NamespaceContext
		if (contextFactory.getMarshallerProperties() != null) {
			Object prefixMapper = contextFactory.getMarshallerProperties().get(JAXB_NSMAPPER_PROP);
			if (prefixMapper != null && prefixMapper instanceof NamespaceContext) {
				setNamespaceContext((NamespaceContext) prefixMapper);
			}
		}
	}

	public XmlBinder(JaxbContextFactory contextFactory, Schema schema) {
		super(schema);
		this.contextFactory = contextFactory;
		if (this.contextFactory.getSchema() != null && this.contextFactory.getSchema() != schema) {
			log.warn("JaxbContextFactory " + contextFactory.getContextPath()
					+ " has another schema configured. It will NOT be used");
		}
		//Try to find NamespaceContext
		if (contextFactory.getMarshallerProperties() != null) {
			Object prefixMapper = contextFactory.getMarshallerProperties().get(JAXB_NSMAPPER_PROP);
			if (prefixMapper != null && prefixMapper instanceof NamespaceContext) {
				setNamespaceContext((NamespaceContext) prefixMapper);
			}
		}
	}

	public XmlBinder(String jaxbPackages, Schema schema) {
		super(schema);
		this.contextFactory = buildJaxbContext(jaxbPackages);
	}

	public XmlBinder(String jaxbPackages, String schemaResource, JaxpSchemaFactory schemaFactory) {
		super(schemaResource, schemaFactory);
		if (jaxbPackages == null) {
			throw new IllegalArgumentException("jaxbPackages parameter must not be null");
		}
		this.contextFactory = buildJaxbContext(jaxbPackages);
	}

	public XmlBinder(String jaxbPackages, URL schemaUrl, JaxpSchemaFactory schemaFactory) {
		super(schemaUrl, schemaFactory);
		if (jaxbPackages == null) {
			throw new IllegalArgumentException("jaxbPackages parameter must not be null");
		}
		this.contextFactory = buildJaxbContext(jaxbPackages);
	}

	private JaxbContextFactory buildJaxbContext(String jaxbPackages) {
		log.debug("Building JAXBContext " + jaxbPackages);
		JaxbContextFactory contextFactory = new JaxbContextFactory();
		contextFactory.setContextPath(jaxbPackages);
		contextFactory.setSchema(getSchema());//v tento moment uz musi byt inicializovane
		return contextFactory;
	}

	public JAXBContext getJaxbContext() {
		return contextFactory.getJaxbContext();
	}

	public Document convert(Object jaxbObject) throws XmlValidationException {
		DOMResult domResult = new DOMResult(newDocument());
		marshal(jaxbObject, domResult);
		return (Document) domResult.getNode();
	}

	public void marshal(Object jaxbObject, Writer writer) throws XmlValidationException {
		marshal(jaxbObject, new StreamResult(writer));
	}

	public String marshal(Object jaxbObject) throws XmlValidationException {
		StringWriter stringWriter = new StringWriter();
		marshal(jaxbObject, stringWriter);
		String ret = stringWriter.toString();
		return ret;
	}

	/**
	 * Not any javax.xml.transform.Result subclas will work. 
	 * It depends heavily on actual JAXB and Stax implementation.
	 * 
	 * @param jaxbObject JAXB Object to marshal  
	 * @param result Trax result to write into
	 * @throws XmlValidationException
	 */
	public void marshal(Object jaxbObject, Result result) throws XmlValidationException {
		try {
			XMLStreamWriter xmlStreamWriter = getStaxOutputFactory().getFactory().createXMLStreamWriter(result);
			if (getIndenting() && result instanceof StreamResult) {
				xmlStreamWriter = new StaxIndentingStreamWriter(xmlStreamWriter);
			}
			marshal(jaxbObject, xmlStreamWriter);
		} catch (XMLStreamException xsx) {
			throw new XmlParseException(xsx);
		}
	}

	private void marshal(Object jaxbObject, XMLStreamWriter xmlStreamWriter) throws XmlValidationException {
		try {
			XPathStreamTracker xpathTracker = new XPathStreamTracker(xmlStreamWriter);

			XmlErrorHandler errorHandler = new XmlErrorHandler(getFastFail());
			errorHandler.setXPathTracker(xpathTracker);

			Marshaller marshaller = contextFactory.createMarshaller();
			marshaller.setSchema(getSchema());
			marshaller.setEventHandler(errorHandler);
			//JAXB_FORMATTED_OUTPUT works only when marshaling into OutputStream/Writer
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
			if (getNamespaceContext() != null) {
				//use own namespace mapper if exist
				marshaller.setProperty(JAXB_NSMAPPER_PROP, getNamespaceContext());
			}
			marshaller.marshal(jaxbObject, xpathTracker);

			if (errorHandler.hasErrors()) {
				throw new XmlValidationException(errorHandler.getErrors());
			}

		} catch (JAXBException jaxbx) {
			jaxbx.getLinkedException();
			throw new XmlParseException(jaxbx);
		}
	}

	//Unmarshalling

	public Object unmarshal(String xmlString) throws XmlValidationException {
		return unmarshal(new StringSource(xmlString));
	}

	public <T> T unmarshal(String xmlString, Class<T> expectedType) throws XmlValidationException {
		return unmarshal(new StringSource(xmlString), expectedType);
	}

	public <T> T unmarshal(Node dom, Class<T> expectedType) throws XmlValidationException {
		return unmarshal(new DOMSource(dom), expectedType);
	}

	public Object unmarshal(Reader reader) throws XmlValidationException {
		return unmarshal(new StreamSource(reader));
	}

	public <T> T unmarshal(Reader reader, Class<T> expectedType) throws XmlValidationException {
		return unmarshal(new StreamSource(reader), expectedType);
	}

	/**
	 * It may not work for any Source subclass
	 * DOMSource and StreamSource should be fine
	 */
	private Object unmarshal(Source source) throws XmlValidationException {
		try {
			XMLStreamReader staxStreamReader = getStaxInputFactory().getFactory().createXMLStreamReader(source);
			XPathStreamTracker xpathTracker = new XPathStreamTracker(staxStreamReader);

			XmlErrorHandler errorHandler = new XmlErrorHandler(getFastFail());
			errorHandler.setXPathTracker(xpathTracker);

			Unmarshaller unmarshaller = contextFactory.createUnmarshaller();
			unmarshaller.setSchema(getSchema());
			unmarshaller.setEventHandler(errorHandler);

			Object jaxbObject = unmarshaller.unmarshal(xpathTracker);

			if (errorHandler.hasErrors()) {
				throw new XmlValidationException(errorHandler.getErrors());
			}

			return jaxbObject;

		} catch (XMLStreamException xsx) {
			throw new XmlParseException(xsx);
		} catch (JAXBException jaxbx) {
			throw new XmlParseException(jaxbx);
		}
	}

	private <T> T unmarshal(Source source, Class<T> expectedType) throws XmlValidationException {
		try {
			XMLStreamReader staxStreamReader = getStaxInputFactory().getFactory().createXMLStreamReader(source);
			XPathStreamTracker xpathTracker = new XPathStreamTracker(staxStreamReader);

			XmlErrorHandler errorHandler = new XmlErrorHandler(getFastFail());
			errorHandler.setXPathTracker(xpathTracker);

			Unmarshaller unmarshaller = contextFactory.createUnmarshaller();
			unmarshaller.setSchema(getSchema());
			unmarshaller.setEventHandler(errorHandler);

			T jaxbObject = unmarshaller.unmarshal(xpathTracker, expectedType).getValue();

			if (errorHandler.hasErrors()) {
				throw new XmlValidationException(errorHandler.getErrors());
			}
			return jaxbObject;

		} catch (XMLStreamException xsx) {
			throw new XmlParseException(xsx);
		} catch (JAXBException jaxbx) {
			throw new XmlParseException(jaxbx);
		}
	}

}
