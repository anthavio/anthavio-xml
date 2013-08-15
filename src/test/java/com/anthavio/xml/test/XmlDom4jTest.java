/**
 * 
 */
package com.anthavio.xml.test;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.FileReader;
import java.io.StringWriter;
import java.util.HashMap;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBResult;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.ValidatorHandler;

import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXContentHandler;
import org.dom4j.io.SAXReader;
import org.dom4j.io.SAXWriter;
import org.dom4j.io.XMLWriter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.DOMOutputter;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.XMLReader;

import com.anthavio.xml.JaxpDomFactory;
import com.anthavio.xml.JaxpDomFactory.DomImplementation;
import com.anthavio.xml.JaxpSaxFactory;
import com.anthavio.xml.JaxpSaxFactory.SaxImplementation;
import com.anthavio.xml.JaxpTraxFactory;
import com.anthavio.xml.JaxpTraxFactory.TraxImplementation;
import com.anthavio.xml.JaxpXpathFactory;
import com.anthavio.xml.JaxpXpathFactory.XpathImplementation;
import com.anthavio.xml.XPathSaxTracker;
import com.anthavio.xml.XmlNamespaceContext;
import com.anthavio.xml.jaxb.JaxbContextFactory;
import com.anthavio.xml.jaxb.JaxbContextFactory.JaxbImplementation;
import com.anthavio.xml.stax.JaxpStaxInputFactory;
import com.anthavio.xml.stax.JaxpStaxInputFactory.StaxInputImplementation;
import com.anthavio.xml.validation.JaxpSchemaFactory;
import com.anthavio.xml.validation.JaxpSchemaFactory.SchemaImplementation;
import com.anthavio.xml.validation.SilentErrorHandler;
import com.anthavio.xml.validation.XmlSchemaLoader;

/**
 * @author vanek
 *
 */
public class XmlDom4jTest {

	JaxpDomFactory domFactory = new JaxpDomFactory(DomImplementation.XERCES);
	JaxpSaxFactory saxFactory = new JaxpSaxFactory(SaxImplementation.XERCES);
	JaxpSchemaFactory schemaFactory = new JaxpSchemaFactory(SchemaImplementation.XERCES);
	JaxpXpathFactory xpathFactory = new JaxpXpathFactory(XpathImplementation.JDK);
	JaxpTraxFactory traxFactory = new JaxpTraxFactory(TraxImplementation.JDK);
	JaxpStaxInputFactory staxFactory = new JaxpStaxInputFactory(StaxInputImplementation.WOODSTOX);

	Schema pingSchema = new XmlSchemaLoader(XmlDom4jTest.class.getResource("/schema/PingMessages.xsd")).getSchema();
	JaxbContextFactory pingContext;
	XmlNamespaceContext namespaceMapper;

	public XmlDom4jTest() {
		pingContext = new JaxbContextFactory(JaxbImplementation.JAXB_RI);
		pingContext.setContextPath("com.anthavio.example.messages");
		pingContext.setSchema(pingSchema);

		HashMap<String, String> uri2prefix = new HashMap<String, String>();
		uri2prefix.put("http://example.anthavio.com/same", "samens");
		uri2prefix.put("http://example.anthavio.com/messages", "emsg");
		uri2prefix.put("http://example.anthavio.com/types", "etyp");
		namespaceMapper = new XmlNamespaceContext(uri2prefix);

		HashMap<String, Object> marshallerProperties = new HashMap<String, Object>();
		marshallerProperties.put(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshallerProperties.put(Marshaller.JAXB_ENCODING, "utf-8");
		marshallerProperties.put("com.sun.xml.bind.namespacePrefixMapper", new XmlPrefixMapper(uri2prefix));
		pingContext.setMarshallerProperties(marshallerProperties);
	}

	@Test
	public void dom4jStream2Dom() throws Exception {

		//String -> DOM4J

		saxFactory.setSchema(pingSchema);
		//underlaying sax reader does the validation
		SAXParser saxParser = saxFactory.newSAXParser();
		XMLReader xmlReader = saxParser.getXMLReader();
		XPathSaxTracker xpathTracker = new XPathSaxTracker(xmlReader);
		SilentErrorHandler errorHandler = new SilentErrorHandler(xpathTracker);
		xpathTracker.setErrorHandler(errorHandler);
		//chain DOM4J SAXReader to SAX XMLReader
		SAXReader dom4jSaxReader = new SAXReader(xpathTracker);
		dom4jSaxReader.setErrorHandler(errorHandler);
		org.dom4j.Document dom4jDocument = dom4jSaxReader.read(new FileReader(
				"src/test/resources/xml/PingRequest-ErrTag.xml"));

		assertThat(dom4jDocument.getRootElement().getName()).isEqualTo("PingRequest");
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(6);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		//w3c DOM conversion has direct support

		//DOM4J -> DOM
		DOMWriter dom4jDomWriter = new DOMWriter();
		Document domDocument = dom4jDomWriter.write(dom4jDocument);
		assertThat(domDocument.getDocumentElement().getLocalName()).isEqualTo("PingRequest");
		//DOM -> DOM4J
		DOMReader dom4jDomReader = new DOMReader();
		org.dom4j.Document dom4jDocument2 = dom4jDomReader.read(domDocument);
		assertThat(dom4jDocument2.getRootElement().getName()).isEqualTo("PingRequest");

		//JAXB support is limited http://kickjava.com/src/org/dom4j/samples/jaxb/JAXBDemo.java.htm
		//dom4j JAXBReader does JAXB -> dom4j Document -> dom4j XMLWriter
		//JAXBReader jaxbReader = new JAXBReader(null);
		//jaxbReader.read(null);
		//dom4j JAXBWriter does JAXB -> dom4j Document -> dom4j XMLWriter
		//JAXBWriter jaxbWriter = new JAXBWriter(null);
		//jaxbWriter.write(null);

		//DOM4J -> String
		StringWriter stringWriter = new StringWriter();
		XMLWriter dom4jOutWriter = new XMLWriter(stringWriter, OutputFormat.createPrettyPrint());

		ValidatorHandler validatorHandler = pingSchema.newValidatorHandler();
		errorHandler.reset();
		validatorHandler.setErrorHandler(errorHandler);
		//SAX ContentHandler -> DOM4J XMLWriter
		validatorHandler.setContentHandler(dom4jOutWriter); //chaining breaks prettyprint :(
		//DOM4J SAXWriter -> SAX ContentHandler
		SAXWriter saxWriter = new SAXWriter(validatorHandler);
		saxWriter.write(dom4jDocument);

		System.out.println(stringWriter);
		assertThat(errorHandler.getErrors()).hasSize(6);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		//org.dom4j.io.DocumentSource/DocumentResult are SAX based therefore compatible and works

		Transformer transformer = traxFactory.newTransformer();
		JAXBResult jaxbResult = new JAXBResult(pingContext.getJaxbContext());
		transformer.transform(new DocumentSource(dom4jDocument), jaxbResult);
		Object jaxb = jaxbResult.getResult();
		//System.out.println(ping);
		//transformer.transform(new JAXBSource(pingContext.getContext(), ping), new StreamResult(System.out));

		DocumentResult dom4jResult = new DocumentResult();
		transformer.transform(new DOMSource(domDocument), dom4jResult);
		dom4jDocument = dom4jResult.getDocument();
		//System.out.println(ping);

		//DOM4J -> JAXB
		errorHandler.reset();
		Unmarshaller unmarshaller = pingContext.createUnmarshaller();
		unmarshaller.setEventHandler(errorHandler);
		jaxb = unmarshaller.unmarshal(new DocumentSource(dom4jDocument));

		assertThat(errorHandler.getErrors()).hasSize(9); //jaxb ma vice validacnich chyb
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		//JAXB -> DOM4J
		errorHandler.reset();
		Marshaller marshaller = pingContext.createMarshaller();
		marshaller.setEventHandler(errorHandler);
		SAXContentHandler dom4jContentHandler = new SAXContentHandler();
		validatorHandler.setContentHandler(dom4jContentHandler);
		marshaller.marshal(jaxb, validatorHandler);

		dom4jDocument = dom4jContentHandler.getDocument();
		assertThat(dom4jDocument2.getRootElement().getName()).isEqualTo("PingRequest");
		assertThat(errorHandler.getErrors()).hasSize(6);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		new XMLWriter(System.out, OutputFormat.createPrettyPrint()).write(dom4jDocument);
	}

	/**
	 * jdom has very limmited support for xml validation
	 */
	public void jdomStream2Dom() throws Exception {
		SAXBuilder jdomBuilder = new SAXBuilder();
		//jdomBuilder.setXMLFilter(null);
		org.jdom2.Document jdomDocument = jdomBuilder
				.build(new FileReader("src/test/resources/xml/PingRequest-ErrTag.xml"));
		//DOM -> JDOM
		//jdomBuilder.build(domDocument);

		StringWriter stringWriter = new StringWriter();
		XMLOutputter outputter = new XMLOutputter();
		outputter.setFormat(Format.getPrettyFormat());
		outputter.output(jdomDocument, stringWriter);
		System.out.println(stringWriter);
		//JDOM -> DOM
		new DOMOutputter().output(jdomDocument);
	}

	public static void main(String[] args) {
		XmlDom4jTest test = new XmlDom4jTest();
		try {
			test.dom4jStream2Dom();
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

}
