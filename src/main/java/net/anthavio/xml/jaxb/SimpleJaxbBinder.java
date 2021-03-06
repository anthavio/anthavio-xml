package net.anthavio.xml.jaxb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import net.anthavio.xml.StringSource;
import net.anthavio.xml.XmlParseException;
import net.anthavio.xml.stax.XPathEventTracker;
import net.anthavio.xml.validation.XmlErrorHandler;
import net.anthavio.xml.validation.XmlValidationException;

import org.apache.commons.lang.UnhandledException;


/**
 * @author vanek
 * 
 * TODO revisit this class
 * 
 * Maly skladny a sikovny JAXB marshaller/unmarshaller pro code first pouziti
 */
public class SimpleJaxbBinder<T> {

	private final Class<T> jaxbClass;

	private JAXBContext jaxbCtx;

	private String schemaXml;

	private Schema schema;

	public SimpleJaxbBinder(Class<T> jaxbClass) {
		this(jaxbClass, false);
	}

	public SimpleJaxbBinder(Class<T> jaxbClass, boolean useSchema) {
		if (jaxbClass == null) {
			throw new IllegalArgumentException("Null class to bind");
		}
		this.jaxbClass = jaxbClass;
		try {
			jaxbCtx = JAXBContext.newInstance(jaxbClass);
			if (useSchema) {
				SimpleSchemaOutputResolver resolver = new SimpleSchemaOutputResolver();
				jaxbCtx.generateSchema(resolver);
				schemaXml = resolver.getSchema();
				SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
				schema = schemaFactory.newSchema(new StringSource(schemaXml));
			}
		} catch (Exception x) {
			throw new UnhandledException(x);
		}
	}

	public String getSchemaXml() {
		return schemaXml;
	}

	public Class<T> getJaxbClass() {
		return jaxbClass;
	}

	public T load(InputStream stream) {
		return load(new StreamSource(stream));
	}

	public T load(Reader reader) {
		return load(new StreamSource(reader));
	}

	public T load(Source source) {
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		XMLEventReader xmlStreamReader;
		try {
			xmlStreamReader = xmlInputFactory.createXMLEventReader(source);
		} catch (XMLStreamException xsx) {
			//xsx.getLocation();
			throw new UnhandledException(xsx);
		}
		XPathEventTracker tracker = new XPathEventTracker(xmlStreamReader);
		XmlErrorHandler handler = new XmlErrorHandler(tracker);
		try {
			Unmarshaller unmarshaller = createUnmarshaller();
			unmarshaller.setEventHandler(handler);
			T value = unmarshaller.unmarshal(tracker, jaxbClass).getValue();
			if (handler.hasErrors()) {
				throw new XmlValidationException(handler.getErrors());
			}
			return value;

		} catch (JAXBException jbx) {
			//System.out.println(handler.getErrors());
			StringBuilder sb = new StringBuilder();
			if (jbx.getLinkedException() != null) {
				sb.append(jbx.getLinkedException());
			} else {
				sb.append(jbx);
			}
			if (tracker.getXPath() != null) {
				sb.append(", xpath: ");
				sb.append(tracker.getXPath());
			}
			if (tracker.getLocation() != null) {
				sb.append(", line: ");
				sb.append(tracker.getLocation().getLineNumber());
				sb.append(", column: ");
				sb.append(tracker.getLocation().getLineNumber());
			}

			throw new XmlParseException(sb.toString(), jbx);
		}

	}

	public T load(String xml) {
		return load(new StringSource(xml));
	}

	public void save(T object, Writer writer) {
		try {
			Marshaller marshaller = createMarshaller();
			marshaller.marshal(object, writer);
		} catch (JAXBException jbx) {
			throw new UnhandledException(jbx);
		}
	}

	public void save(T object, OutputStream out) {
		OutputStreamWriter writer = new OutputStreamWriter(out, Charset.forName("utf-8"));
		save(object, writer);
	}

	private Marshaller createMarshaller() {
		try {
			Marshaller marshaller = jaxbCtx.createMarshaller();
			if (schema != null) {
				marshaller.setSchema(schema);
			}
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			return marshaller;
		} catch (JAXBException jbx) {
			throw new UnhandledException(jbx);
		}
	}

	private Unmarshaller createUnmarshaller() {
		try {
			Unmarshaller unmarshaller = jaxbCtx.createUnmarshaller();
			if (schema != null) {
				unmarshaller.setSchema(schema);
			}
			return unmarshaller;
		} catch (JAXBException jbx) {
			throw new UnhandledException(jbx);
		}
	}

}

class SimpleSchemaOutputResolver extends SchemaOutputResolver {

	private File file;

	@Override
	public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
		file = File.createTempFile("sjp", "xsd");
		return new StreamResult(file);
	}

	public String getSchema() throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),
				Charset.forName("UTF-8")));
		try {
			String line = null;
			StringBuilder sb = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				sb.append(line);
				sb.append('\n');
			}
			return sb.toString();
		} finally {
			reader.close();
		}
	}
}