package net.anthavio.xml.jaxb;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import net.anthavio.io.ResetableReader;
import net.anthavio.xml.XmlParseException;
import net.anthavio.xml.XmlParserMultiSchema;
import net.anthavio.xml.stax.Dom2StaxStreamReader;
import net.anthavio.xml.stax.StaxIndentingStreamWriter;
import net.anthavio.xml.stax.XPathStreamTracker;
import net.anthavio.xml.validation.XmlErrorHandler;
import net.anthavio.xml.validation.XmlValidationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XmlBinderMultiSchema extends XmlParserMultiSchema {

	private final JaxbContextFactory contextFactory;

	public XmlBinderMultiSchema(String jaxbPackages, String schemasResource, boolean eagerXsdLoad) {
		super(schemasResource, eagerXsdLoad);
		if (jaxbPackages == null) {
			throw new IllegalArgumentException("jaxbPackages parameter must not be null");
		}
		this.contextFactory = buildJaxbContext(jaxbPackages);
	}

	public XmlBinderMultiSchema(String jaxbPackages, String schemasResource) {
		super(schemasResource);
		if (jaxbPackages == null) {
			throw new IllegalArgumentException("jaxbPackages parameter must not be null");
		}
		this.contextFactory = buildJaxbContext(jaxbPackages);
	}

	public XmlBinderMultiSchema(String jaxbPackages, URL schemasUrl) {
		super(schemasUrl, false);
		if (jaxbPackages == null) {
			throw new IllegalArgumentException("jaxbPackages parameter must not be null");
		}
		this.contextFactory = buildJaxbContext(jaxbPackages);
	}

	public XmlBinderMultiSchema(String jaxbPackages, URL schemasUrl, boolean eagerXsdLoad) {
		super(schemasUrl, eagerXsdLoad);

		if (jaxbPackages == null) {
			throw new IllegalArgumentException("jaxbPackages parameter must not be null");
		}
		this.contextFactory = buildJaxbContext(jaxbPackages);
	}

	private JaxbContextFactory buildJaxbContext(String jaxbPackages) {
		log.debug("Building JAXBContext " + jaxbPackages);
		JaxbContextFactory contextFactory = new JaxbContextFactory();
		contextFactory.setContextPath(jaxbPackages);
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
			XMLStreamWriter staxStreamWriter = getStaxOutputFactory().getFactory().createXMLStreamWriter(result);
			if (getIndenting() && result instanceof StreamResult) {
				staxStreamWriter = new StaxIndentingStreamWriter(staxStreamWriter);
			}
			marshal(jaxbObject, staxStreamWriter);
		} catch (XMLStreamException xsx) {
			throw new XmlParseException(xsx);
		}
	}

	public String marshal(Object jaxbObject) throws XmlValidationException {
		StringWriter writer = new StringWriter();
		marshal(jaxbObject, writer);
		return writer.toString();
	}

	private void marshal(Object jaxbObject, XMLStreamWriter staxStreamWriter) {
		String schemaName = jaxbObject.getClass().getSimpleName();
		Schema xmlSchema = getSchema(schemaName);
		try {
			XPathStreamTracker xpathTracker = new XPathStreamTracker(staxStreamWriter);

			XmlErrorHandler errorHandler = new XmlErrorHandler(getFastFail());
			errorHandler.setXPathTracker(xpathTracker);

			Marshaller marshaller = contextFactory.createMarshaller();
			marshaller.setSchema(xmlSchema);
			marshaller.setEventHandler(errorHandler);
			//JAXB_FORMATTED_OUTPUT works only when marshaling into OutputStream/Writer
			//Never use it because of pipelining through XPathStreamTracker
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);

			marshaller.marshal(jaxbObject, xpathTracker);

			if (errorHandler.hasErrors()) {
				throw new XmlValidationException(errorHandler.getErrors());
			}

		} catch (JAXBException jx) {
			throw new XmlParseException(jx);
		}
	}

	//Untyped unmarshalling

	public Object unmarshal(Node node) throws XmlValidationException {
		Schema schema = getSchema(node.getLocalName());
		try {
			//DOMSource umi jen Woodstox
			Dom2StaxStreamReader dom2StaxStreamReader = new Dom2StaxStreamReader(node);
			return unmarshal(dom2StaxStreamReader, schema);
		} catch (XMLStreamException xsx) {
			throw new XmlParseException(xsx);
		}

	}

	public Object unmarshal(String xmlString) throws XmlValidationException {
		return unmarshal(new StringReader(xmlString));
	}

	public Object unmarshal(Reader reader) throws XmlValidationException {
		ResetableReader rereader = new ResetableReader(reader, 1024);
		String schemaName = getSchemaNameDetector().getSchemaName(rereader, getStaxInputFactory().getFactory());
		Schema schema = getSchema(schemaName);
		return unmarshal(new StreamSource(rereader), schema);
	}

	private Object unmarshal(Source source, Schema schema) throws XmlValidationException {
		try {
			XMLStreamReader staxStreamReader = getStaxInputFactory().getFactory().createXMLStreamReader(source);
			return unmarshal(staxStreamReader, schema);
		} catch (XMLStreamException xsx) {
			throw new XmlParseException(xsx);
		}
	}

	private Object unmarshal(XMLStreamReader staxStreamReader, Schema schema) throws XmlValidationException {
		try {
			XPathStreamTracker xpathTracker = new XPathStreamTracker(staxStreamReader);

			XmlErrorHandler errorHandler = new XmlErrorHandler(getFastFail());
			errorHandler.setXPathTracker(xpathTracker);

			Unmarshaller unmarshaller = contextFactory.createUnmarshaller();
			unmarshaller.setSchema(schema);
			unmarshaller.setEventHandler(errorHandler);

			Object jaxbObject = unmarshaller.unmarshal(xpathTracker);
			if (errorHandler.hasErrors()) {
				throw new XmlValidationException(errorHandler.getErrors());
			}
			return jaxbObject;

		} catch (JAXBException jaxbx) {
			throw new XmlParseException(jaxbx);
		}
	}

	//Typed unmarshalling

	public <T> T unmarshal(String xmlString, Class<T> expectedType) throws XmlValidationException {
		return unmarshal(new StringReader(xmlString), expectedType);
	}

	public <T> T unmarshal(Reader reader, Class<T> expectedType) throws XmlValidationException {
		ResetableReader rereader = new ResetableReader(reader, 1024);
		String schemaName = getSchemaNameDetector().getSchemaName(rereader, getStaxInputFactory().getFactory());
		Schema schema = getSchema(schemaName);
		return unmarshal(new StreamSource(rereader), schema, expectedType);
	}

	public <T> T unmarshal(Node node, Class<T> expectedType) throws XmlValidationException {
		String schemaName = node.getLocalName();
		if (!schemaName.equals(expectedType.getSimpleName())) {
			throw new IllegalArgumentException("Schema name '" + schemaName + "' differs from expected type "
					+ expectedType.getSimpleName());
		}
		Schema schema = getSchema(schemaName);
		try {
			//DOMSource umi jen Woodstox
			Dom2StaxStreamReader dom2StaxStreamReader = new Dom2StaxStreamReader(node);
			return unmarshal(dom2StaxStreamReader, schema, expectedType);
		} catch (XMLStreamException xsx) {
			throw new XmlParseException(xsx);
		}
	}

	private <T> T unmarshal(Source source, Schema schema, Class<T> expectedType) throws XmlValidationException {
		try {
			XMLStreamReader staxStreamReader = getStaxInputFactory().getFactory().createXMLStreamReader(source);
			return unmarshal(staxStreamReader, schema, expectedType);
		} catch (XMLStreamException xsx) {
			throw new XmlParseException(xsx);
		}
	}

	private <T> T unmarshal(XMLStreamReader staxStreamReader, Schema schema, Class<T> expectedType)
			throws XmlValidationException {
		try {
			XPathStreamTracker xpathTracker = new XPathStreamTracker(staxStreamReader);

			XmlErrorHandler errorHandler = new XmlErrorHandler(getFastFail());
			errorHandler.setXPathTracker(xpathTracker);

			Unmarshaller unmarshaller = contextFactory.createUnmarshaller();
			unmarshaller.setSchema(schema);
			unmarshaller.setEventHandler(errorHandler);

			T jaxbObject = unmarshaller.unmarshal(xpathTracker, expectedType).getValue();
			if (errorHandler.hasErrors()) {
				throw new XmlValidationException(errorHandler.getErrors());
			}
			return jaxbObject;

		} catch (JAXBException jaxbx) {
			throw new XmlParseException(jaxbx);
		}
	}

}
