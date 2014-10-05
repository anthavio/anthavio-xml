package net.anthavio.xml.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBResult;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import javax.xml.validation.ValidatorHandler;

import net.anthavio.example.messages.PingRequest;
import net.anthavio.xml.Dom2SaxEventGenerator;
import net.anthavio.xml.JaxpDomFactory;
import net.anthavio.xml.JaxpDomFactory.DomImplementation;
import net.anthavio.xml.JaxpSaxFactory;
import net.anthavio.xml.JaxpSaxFactory.SaxImplementation;
import net.anthavio.xml.JaxpTraxFactory;
import net.anthavio.xml.JaxpTraxFactory.TraxImplementation;
import net.anthavio.xml.JaxpXpathFactory;
import net.anthavio.xml.JaxpXpathFactory.XpathImplementation;
import net.anthavio.xml.Sax2DomContentHandler;
import net.anthavio.xml.Sax2IndentingWriter;
import net.anthavio.xml.StringResult;
import net.anthavio.xml.XPathSaxTracker;
import net.anthavio.xml.XPathTracker;
import net.anthavio.xml.jaxb.JaxbContextFactory;
import net.anthavio.xml.jaxb.JaxbContextFactory.JaxbImplementation;
import net.anthavio.xml.stax.Dom2StaxStreamReader;
import net.anthavio.xml.stax.JaxpStaxFactory;
import net.anthavio.xml.stax.JaxpStaxFactory.StaxImplementation;
import net.anthavio.xml.stax.JaxpStaxInputFactory;
import net.anthavio.xml.stax.Stax2SaxStreamReader;
import net.anthavio.xml.stax.StaxIndentingStreamWriter;
import net.anthavio.xml.stax.XPathEventTracker;
import net.anthavio.xml.stax.XPathStreamTracker;
import net.anthavio.xml.validation.JaxpSchemaFactory;
import net.anthavio.xml.validation.JaxpSchemaFactory.SchemaImplementation;
import net.anthavio.xml.validation.SilentErrorHandler;
import net.anthavio.xml.validation.ValidationEventImpl;
import net.anthavio.xml.validation.XmlErrorHandler;
import net.anthavio.xml.validation.XmlSchemaLoader;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

public class XmlJaxpTest {

	JaxpDomFactory domFactoryNv;
	JaxpSaxFactory saxFactoryNv;
	JaxpDomFactory domFactoryVx;
	JaxpSaxFactory saxFactoryVx;

	JaxpSchemaFactory schemaFactory = new JaxpSchemaFactory(SchemaImplementation.XERCES);
	JaxpXpathFactory xpathFactory = new JaxpXpathFactory(XpathImplementation.JDK);
	JaxpTraxFactory traxFactory = new JaxpTraxFactory(TraxImplementation.JDK);
	JaxpStaxFactory staxFactory = new JaxpStaxFactory(StaxImplementation.WOODSTOX);

	Schema pingSchema = XmlSchemaLoader.load(getClass().getResource("/schema/PingMessages.xsd"), schemaFactory);

	JaxbContextFactory pingContext;
	XmlPrefixMapper namespaceMapper;

	public XmlJaxpTest() {

		HashMap<String, String> uri2prefix = new HashMap<String, String>();
		uri2prefix.put("http://example.anthavio.net/same", "samens");
		uri2prefix.put("http://example.anthavio.net/messages", "emsg");
		uri2prefix.put("http://example.anthavio.net/types", "etyp");
		namespaceMapper = new XmlPrefixMapper(uri2prefix);

		//dom & trax

		domFactoryNv = new JaxpDomFactory(DomImplementation.XERCES);
		saxFactoryNv = new JaxpSaxFactory(SaxImplementation.XERCES);

		domFactoryVx = new JaxpDomFactory(DomImplementation.XERCES);
		domFactoryVx.setSchema(pingSchema);
		saxFactoryVx = new JaxpSaxFactory(SaxImplementation.XERCES);
		saxFactoryVx.setSchema(pingSchema);

		//domFactory.setIgnoringElementContentWhitespace(true);
		//domFactory.setIgnoringComments(true);

		HashMap<String, Object> traxFactoryAttrs = new HashMap<String, Object>();
		traxFactoryAttrs.put("indent-number", 2);
		traxFactory.setFactoryAttributes(traxFactoryAttrs);

		HashMap<String, String> traxOutputProperties = new HashMap<String, String>();
		//traxOutputProperties.put(OutputKeys.INDENT, "yes");
		traxFactory.setOutputProperties(traxOutputProperties);

		//jaxb
		HashMap<String, Object> marshallerProperties = new HashMap<String, Object>();
		marshallerProperties.put(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshallerProperties.put(Marshaller.JAXB_ENCODING, "utf-8");
		//marshallerProperties.put("com.sun.xml.bind.namespacePrefixMapper", namespaceMapper);

		pingContext = new JaxbContextFactory(JaxbImplementation.JAXB_RI);
		pingContext.setContextPath("net.anthavio.example.messages");
		pingContext.setSchema(pingSchema);
		pingContext.setMarshallerProperties(marshallerProperties);

	}

	// DOM -> SAX ContentHandler
	// anthavio.net.xml.Dom2SaxEventGenerator

	// SAX ContentHandler -> DOM
	// anthavio.net.xml.Sax2DomContentHandler

	// STAX Reader -> SAX ContentHandler
	// javanet.staxutils.XMLEventReaderToContentHandler/XMLStreamReaderToContentHandler

	// SAX ContentHandler -> STAX Writer
	// javanet.staxutils.ContentHandlerToXMLEventWriter/ContentHandlerToXMLStreamWriter
	// SAX2StAXEventWriter

	// SAX ContentHandler -> STAX EventConsumer
	// javanet.staxutils.StAXEventContentHandler/StAXStreamContentHandler

	// DOM -> STAX Reader
	// org.codehaus.stax2.ri.dom.DOMWrappingReader (com.ctc.wstx.dom.WstxDOMWrappingReader)

	// STAX Writer -> DOM
	// org.codehaus.stax2.ri.dom.DOMWrappingWriter (com.ctc.wstx.dom.WstxDOMWrappingWriter)

	// STAX Reader -> DOM
	// org.codehaus.staxmate.dom.DOMConverter.buildDocument(sr);

	// DOM -> STAX Writer
	// org.codehaus.staxmate.dom.DOMConverter.writeDocument(doc, sw);

	// SAX -> Stream
	// com.sun.xml.bind.marshaller.XMLWriter

	/**
	 * DocumentBuilder umi jen org.xml.sax.InputSource nebo java.io.InputStream
	 * Nelze vlozit XPathTracker pro stopovani prubehu parsovani
	 */
	@Test
	public void testPlain() throws Exception {

		//JAXP 1.4
		//DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance(JaxpDomFactory.DOM_FACTORY_JAXP, null);
		//JAXP 1.3
		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
		documentFactory.setNamespaceAware(true);
		documentFactory.setSchema(pingSchema);
		DocumentBuilder builder = documentFactory.newDocumentBuilder();
		SilentErrorHandler errorHandler = new SilentErrorHandler();
		builder.setErrorHandler(errorHandler);

		InputSource inputSource = new InputSource(new FileInputStream("src/test/resources/xml/PingRequest-OK.xml"));
		//InputSource inputSource = new InputSource(new StringReader("<x>\n<y>\n</y>\n</x><z>"));
		Document document = null;
		try {
			document = builder.parse(inputSource);
		} catch (SAXParseException spx) {
			//errorHandler.error(spx);
		}

		assertThat(document.getDocumentElement().getLocalName()).isEqualTo("PingRequest");
		//printEvents(errorHandler);
		//assertThat(errorHandler.getErrors()).hasSize(4);

		//JAXP 1.4
		//TransformerFactory transformerFactory = TransformerFactory.newInstance(JaxpTraxFactory.TRAX_FACTORY_JAXP, null);
		//JAXP 1.3
		TransformerFactory transformerFactory = TransformerFactory.newInstance();

		transformerFactory.setAttribute("indent-number", 4);
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		StreamResult stringResult = new StreamResult(new StringWriter());
		transformer.transform(new DOMSource(document), stringResult);

		//System.out.println(stringResult.getWriter());

		//The DOM Level 3 Load and Save API

		DOMImplementationRegistry domRegistry = DOMImplementationRegistry.newInstance();
		DOMImplementationLS domLoadSaveImpl = (DOMImplementationLS) domRegistry.getDOMImplementation("LS");

		LSSerializer lsSerializer = domLoadSaveImpl.createLSSerializer();
		lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);

		LSOutput lsOutput = domLoadSaveImpl.createLSOutput();
		lsOutput.setEncoding("UTF-8");
		lsOutput.setByteStream(System.out);
		lsSerializer.write(document, lsOutput);
	}

	private void plainStax() throws Exception {

		Reader reader = new FileReader("src/test/resources/xml/PingRequest-OK.xml");
		XMLInputFactory inputFactory = XMLInputFactory.newInstance(JaxpStaxInputFactory.STAX_INPUT_FACTORY_WOODSTOX, null);
		XMLEventReader eventReader = inputFactory.createXMLEventReader(reader);
		//DOMConverter domConverter = new DOMConverter();
		//XMLStreamReader streamReader = inputFactory.createXMLStreamReader(reader);
		//Document document = domConverter.buildDocument(streamReader);
		//print(document);

		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance(JaxpDomFactory.DOM_FACTORY_JAXP, null);
		documentFactory.setNamespaceAware(true);
		DocumentBuilder builder = documentFactory.newDocumentBuilder();
		Document document = builder.newDocument();

		DOMResult domResult = new DOMResult(document);
		XMLOutputFactory staxOutputFactory = staxFactory.getXMLOutputFactory();
		//Woodstox ma bug pri zapisu komentaru do DOMWrappingWriter http://jira.codehaus.org/browse/WSTX-271
		XMLEventWriter eventWriter = staxOutputFactory.createXMLEventWriter(domResult);
		//XMLStreamWriter streamWriter = staxOutputFactory.createXMLStreamWriter(domResult);

		eventWriter.add(eventReader);
		//XMLStreamUtils.copy(streamReader, streamWriter);
		print(document);

		//Transformer transformer = traxFactory.newTransformer();
		//transformer.transform(new StAXSource(eventReader), new StAXResult(streamWriter));
		//print(document);

		//transformer & validator needs to create underlying xml parsers
		//since we cannot enforce implementation in code, it is vital to have META-INF/services
		Validator validator = pingSchema.newValidator();
		validator.setErrorHandler(new SilentErrorHandler());
		//errorHandler.setXPathTracker(xpathTracker);
		validator.validate(new StAXSource(eventReader), new StAXResult(eventWriter));
		print(document);
	}

	@Test
	public void testSax2Dom() throws Exception {

		String errTagXml = readFile("src/test/resources/xml/PingRequest-ErrTag.xml");
		SilentErrorHandler errorHandler = new SilentErrorHandler();
		Document document = doString2Dom_sax_parser_validation(new StringReader(errTagXml), pingSchema, errorHandler);

		assertThat(document.getDocumentElement().getLocalName()).isEqualTo("PingRequest");
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(6);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		errorHandler.reset();
		document = doString2Dom_sax_handler_validation(new StringReader(errTagXml), pingSchema, errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(6);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		errorHandler.reset();
		String xml = doDom2String_trax(document, pingSchema, errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(6);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();
		//System.out.println(xml);

		//print(document);
		//Result stringResult = new StringResult();
		//transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		//transformer.transform(new DOMSource(document), stringResult);
		//System.out.println(stringResult);
	}

	@Test
	public void testString2Dom() throws Exception {
		Document document;
		SilentErrorHandler errorHandler = new SilentErrorHandler();
		String errTagXml = readFile("src/test/resources/xml/PingRequest-ErrTag.xml");
		//SAX validator reportuje mene chyb nez pri JAXB
		int expectedErrors = 6;

		//STAX2DOM

		errorHandler.reset();
		document = doString2Dom_stax_trax(new StringReader(errTagXml), pingSchema, errorHandler);
		//print(document);
		assertThat(document.getDocumentElement().getLocalName()).isEqualTo("PingRequest");
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(expectedErrors);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();
		//ValidationEventImpl event = errorHandler.getEvents().get(0);
		//assertThat(event.getMessage()).isEqualTo("cvc-elt.1: Cannot find the declaration of element 'x'.");

		errorHandler.reset();
		document = doString2Dom_stax_validator(new StringReader(errTagXml), pingSchema, errorHandler);
		//print(document);
		assertThat(document.getDocumentElement().getLocalName()).isEqualTo("PingRequest");
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(expectedErrors);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		errorHandler.reset();
		document = doString2Dom_stax2sax(new StringReader(errTagXml), pingSchema, errorHandler);
		//print(document);
		assertThat(document.getDocumentElement().getLocalName()).isEqualTo("PingRequest");
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(expectedErrors);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		//SAX2DOM

		errorHandler.reset();
		document = doString2Dom_sax_trax(new StringReader(errTagXml), pingSchema, errorHandler);
		//print(document);
		assertThat(document.getDocumentElement().getLocalName()).isEqualTo("PingRequest");
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(expectedErrors);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		errorHandler.reset();
		document = doString2Dom_sax_handler_validation(new StringReader(errTagXml), pingSchema, errorHandler);
		//print(document);
		assertThat(document.getDocumentElement().getLocalName()).isEqualTo("PingRequest");
		printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(expectedErrors);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		errorHandler.reset();
		document = doString2Dom_sax_parser_validation(new StringReader(errTagXml), pingSchema, errorHandler);
		//print(document);
		assertThat(document.getDocumentElement().getLocalName()).isEqualTo("PingRequest");
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(expectedErrors);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

	}

	/**
	 * Custom SAX2DOM Sax2DomContentHandler
	 * Validation in SAX reader
	 * 
	 * XMLReader(validation) -> XPathSaxTracker -> Sax2DomContentHandler -> DOM
	 * 
	 * 5000 - 6700 ms
	 */
	private Document doString2Dom_sax_parser_validation(Reader reader, Schema schema, XmlErrorHandler errorHandler)
			throws Exception {

		SAXParserFactory saxFactory = saxFactoryVx.getFactory();
		SAXParser saxParser = saxFactory.newSAXParser();
		XMLReader saxReader = saxParser.getXMLReader();

		XPathSaxTracker xpathSaxReader = new XPathSaxTracker(saxReader); //XMLReader -> XPathSaxTracker
		xpathSaxReader.setErrorHandler(errorHandler);
		errorHandler.setXPathTracker(xpathSaxReader);

		DocumentBuilder domBuilder = domFactoryNv.newDocumentBuilder();
		Document document = domBuilder.newDocument();
		Sax2DomContentHandler sax2DomHandler = new Sax2DomContentHandler(document);
		xpathSaxReader.setContentHandler(sax2DomHandler); //XPathSaxTracker -> Sax2DomContentHandler

		xpathSaxReader.parse(new InputSource(reader));
		return document;
	}

	/**
	 * Custom SAX2DOM Sax2DomContentHandler
	 * Validation in chained ContentHandler
	 * 
	 * XMLReader -> XPathSaxTracker -> ValidatorHandler -> Sax2DomContentHandler -> DOM
	 * 
	 * 5000 - 7080 ms
	 */
	private Document doString2Dom_sax_handler_validation(Reader reader, Schema schema, XmlErrorHandler errorHandler)
			throws Exception {
		SAXParserFactory saxFactory = saxFactoryNv.getFactory();
		SAXParser saxParser = saxFactory.newSAXParser();
		XMLReader saxReader = saxParser.getXMLReader();

		XPathSaxTracker xpathSaxReader = new XPathSaxTracker(saxReader); //XMLReader -> XPathSaxTracker
		xpathSaxReader.setErrorHandler(errorHandler);
		errorHandler.setXPathTracker(xpathSaxReader);

		DocumentBuilder domBuilder = domFactoryNv.newDocumentBuilder();
		Document document = domBuilder.newDocument();
		Sax2DomContentHandler sax2DomHandler = new Sax2DomContentHandler(document);

		ValidatorHandler validatorHandler = schema.newValidatorHandler();
		validatorHandler.setErrorHandler(errorHandler);
		validatorHandler.setContentHandler(sax2DomHandler); //ValidatorHandler -> Sax2DomContentHandler
		xpathSaxReader.setContentHandler(validatorHandler); //XPathSaxTracker -> ValidatorHandler

		xpathSaxReader.parse(new InputSource(reader));
		return document;
	}

	/**
	 * Standardni JAXP cesta
	 * 
	 * Slozite je chovani Sax ErrorHandler a Trax ErrorListener
	 * Zalezi na tom jestli vyjimka ze Sax probubla do Trax
	 * 
	 * XMLReader(validation) -> XPathSaxTracker -> SAXSource -> Transformer -> DOMResult
	 * 
	 * 5000 - 7200 ms
	 */
	private Document doString2Dom_sax_trax(Reader reader, Schema schema, XmlErrorHandler errorHandler) throws Exception {
		SAXParserFactory saxFactory = saxFactoryVx.getFactory();
		SAXParser saxParser = saxFactory.newSAXParser();
		XMLReader saxReader = saxParser.getXMLReader();

		XPathSaxTracker xpathSaxReader = new XPathSaxTracker(saxReader);
		xpathSaxReader.setErrorHandler(errorHandler);

		SAXSource saxSource = new SAXSource(xpathSaxReader, new InputSource(reader));

		DocumentBuilder domBuilder = domFactoryNv.newDocumentBuilder();
		Document document = domBuilder.newDocument();

		Transformer transformer = traxFactory.newTransformer();
		transformer.setErrorListener(errorHandler);
		transformer.transform(saxSource, new DOMResult(document));
		return document;
	}

	/**
	 * Cesta vede pres
	 * 1. Zretezteni XPathTracker a XMLEventReader
	 * 2. XMLOutputFactory.createXMLStreamWriter(DOMResult) !!!
	 * 3. java.xml.validation.Validator.validate(StAXSource, StAXResult)
	 * 
	 * Nelze pouzit Schema.newValidatorHandler()
	 * protoze Trax na vstupnim XMLEventReader/XMLStreamReader prepisuje ContentHandler
	 * 
	 * Woodstox i JAXP
	 * 
	 * Woodstox XMLEventReader prevadi xml komentare na CDATA - https://jira.codehaus.org/browse/WSTX-271
	 * 
	 * XMLStreamReader -> XPathEventTracker -> Validator -> XMLEventWriter ->
	 * Validator interne pouziva Trax Stax->SAX->Stax a validuje SAX eventy
	 * 
	 * 5000 - 6900 ms
	 */
	private Document doString2Dom_stax_validator(Reader reader, Schema schema, XmlErrorHandler errorHandler)
			throws Exception {

		XMLInputFactory staxInputFactory = staxFactory.getXMLInputFactory();
		//XMLEventReader xmlEventReader = staxInputFactory.createXMLEventReader(reader);
		//XPathEventTracker xpathTracker = new XPathEventTracker(xmlEventReader);
		XMLStreamReader staxStreamReader = staxInputFactory.createXMLStreamReader(reader);
		XPathStreamTracker xpathTracker = new XPathStreamTracker(staxStreamReader);

		DocumentBuilder domBuilder = domFactoryNv.newDocumentBuilder();
		Document document = domBuilder.newDocument();
		DOMResult domResult = new DOMResult(document);
		XMLOutputFactory staxOutputFactory = staxFactory.getXMLOutputFactory();
		XMLEventWriter staxEventWriter = staxOutputFactory.createXMLEventWriter(domResult);//Bug Woodstox http://jira.codehaus.org/browse/WSTX-271

		Validator validator = schema.newValidator();
		validator.setErrorHandler(errorHandler);
		errorHandler.setXPathTracker(xpathTracker);
		validator.validate(new StAXSource(xpathTracker), new StAXResult(staxEventWriter));
		return document;
	}

	/**
	 * Custom Sax2DomContentHandler
	 * 
	 * XMLStreamReader -> XPathStreamTracker -> Transformer Stax2Sax -> ValidatorHandler -> Sax2DomContentHandler
	 * 
	 * Nefunguje v kombinaci Woodstox XMLEventReader & JDK Trax s chybou
	 * java.lang.ClassCastException: org.codehaus.stax2.ri.evt.NamespaceEventImpl cannot be cast to java.lang.String
	 * XMLStreamReader na vstupu je to OK
	 * 
	 * 5000 - 5900 ms (XMLStreamReader)
	 */
	private Document doString2Dom_stax_trax(Reader reader, Schema schema, XmlErrorHandler errorHandler) throws Exception {

		XMLInputFactory staxInputFactory = staxFactory.getXMLInputFactory();
		//XMLEventReader xmlEventReader = staxInputFactory.createXMLEventReader(reader);
		//XPathEventTracker xpathTracker = new XPathEventTracker();
		//XMLEventReader xmlEventReaderFilter = staxInputFactory.createFilteredReader(xmlEventReader, xpathTracker);
		//StAXSource stAXSource = new StAXSource(xmlEventReaderFilter);

		//XPathEventTracker xpathTracker = new XPathEventTracker(xmlEventReader);
		//StAXSource stAXSource = new StAXSource(xpathTracker);

		XMLStreamReader staxStreamReader = staxInputFactory.createXMLStreamReader(reader);
		XPathStreamTracker xpathTracker = new XPathStreamTracker(staxStreamReader);
		StAXSource staxSource = new StAXSource(xpathTracker);

		DocumentBuilder domBuilder = domFactoryNv.newDocumentBuilder();
		Document document = domBuilder.newDocument();

		ValidatorHandler validatorHandler = schema.newValidatorHandler();
		validatorHandler.setContentHandler(new Sax2DomContentHandler(document));
		validatorHandler.setErrorHandler(errorHandler);
		errorHandler.setXPathTracker(xpathTracker);

		Transformer transformer = traxFactory.newTransformer();
		transformer.setErrorListener(errorHandler);
		transformer.transform(staxSource, new SAXResult(validatorHandler));

		return document;
	}

	/**
	 * Custom Stax2SaxStreamReader a Sax2DomContentHandler
	 * 
	 * Reader -> XPathStreamTracker -> Stax2SaxStreamReader -> ValidatorHandler -> Sax2DomContentHandler
	 * 
	 * 5000 - 6330 ms (XMLStreamReader)
	 */
	private Document doString2Dom_stax2sax(Reader reader, Schema schema, XmlErrorHandler errorHandler) throws Exception {

		XMLInputFactory staxInputFactory = staxFactory.getXMLInputFactory();

		XMLStreamReader staxStreamReader = staxInputFactory.createXMLStreamReader(reader);
		XPathStreamTracker xpathTracker = new XPathStreamTracker(staxStreamReader);

		DocumentBuilder domBuilder = domFactoryNv.newDocumentBuilder();
		Document document = domBuilder.newDocument();

		ValidatorHandler validatorHandler = schema.newValidatorHandler();
		validatorHandler.setContentHandler(new Sax2DomContentHandler(document));
		validatorHandler.setErrorHandler(errorHandler);
		errorHandler.setXPathTracker(xpathTracker);

		Stax2SaxStreamReader stax2saxReader = new Stax2SaxStreamReader(xpathTracker, validatorHandler);
		stax2saxReader.bridge();

		return document;
	}

	public void testDom2String() throws Exception {
		SilentErrorHandler errorHandler = new SilentErrorHandler();
		String xml;
		String errTagXml = readFile("src/test/resources/xml/PingRequest-ErrTag.xml");
		//Reader reader = new InputStreamReader(new FileInputStream("src/test/resources/xml/PingRequest-ErrTag.xml"), "utf-8");
		Document document = doString2Dom_sax_trax(new StringReader(errTagXml), pingSchema, errorHandler);
		//DOM obsahuje vsechny nevalidni prvky

		errorHandler.reset();
		xml = doDom2String_trax(document, pingSchema, errorHandler);
		//System.out.println(xml);
		assertThat(xml).isNotEmpty();
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(4);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		errorHandler.reset();
		doDom2String_stax(document, pingSchema, errorHandler);
		//System.out.println(xml);
		assertThat(xml).isNotEmpty();
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(4);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		errorHandler.reset();
		doDom2String_sax_custom(document, pingSchema, errorHandler);
		//System.out.println(xml);
		assertThat(xml).isNotEmpty();
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(4);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		errorHandler.reset();
		doDom2String_stax_custom(document, pingSchema, errorHandler);
		//System.out.println(xml);
		assertThat(xml).isNotEmpty();
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(4);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();
	}

	/**
	 * Pomoci validatoru
	 * 
	 * DOMSource -> XMLEventReader -> XPathEventTracker -> Validator(StAXSource, StAXResult)
	 * 
	 * JAXP Stax neumi EventReader nad DOMSource
	 * JAXP Validator nespolupracuje s WOODSTOX StreamReader (IllegalArgumentException: Unrecognized property 'javax.xml.stream.isInterning')
	 * 
	 * 5000 - 5070 ms (EventReader/EventWriter)
	 * 5000 - 3410 ms (EventReader/StreamWriter)
	 * 
	 */
	private String doDom2String_stax(Document document, Schema schema, XmlErrorHandler errorHandler) throws Exception {

		XMLInputFactory staxInputFactory = staxFactory.getXMLInputFactory();
		//JAXP neumi EventReader nad DOMSource
		XMLEventReader xmlEventReader = staxInputFactory.createXMLEventReader(new DOMSource(document));
		XPathEventTracker xpathTracker = new XPathEventTracker(xmlEventReader);
		errorHandler.setXPathTracker(xpathTracker);

		StringResult stringResult = new StringResult();
		XMLOutputFactory staxOutputFactory = staxFactory.getXMLOutputFactory();
		XMLStreamWriter xmlStreamWriter = staxOutputFactory.createXMLStreamWriter(stringResult);
		StaxIndentingStreamWriter staxIndentWriter = new StaxIndentingStreamWriter(xmlStreamWriter);

		Validator validator = schema.newValidator();
		validator.setErrorHandler(errorHandler);
		validator.validate(new StAXSource(xpathTracker), new StAXResult(staxIndentWriter));

		// validator.setProperty(OutputKeys.INDENT, "no");

		return stringResult.toString();
	}

	/**
	 * Pomoci Trax
	 * 
	 * DOMSource -> Transformer -> SAXResult -> XPathSaxTracker -> ValidatorHandler -> Sax2OutputWriter
	 * 
	 * 5000 - 3640 ms
	 */
	private String doDom2String_trax(Document document, Schema schema, XmlErrorHandler errorHandler) throws Exception {
		ValidatorHandler validatorHandler = schema.newValidatorHandler();
		validatorHandler.setErrorHandler(errorHandler);
		XPathSaxTracker xpathTracker = new XPathSaxTracker(validatorHandler);
		errorHandler.setXPathTracker(xpathTracker);

		StringWriter stringWriter = new StringWriter();
		Sax2IndentingWriter saxIndentWriter = new Sax2IndentingWriter(stringWriter, "utf-8");//handler ktery to zestringje
		validatorHandler.setContentHandler(saxIndentWriter);

		Transformer transformer = traxFactory.newTransformer();
		transformer.setErrorListener(errorHandler);
		transformer.transform(new DOMSource(document), new SAXResult(xpathTracker));

		return stringWriter.toString();
	}

	/**
	 * DOM -> Dom2SaxEventGenerator -> ValidatorHandler -> XPathSaxTracker -> Sax2IndentingWriter
	 * 
	 * 5000 - 3140 ms
	 */
	private String doDom2String_sax_custom(Document document, Schema schema, XmlErrorHandler errorHandler)
			throws Exception {
		ValidatorHandler validatorHandler = schema.newValidatorHandler();
		validatorHandler.setErrorHandler(errorHandler);

		StringWriter stringWriter = new StringWriter();
		Sax2IndentingWriter saxIndentWriter = new Sax2IndentingWriter(stringWriter, "utf-8");
		XPathSaxTracker xpathTracker = new XPathSaxTracker((ContentHandler) saxIndentWriter);
		validatorHandler.setContentHandler(xpathTracker);

		errorHandler.setXPathTracker(xpathTracker);

		new Dom2SaxEventGenerator(validatorHandler).scan(document);

		return stringWriter.toString();
	}

	/**
	 * DOM -> Dom2StaxStreamReader -> XPathStreamTracker -> Validator -> StaxIndentingStreamWriter
	 */
	private String doDom2String_stax_custom(Document document, Schema schema, XmlErrorHandler errorHandler)
			throws Exception {

		Dom2StaxStreamReader dom2StaxStreamReader = new Dom2StaxStreamReader(document);
		XPathStreamTracker xpathTracker = new XPathStreamTracker(dom2StaxStreamReader);
		errorHandler.setXPathTracker(xpathTracker);

		StringResult stringResult = new StringResult();
		XMLOutputFactory staxOutputFactory = staxFactory.getXMLOutputFactory();
		XMLStreamWriter xmlStreamWriter = staxOutputFactory.createXMLStreamWriter(stringResult);
		StaxIndentingStreamWriter staxIndentWriter = new StaxIndentingStreamWriter(xmlStreamWriter);

		Validator validator = schema.newValidator();
		validator.setErrorHandler(errorHandler);
		validator.validate(new StAXSource(xpathTracker), new StAXResult(staxIndentWriter));

		return stringResult.toString();
	}

	@Test
	public void testString2Jaxb() throws Exception {
		Object jaxb;
		SilentErrorHandler errorHandler = new SilentErrorHandler();
		String errTagXml = readFile("src/test/resources/xml/PingRequest-ErrTag.xml");

		errorHandler.reset();
		jaxb = doString2Jaxb_stax(new StringReader(errTagXml), pingSchema, errorHandler);
		assertThat(jaxb).isNotNull();
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(9); //Jaxb reportuje stejne chyby vicero hlaskama
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		errorHandler.reset();
		jaxb = doString2Jaxb_sax(new StringReader(errTagXml), pingSchema, errorHandler);
		assertThat(jaxb).isNotNull();
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(9); //Jaxb reportuje stejne chyby vicero hlaskama
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		errorHandler.reset();
		jaxb = doString2Jaxb_sax_parser_validation(new StringReader(errTagXml), pingSchema, errorHandler);
		//System.out.println(jaxb);
		assertThat(jaxb).isNotNull();
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(9); //Jaxb reportuje stejne chyby vicero hlaskama
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();
	}

	/**
	 * Standardni JAXB cesta
	 * 
	 * XMLStreamReader -> XPathStreamTracker -> JAXB Unmarshaller (validation)
	 * 
	 * 5000 - 4400 ms (XMLStreamReader)
	 * 5000 - 4750 ms (XMLEventReader)
	 */
	private Object doString2Jaxb_stax(Reader reader, Schema schema, XmlErrorHandler errorHandler) throws Exception {
		XMLInputFactory staxInputFactory = staxFactory.getXMLInputFactory();
		XMLStreamReader staxStreamReader = staxInputFactory.createXMLStreamReader(reader);

		XPathStreamTracker xpathTracker = new XPathStreamTracker(staxStreamReader);
		errorHandler.setXPathTracker(xpathTracker);

		JAXBContext jaxbContext = pingContext.getJaxbContext();
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		unmarshaller.setSchema(schema);
		unmarshaller.setEventHandler(errorHandler);
		return unmarshaller.unmarshal(xpathTracker);
	}

	/**
	 * XMLReader -> XPathSaxTracker -> SAXSource -> JAXB Unmarshaller (validation)
	 * 
	 * 5000 - 7050 ms
	 */
	private Object doString2Jaxb_sax(Reader reader, Schema schema, XmlErrorHandler errorHandler) throws Exception {
		XMLReader saxReader = saxFactoryNv.getXMLReader();
		XPathSaxTracker xpathTracker = new XPathSaxTracker(saxReader);
		errorHandler.setXPathTracker(xpathTracker);
		SAXSource saxSource = new SAXSource(xpathTracker, new InputSource(reader));

		JAXBContext jaxbContext = pingContext.getJaxbContext();
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		unmarshaller.setSchema(schema);
		unmarshaller.setEventHandler(errorHandler);
		return unmarshaller.unmarshal(saxSource);
	}

	/**
	 * XMLReader (validation) -> XPathSaxTracker -> SAXSource -> JAXB Unmarshaller
	 * 
	 * 5000 - 6656 ms
	 */
	private Object doString2Jaxb_sax_parser_validation(Reader reader, Schema schema, XmlErrorHandler errorHandler)
			throws Exception {
		SAXParserFactory saxFactory = saxFactoryVx.getFactory();
		SAXParser saxParser = saxFactory.newSAXParser();
		XMLReader saxReader = saxParser.getXMLReader();

		XPathSaxTracker xpathSaxReader = new XPathSaxTracker(saxReader);
		SAXSource saxSource = new SAXSource(xpathSaxReader, new InputSource(reader));

		JAXBContext jaxbContext = pingContext.getJaxbContext();
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		unmarshaller.setEventHandler(errorHandler);
		unmarshaller.setSchema(null);

		return unmarshaller.unmarshal(saxSource);
	}

	@Test
	public void testJaxb2String() throws Exception {
		String xml;
		SilentErrorHandler errorHandler = new SilentErrorHandler();
		Reader reader = new FileReader("src/test/resources/xml/PingRequest-ErrTag.xml");
		Object jaxb = doString2Jaxb_stax(reader, pingSchema, errorHandler);
		//V JAXB podobe zmizi nektere chyby.
		//Zustane uz jen jeden event za 3ti opakovani u maxoccurs 2 a dva za prazdne version

		errorHandler.reset();
		xml = doJaxb2String_stax(jaxb, pingSchema, errorHandler);
		//System.out.println(xml);
		assertThat(xml).isNotNull();
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(3);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		errorHandler.reset();
		xml = doJaxb2String_sax(jaxb, pingSchema, errorHandler);
		//System.out.println(xml);
		assertThat(xml).isNotNull();
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(3);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();
	}

	/**
	 * JAXB standardni cesta
	 * 
	 * JAXB Marshaller (validation)-> XPathStreamTracker -> StaxIndentingStreamWriter -> Writer
	 * 
	 * 5000 - 4100 ms (EventWriter)
	 * 5000 - 2200 ms (StreamWriter)
	 */
	private String doJaxb2String_stax(Object jaxb, Schema schema, XmlErrorHandler errorHandler) throws Exception {
		StringWriter stringWriter = new StringWriter();
		XMLOutputFactory staxOutputFactory = staxFactory.getXMLOutputFactory();
		//XMLEventWriter xmlEventWriter = staxOutputFactory.createXMLEventWriter(stringResult);
		//IndentingXMLEventWriter indentingWriter = new IndentingXMLEventWriter(xmlEventWriter);
		//XPathEventTracker xpathTracker = new XPathEventTracker(indentingWriter);

		//XMLStreamWriter xmlStreamWriter = staxOutputFactory.createXMLStreamWriter(stringResult);
		//XPathStreamTracker xpathTracker = new XPathStreamTracker(xmlStreamWriter);
		//IndentingXMLStreamWriter indentingWriter = new IndentingXMLStreamWriter(xpathTracker);

		XMLStreamWriter staxStreamWriter = staxOutputFactory.createXMLStreamWriter(stringWriter);
		StaxIndentingStreamWriter staxIndentWriter = new StaxIndentingStreamWriter(staxStreamWriter);
		XPathStreamTracker xpathTracker = new XPathStreamTracker(staxIndentWriter);

		errorHandler.setXPathTracker(xpathTracker);

		JAXBContext jaxbContext = pingContext.getJaxbContext();
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setSchema(schema);
		marshaller.setEventHandler(errorHandler);
		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", namespaceMapper);
		marshaller.marshal(jaxb, xpathTracker);

		//Marshaller.JAXB_FORMATTED_OUTPUT output property works only when marshalling into Stream/Writer
		//marshaller.marshal(jaxb, stringResult);
		return stringWriter.toString();
	}

	/**
	 * JAXB Marshaller -> XPathSaxTracker -> Sax2IndentingWriter -> Writer
	 * 
	 * 5000 - 2150 ms (StreamWriter)
	 */
	private String doJaxb2String_sax(Object jaxb, Schema schema, XmlErrorHandler errorHandler) throws Exception {
		StringWriter stringWriter = new StringWriter();
		Sax2IndentingWriter saxWriter = new Sax2IndentingWriter(stringWriter, "utf-8");
		saxWriter.setIndentStep("  ");
		XPathSaxTracker xpathTracker = new XPathSaxTracker((ContentHandler) saxWriter);
		errorHandler.setXPathTracker(xpathTracker);

		JAXBContext jaxbContext = pingContext.getJaxbContext();
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setSchema(schema);
		marshaller.setEventHandler(errorHandler);
		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", namespaceMapper);
		marshaller.marshal(jaxb, xpathTracker);

		return stringWriter.toString();
	}

	@Test
	public void testDom2Jaxb() throws Exception {
		Object jaxb;
		SilentErrorHandler errorHandler = new SilentErrorHandler();
		String errTagXml = readFile("src/test/resources/xml/PingRequest-ErrTag.xml");
		//Reader reader = new InputStreamReader(new FileInputStream("src/test/resources/xml/PingRequest-ErrTag.xml"), "utf-8");
		Document document = doString2Dom_sax_trax(new StringReader(errTagXml), pingSchema, errorHandler);
		//DOM obsahuje vsechny nevalidni prvky a jaxb validace navic reportuje
		int expectedErrors = 9;

		errorHandler.reset();
		jaxb = doDom2Jaxb_stax(document, pingSchema, errorHandler);
		assertThat(jaxb).isNotNull();
		assertThat(errorHandler.getErrors()).hasSize(expectedErrors);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		errorHandler.reset();
		jaxb = doDom2Jaxb_trax(document, pingSchema, errorHandler);
		assertThat(jaxb).isNotNull();
		assertThat(errorHandler.getErrors()).hasSize(expectedErrors);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		errorHandler.reset();
		jaxb = doDom2Jaxb_custom(document, pingSchema, errorHandler);
		assertThat(jaxb).isNotNull();
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(expectedErrors);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		//errorHandler.reset();
		//jaxb = doDom2Jaxb_stax2(document, pingSchema, errorHandler);
		//assertThat(jaxb).isNotNull();
		//assertThat(errorHandler.getErrors()).hasSize(9);
		//assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();
	}

	/**
	 * DOMSource -> XMLEventReader -> XPathEventTracker -> JAXB Unmarshaller (validation) -> JAXB
	 * Wodstox ano , JDK 6.0_27 neumi XMLEventReader nad DOMSource
	 * 
	 * 5000 - 5150ms (XMLEventReader)
	 * 5000 - 5050ms (XMLStreamReader)
	 */
	private Object doDom2Jaxb_stax(Document document, Schema schema, XmlErrorHandler errorHandler) throws Exception {
		XMLInputFactory staxInputFactory = staxFactory.getXMLInputFactory();
		//Je treba pro zapnout pro Woodstox XMLStreamReader, jinak JAXB protestuje
		//staxInputFactory.setProperty(XMLInputFactory2.P_INTERN_NS_URIS, true);
		//staxInputFactory.setProperty(XMLInputFactory2.P_INTERN_NAMES, true);

		DOMSource domSource = new DOMSource(document);
		//Woodstox umi EventReader nad DOMSource, JAXP nikoliv
		XMLEventReader xmlEventReader = staxInputFactory.createXMLEventReader(domSource);

		XPathEventTracker xpathTracker = new XPathEventTracker(xmlEventReader);

		//WstxDOMWrappingReader domReader = WstxDOMWrappingReader.createFrom(new DOMSource(document),
		//		ReaderConfig.createFullDefaults());
		//domReader.setProperty("org.codehaus.stax2.internNames", false);
		//domReader.setProperty("org.codehaus.stax2.internNsUris", false);
		//XPathStreamTracker xpathTracker = new XPathStreamTracker(new Dom2StaxStreamReader(document));
		errorHandler.setXPathTracker(xpathTracker);

		Unmarshaller unmarshaller = pingContext.createUnmarshaller();
		unmarshaller.setEventHandler(errorHandler);

		return unmarshaller.unmarshal(xpathTracker);
	}

	/**
	 * Custom Dom2StaxStreamReader
	 * 
	 * Wodstox ano , JDK ano
	 * 
	 * DOM -> Dom2StaxStreamReader -> XPathStreamTracker -> JAXB Unmarshaller (validation)
	 * 
	 * 5000 - 4950 ms
	 */
	private Object doDom2Jaxb_custom(Document document, Schema schema, XmlErrorHandler errorHandler) throws Exception {
		Dom2StaxStreamReader dom2StaxStreamReader = new Dom2StaxStreamReader(document);
		XPathStreamTracker xpathTracker = new XPathStreamTracker(dom2StaxStreamReader);
		errorHandler.setXPathTracker(xpathTracker);

		Unmarshaller unmarshaller = pingContext.createUnmarshaller();
		unmarshaller.setEventHandler(errorHandler);

		return unmarshaller.unmarshal(xpathTracker);
	}

	/**
	 * Custom Dom2StaxStreamReader
	 * 
	 * DOM -> Dom2StaxStreamReader -> XPathStreamTracker -> Transformer(StAXSource, JAXBResult)
	 * 
	 * 5000 - 5400 ms
	 */
	private Object doDom2Jaxb_trax(Document document, Schema schema, XmlErrorHandler errorHandler) throws Exception {
		Unmarshaller unmarshaller = pingContext.createUnmarshaller();
		unmarshaller.setEventHandler(errorHandler);
		JAXBResult jaxbResult = new JAXBResult(unmarshaller);

		Dom2StaxStreamReader dom2StaxStreamReader = new Dom2StaxStreamReader(document);
		XPathStreamTracker xpathTracker = new XPathStreamTracker(dom2StaxStreamReader);
		errorHandler.setXPathTracker(xpathTracker);

		Transformer transformer = traxFactory.newTransformer();
		transformer.transform(new StAXSource(xpathTracker), jaxbResult);
		return jaxbResult.getResult();
	}

	/**
	 * WOODSTOX neumi XMLStreamWriter nad JAXBResult
	 * JDK neumi XMLEventReader nad DOMSource
	 */
	@Deprecated
	private Object doDom2Jaxb_stax2(Document document, Schema schema, XmlErrorHandler errorHandler) throws Exception {
		XMLInputFactory staxInputFactory = staxFactory.getXMLInputFactory();
		XMLOutputFactory staxOutputFactory = staxFactory.getXMLOutputFactory();

		XMLEventReader xmlEventReader = staxInputFactory.createXMLEventReader(new DOMSource(document));//-JAXP
		XPathEventTracker xpathTracker = new XPathEventTracker(xmlEventReader);
		errorHandler.setXPathTracker(xpathTracker);

		JAXBResult jaxbResult = new JAXBResult(pingContext.createUnmarshaller());
		//StAXResult staxResult = new StAXResult(staxOutputFactory.);
		//jaxbResult.s
		XMLStreamWriter xmlStreamWriter = staxOutputFactory.createXMLStreamWriter(jaxbResult); //-WOODSTOX

		Validator validator = schema.newValidator();
		validator.setErrorHandler(errorHandler);
		validator.validate(new StAXSource(xpathTracker), new StAXResult(xmlStreamWriter));

		return jaxbResult.getResult();
	}

	@Test
	public void testJaxb2Dom() throws Exception {
		Document document;
		SilentErrorHandler errorHandler = new SilentErrorHandler();
		Reader reader = new FileReader("src/test/resources/xml/PingRequest-ErrTag.xml");
		PingRequest jaxb = (PingRequest) doString2Jaxb_stax(reader, pingSchema, errorHandler);
		jaxb.getPingData().setTimestamp(Calendar.getInstance());
		//Uz jen jeden event za 3ti opakovani maxoccurs 2
		//A dva eventy za prazdne version

		errorHandler.reset();
		document = doJaxb2Dom_stax(jaxb, pingSchema, errorHandler);
		assertThat(document).isNotNull();
		assertThat(document.getDocumentElement().getLocalName()).isEqualTo("PingRequest");
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(3);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();

		errorHandler.reset();
		document = doJaxb2Dom_sax(jaxb, pingSchema, errorHandler);
		assertThat(document).isNotNull();
		assertThat(document.getDocumentElement().getLocalName()).isEqualTo("PingRequest");
		//printEvents(errorHandler);
		assertThat(errorHandler.getErrors()).hasSize(3);
		assertThat(errorHandler.getErrors().get(0).getXpath()).isNotEmpty();
	}

	/**
	 * Marshall do DOMResult
	 * JAXB Marshaller (validation) -> XPathTracker(stax) -> DOMResult
	 * 
	 * 5000 - 6200 ms (XMLEventWriter)
	 * 5000 - 4600 ms (XMLStreamWriter)
	 */
	private Document doJaxb2Dom_stax(Object jaxb, Schema schema, XmlErrorHandler errorHandler) throws Exception {
		DocumentBuilder builder = domFactoryNv.newDocumentBuilder();
		Document document = builder.newDocument();
		DOMResult domResult = new DOMResult(document);

		XMLOutputFactory staxOutputFactory = staxFactory.getXMLOutputFactory();
		XMLStreamWriter streamWriter = staxOutputFactory.createXMLStreamWriter(domResult);
		XPathStreamTracker xpathTracker = new XPathStreamTracker(streamWriter);
		errorHandler.setXPathTracker(xpathTracker);
		Marshaller marshaller = pingContext.createMarshaller(errorHandler);
		marshaller.marshal(jaxb, xpathTracker);
		return document;
	}

	/**
	 * Custom Sax2DomContentHandler
	 * 
	 * JAXB Marshaller (validation) -> XPathTracker(Sax) -> Sax2DomContentHandler -> DOM
	 * 
	 * 5000 - 4000 ms
	 */
	private Document doJaxb2Dom_sax(Object jaxb, Schema schema, XmlErrorHandler errorHandler) throws Exception {

		DocumentBuilder builder = domFactoryNv.newDocumentBuilder();
		Document document = builder.newDocument();

		Marshaller marshaller = pingContext.createMarshaller();
		marshaller.setEventHandler(errorHandler);

		Sax2DomContentHandler domHandler = new Sax2DomContentHandler(document);
		XPathTracker xpathTracker = new XPathSaxTracker(domHandler);
		errorHandler.setXPathTracker(xpathTracker);
		marshaller.marshal(jaxb, (ContentHandler) xpathTracker);
		return document;
	}

	private void printEvents(XmlErrorHandler errorHandler) {
		List<ValidationEventImpl> errors = errorHandler.getErrors();
		if (errors.size() > 0) {
			System.out.println("ERRORS");
			for (ValidationEventImpl event : errors) {
				System.out.println(event);
			}
		}
		List<ValidationEventImpl> warnings = errorHandler.getWarnings();
		if (warnings.size() > 0) {
			System.out.println("WARNINGS");
			for (ValidationEventImpl event : warnings) {
				System.out.println(event);
			}
		}
	}

	private static String print(Document dom) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance(JaxpTraxFactory.TRAX_FACTORY_XALAN, null);
		//transformerFactory.setAttribute("indent-number", 2);
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(dom), new StreamResult(writer));

		System.out.println(writer.toString());
		return writer.toString();
	}

	private static String readFile(String fileName) throws IOException {
		StringWriter writer = new StringWriter();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
		String line = null;
		while ((line = reader.readLine()) != null) {
			writer.append(line).append('\n');
		}
		reader.close();
		return writer.getBuffer().toString();
	}

	public void testParalel() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(20);

		for (int i = 0; i < 15; ++i) {
			Runnable runnable = new Runnable() {

				public void run() {
					//System.out.println("Started SAX Thread");
					for (int i = 0; i < 1000; ++i) {
						try {
							saxFactoryNv.getXMLReader().parse(new InputSource(new StringReader("<x:x xmlns:x='x'></x:x>")));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
					//System.out.println("Ended SAX Thread");
				}

			};
			executor.execute(runnable);
		}

		for (int i = 0; i < 15; ++i) {
			executor.execute(new Runnable() {

				public void run() {

					for (int i = 0; i < 1000; ++i) {
						try {
							domFactoryNv.newDocumentBuilder().parse(new InputSource(new StringReader("<x:x xmlns:x='x'></x:x>")));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				}

			});
		}
		executor.shutdown(); //this will wait for threads to finish
	}

	public static void main(String[] args) {
		XmlJaxpTest test = new XmlJaxpTest();
		try {
			String errTagXml = readFile("src/test/resources/xml/PingRequest-ErrTag.xml");
			XmlErrorHandler errorHandler = new XmlErrorHandler(false);
			//test.testSax2Dom();
			//if (true) {
			//	return;
			//}
			//test.testSax2Dom();
			Object jaxb = test.doString2Jaxb_stax(new StringReader(errTagXml), test.pingSchema, errorHandler);
			Document document = test.doString2Dom_stax2sax(new StringReader(errTagXml), test.pingSchema, errorHandler);

			long before = System.currentTimeMillis();
			for (int i = 0; i < 5000; ++i) {
				//test.doDom2Jaxb_trax(document, test.pingSchema, errorHandler);
				//test.doString2Jaxb_sax(new StringReader(errTagXml), test.pingSchema, errorHandler);
				//test.doDom2String_stax(document, test.pingSchema, errorHandler);
				test.doString2Dom_stax2sax(new StringReader(errTagXml), test.pingSchema, errorHandler);
			}
			long after = System.currentTimeMillis();
			System.out.println(after - before + " millis");

			//test.plainStax();
			//test.testPlain();
			//test.testSax2Dom();
			//test.testStax2Dom();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
