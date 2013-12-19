package net.anthavio.xml;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import javax.xml.validation.ValidatorHandler;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import net.anthavio.xml.stax.Dom2StaxStreamReader;
import net.anthavio.xml.stax.JaxpStaxInputFactory;
import net.anthavio.xml.stax.JaxpStaxOutputFactory;
import net.anthavio.xml.stax.StaxIndentingStreamWriter;
import net.anthavio.xml.stax.XPathEventTracker;
import net.anthavio.xml.stax.XPathStreamTracker;
import net.anthavio.xml.validation.XmlErrorHandler;
import net.anthavio.xml.validation.XmlValidationException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.UnhandledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;


/**
 * 
 * @author vanek
 *
 */
public class XmlAbstractParser {

	protected final Logger log = LoggerFactory.getLogger(XmlAbstractParser.class);

	private NamespaceContext namespaceContext;

	//nonvalidating (without schema)
	private JaxpDomFactory domFactoryNv;

	private JaxpTraxFactory traxFactory;

	private JaxpXpathFactory xpathFactory;

	private JaxpStaxInputFactory staxInputFactory;

	private JaxpStaxOutputFactory staxOutputFactory;

	private boolean fastFail = false;

	private boolean indenting = true;

	public boolean getFastFail() {
		return fastFail;
	}

	public void setFastFail(boolean fastFail) {
		this.fastFail = fastFail;
	}

	public boolean getIndenting() {
		return indenting;
	}

	public void setIndenting(boolean indenting) {
		this.indenting = indenting;
	}

	public NamespaceContext getNamespaceContext() {
		return namespaceContext;
	}

	public void setNamespaceContext(NamespaceContext namespaceContext) {
		this.namespaceContext = namespaceContext;
	}

	public void setTraxFactory(JaxpTraxFactory traxFactory) {
		this.traxFactory = traxFactory;
	}

	public JaxpTraxFactory getTraxFactory() {
		if (traxFactory == null) {
			traxFactory = new JaxpTraxFactory();
			traxFactory.setFactoryCached(true);
		}
		return traxFactory;
	}

	public void setXpathFactory(JaxpXpathFactory xpathFactory) {
		this.xpathFactory = xpathFactory;
	}

	public JaxpXpathFactory getXpathFactory() {
		if (xpathFactory == null) {
			xpathFactory = new JaxpXpathFactory();
			xpathFactory.setFactoryCached(true);
		}
		return xpathFactory;
	}

	public void setStaxOutputFactory(JaxpStaxOutputFactory staxOutputFactory) {
		this.staxOutputFactory = staxOutputFactory;
	}

	public JaxpStaxOutputFactory getStaxOutputFactory() {
		if (staxOutputFactory == null) {
			staxOutputFactory = new JaxpStaxOutputFactory();
			staxOutputFactory.setFactoryCached(true);
		}
		return staxOutputFactory;
	}

	public void setStaxInputFactory(JaxpStaxInputFactory staxInputFactory) {
		this.staxInputFactory = staxInputFactory;
	}

	public JaxpStaxInputFactory getStaxInputFactory() {
		if (staxInputFactory == null) {
			staxInputFactory = new JaxpStaxInputFactory();
			staxInputFactory.setFactoryCached(true);
		}
		return staxInputFactory;
	}

	private JaxpDomFactory getDomFactoryNoSchema() {
		if (domFactoryNv == null) {
			domFactoryNv = new JaxpDomFactory();
			domFactoryNv.setNamespaceAware(true);
			domFactoryNv.setFactoryCached(true);
		}
		return domFactoryNv;
	}

	protected Document newDocument() {
		return getDomFactoryNoSchema().newDocument();
	}

	protected Document parseStax(Reader reader, Schema schema) {
		try {
			XMLStreamReader staxStreamReader = getStaxInputFactory().getFactory().createXMLStreamReader(reader);
			return parseStax(staxStreamReader, schema);
		} catch (XMLStreamException xsx) {
			throw new XmlParseException(xsx);
		}
	}

	private Document parseStax(XMLStreamReader staxStreamReader, Schema schema) {
		XPathStreamTracker xpathTracker = new XPathStreamTracker(staxStreamReader);
		StAXSource staxSource = new StAXSource(xpathTracker);

		Document document = newDocument();

		ValidatorHandler validatorHandler = schema.newValidatorHandler();
		validatorHandler.setContentHandler(new Sax2DomContentHandler(document));

		XmlErrorHandler errorHandler = new XmlErrorHandler(getFastFail());
		errorHandler.setXPathTracker(xpathTracker);
		validatorHandler.setErrorHandler(errorHandler);
		/*
		Stax2SaxStreamReader stax2saxReader = new Stax2SaxStreamReader(xpathTracker, validatorHandler);
		try {
			stax2saxReader.bridge();
		} catch (XMLStreamException xsx) {
			if (xsx.getNestedException() != null) {
				throw new XmlParseException(xsx.getNestedException());
			} else {
				throw new XmlParseException(xsx);
			}
		}
		*/
		Transformer transformer = getTraxFactory().newTransformer();
		transformer.setErrorListener(errorHandler);

		try {
			transformer.transform(staxSource, new SAXResult(validatorHandler));
		} catch (TransformerException trx) {
			if (trx.getException() != null) {
				throw new XmlParseException(trx.getException());
			} else {
				throw new XmlParseException(trx);
			}
		}
		if (errorHandler.hasErrors()) {
			throw new XmlValidationException(errorHandler.getErrors());
		}

		return document;
	}

	/**
	 * @param reader java.io.Reader to read xml from
	 * @param saxReaderWithSchema Sax XMLReader built in SAXParserFactory with Schema
	 * @param domBuilderPlain Dom DocumentBuilder built in DocumentBuilderFactory without Schema
	 */
	protected Document parseSax(Reader reader, XMLReader saxReaderWithSchema) {
		XPathSaxTracker xpathSaxReader = new XPathSaxTracker(saxReaderWithSchema);

		XmlErrorHandler errorHandler = new XmlErrorHandler(getFastFail());
		errorHandler.setXPathTracker(xpathSaxReader);
		saxReaderWithSchema.setErrorHandler(errorHandler);

		Document document = newDocument();
		Sax2DomContentHandler sax2DomHandler = new Sax2DomContentHandler(document);
		xpathSaxReader.setContentHandler(sax2DomHandler);

		try {
			xpathSaxReader.parse(new InputSource(reader));
		} catch (SAXException sax) {
			throw new XmlParseException(sax);
		} catch (IOException iox) {
			throw new XmlParseException(iox);
		}

		if (errorHandler.hasErrors()) {
			throw new XmlValidationException(errorHandler.getErrors());
		}

		return document;

	}

	protected void validate(Node node, Schema schema) throws XmlValidationException {
		try {
			XmlErrorHandler errorHandler = new XmlErrorHandler(getFastFail());
			Dom2StaxStreamReader dom2StaxStreamReader = new Dom2StaxStreamReader(node);
			XPathStreamTracker xpathTracker = new XPathStreamTracker(dom2StaxStreamReader);
			errorHandler.setXPathTracker(xpathTracker);

			Validator validator = schema.newValidator();
			validator.setErrorHandler(errorHandler);
			validator.validate(new StAXSource(xpathTracker));

			if (errorHandler.hasErrors()) {
				throw new XmlValidationException(errorHandler.getErrors());
			}
		} catch (IOException x) {
			throw new XmlParseException(x);
		} catch (XMLStreamException xsx) {
			throw new XmlParseException(xsx);
		} catch (SAXException sax) {
			throw new XmlParseException(sax);
		}
	}

	protected void write(Node node, Writer writer, Schema schema) throws XmlValidationException {
		writeDom2SaxValidationHandlerSax(node, writer, schema);
	}

	/**
	 * Snad jako jedine funguje bez problemu
	 * DOM -> Dom2SaxEventGenerator -> ValidatorHandler -> XPathSaxTracker -> Sax2IndentingWriter
	 */
	private void writeDom2SaxValidationHandlerSax(Node node, Writer writer, Schema schema) throws XmlValidationException {

		XmlErrorHandler errorHandler = new XmlErrorHandler(getFastFail());
		ValidatorHandler validatorHandler = schema.newValidatorHandler();
		validatorHandler.setErrorHandler(errorHandler);

		ContentHandler xmlWriter;
		if (indenting) {
			xmlWriter = new Sax2IndentingWriter(writer, "utf-8");
		} else {
			xmlWriter = new Sax2Writer(writer, "utf-8");
		}
		XPathSaxTracker xpathTracker = new XPathSaxTracker(xmlWriter);

		validatorHandler.setContentHandler(xpathTracker);

		errorHandler.setXPathTracker(xpathTracker);

		try {
			new Dom2SaxEventGenerator(validatorHandler).scan(node);
		} catch (SAXException sax) {
			throw new XmlParseException(sax);
		}

		if (errorHandler.hasErrors()) {
			throw new XmlValidationException(errorHandler.getErrors());
		}

	}

	/**
	 * XXX Unbound namespace URI 'http://www.europa.eu/schengen/sis/xsd/v1/nsmessages'
	 */
	@SuppressWarnings("unused")
	private void writeDom2StaxValidatorStax(Node node, Writer writer, Schema schema) throws XmlValidationException {
		XmlErrorHandler errorHandler = new XmlErrorHandler(getFastFail());
		try {
			Dom2StaxStreamReader dom2StaxStreamReader = new Dom2StaxStreamReader(node);
			XPathStreamTracker xpathTracker = new XPathStreamTracker(dom2StaxStreamReader);
			errorHandler.setXPathTracker(xpathTracker);

			StringResult stringResult = new StringResult();
			XMLOutputFactory staxOutputFactory = getStaxOutputFactory().getFactory();
			XMLStreamWriter xmlStreamWriter = staxOutputFactory.createXMLStreamWriter(stringResult);
			StaxIndentingStreamWriter staxIndentWriter = new StaxIndentingStreamWriter(xmlStreamWriter);

			Validator validator = schema.newValidator();
			validator.setErrorHandler(errorHandler);
			validator.validate(new StAXSource(xpathTracker), new StAXResult(staxIndentWriter));

		} catch (SAXException sax) {
			throw new XmlParseException(sax);
		} catch (IOException iox) {
			throw new XmlParseException(iox);
		} catch (XMLStreamException xsx) {
			throw new XmlParseException(xsx);
		}

		if (errorHandler.hasErrors()) {
			throw new XmlValidationException(errorHandler.getErrors());
		}
	}

	/**
	 * XXX Unbound namespace URI 'http://www.europa.eu/schengen/sis/xsd/v1/nsmessages'
	 */
	@SuppressWarnings("unused")
	private void writeStaxValidatorStax(Node node, Writer writer, Schema schema) throws XmlValidationException {
		XmlErrorHandler errorHandler = new XmlErrorHandler(getFastFail());
		try {
			XMLInputFactory staxInputFactory = getStaxInputFactory().getFactory();
			//JAXP neumi EventReader nad DOMSource
			XMLEventReader xmlEventReader = staxInputFactory.createXMLEventReader(new DOMSource(node));
			XPathEventTracker xpathTracker = new XPathEventTracker(xmlEventReader);
			errorHandler.setXPathTracker(xpathTracker);

			XMLOutputFactory staxOutputFactory = getStaxOutputFactory().getFactory();
			XMLStreamWriter xmlStreamWriter = staxOutputFactory.createXMLStreamWriter(writer);
			if (getIndenting()) {
				xmlStreamWriter = new StaxIndentingStreamWriter(xmlStreamWriter);
			}

			Validator validator = schema.newValidator();
			validator.setErrorHandler(errorHandler);
			validator.validate(new StAXSource(xpathTracker), new StAXResult(xmlStreamWriter));

		} catch (SAXException sax) {
			throw new XmlParseException(sax);
		} catch (IOException iox) {
			throw new XmlParseException(iox);
		} catch (XMLStreamException xsx) {
			throw new XmlParseException(xsx);
		}

		if (errorHandler.hasErrors()) {
			throw new XmlValidationException(errorHandler.getErrors());
		}
	}

	/**
	 * XXX Sax2Writer duplikuje namespace daklarace kvuli locallyDeclaredPrefix
	 */
	@SuppressWarnings("unused")
	private void writeDomTraxSax(Node node, Writer writer, Schema schema, XmlErrorHandler errorHandler)
			throws XmlValidationException {

		ValidatorHandler validatorHandler = schema.newValidatorHandler();
		validatorHandler.setErrorHandler(errorHandler);
		XPathSaxTracker xpathTracker = new XPathSaxTracker(validatorHandler);
		errorHandler.setXPathTracker(xpathTracker);

		ContentHandler xmlWriter;
		if (indenting) {
			xmlWriter = new Sax2IndentingWriter(writer, "utf-8");
		} else {
			xmlWriter = new Sax2Writer(writer, "utf-8");
		}
		validatorHandler.setContentHandler(xmlWriter);
		Transformer transformer = getTraxFactory().newTransformer();
		transformer.setErrorListener(errorHandler);

		try {
			transformer.transform(new DOMSource(node), new SAXResult(xpathTracker));
		} catch (TransformerException trx) {
			if (trx.getException() != null) {
				throw new XmlParseException(trx.getException());
			} else {
				throw new XmlParseException(trx);
			}
		}

		if (errorHandler.getErrors().size() > 0) {
			throw new XmlValidationException(errorHandler.getErrors());
		}
	}

	public boolean xpathBoolean(String strXpath, Node node) {
		return (Boolean) xpath(strXpath, node, XPathConstants.BOOLEAN);
	}

	public String xpathString(String strXpath, Node node) {
		Node selected = (Node) xpath(strXpath, node, XPathConstants.NODE);
		if (selected != null) {
			return selected.getTextContent();
		}
		return null;
		/*
		String string = (String) xpath(strXpath, dom, XPathConstants.STRING);
		if (string != null && string.length() == 0) {
			//JDK returns "" even if Xpath does not even exist in xml !?!
			//this makes impossible to distinguish case that node exist with blank value "" 
			//It is better to test with existence xpathBoolean or xpathNode
			return null;
		} else {
			return string;
		}
		*/
	}

	public Node xpathNode(String strXpath, Node node) {
		return (Node) xpath(strXpath, node, XPathConstants.NODE);
	}

	public List<Node> xpath(String strXpath, Node node) {
		NodeList nodeList = (NodeList) xpath(strXpath, node, XPathConstants.NODESET);
		//Convert ugly w3c NodeList to standard java List
		List<Node> ret = new ArrayList<Node>(nodeList.getLength());
		for (int i = 0; i < nodeList.getLength(); ++i) {
			ret.add(nodeList.item(i));
		}
		return ret;
	}

	public Object xpath(String strXpath, Node node, QName qname) {
		if (StringUtils.isBlank(strXpath)) {
			throw new IllegalArgumentException("Invalid xpath string: " + strXpath);
		}
		if (node == null) {
			throw new IllegalArgumentException("Null node");
		}
		if (qname == null) {
			throw new IllegalArgumentException("Null qname");
		}
		XPath xpath = getXpathFactory().newXPath();
		if (namespaceContext == null) {
			throw new IllegalStateException("NamespaceContext is not configured. Xpath will not work");
		}
		xpath.setNamespaceContext(namespaceContext);
		XPathExpression expr;
		try {
			expr = xpath.compile(strXpath);
		} catch (XPathExpressionException xpx) {
			throw new UnhandledException(xpx);
		}

		try {
			return expr.evaluate(node, qname);
		} catch (XPathExpressionException xpx) {
			throw new UnhandledException(xpx);
		}
	}

	/*
		public boolean xpathBoolean(String strXpath, Node dom) {
			return (Boolean) xpath(strXpath, dom, XPathConstants.BOOLEAN);
		}

		public String xpathString(String strXpath, Node dom) {
			return (String) xpath(strXpath, dom, XPathConstants.STRING);
		}

		public Node xpathNode(String strXpath, Node dom) {
			return (Node) xpath(strXpath, dom, XPathConstants.NODE);
		}

		@SuppressWarnings("unchecked")
		public List<Node> xpath(String strXpath, Node dom) {
			return (List<Node>) xpath(strXpath, dom, XPathConstants.NODESET);
		}

		public Object xpath(String strXpath, Node dom, QName qname) {
			try {
				DOMXPath jxpath = new org.jaxen.dom.DOMXPath(strXpath);
				//jxpath.setNamespaceContext(new XmlNamespaceContext(dom));
				jxpath.addNamespace("siscsmsg", SisNamespaceMapper.NS_ICD_CSMSG);
				jxpath.addNamespace("sisnsmsg", SisNamespaceMapper.NS_ICD_NSMSG);
				jxpath.addNamespace("sisdt", SisNamespaceMapper.NS_ICD_COMMON);
				jxpath.addNamespace("sisalertdt", SisNamespaceMapper.NS_ICD_ALERT);
				jxpath.addNamespace("sislinkdt", SisNamespaceMapper.NS_ICD_LINK);
				jxpath.addNamespace("sisflagdt", SisNamespaceMapper.NS_ICD_FLAG);
				Navigator navigator = jxpath.getNavigator();
				List<Node> nodes = jxpath.selectNodes(dom);

				if (qname.equals(XPathConstants.NODESET)) {
					return nodes;
				} else if (nodes.size() > 1) {
					throw new IllegalArgumentException("Xpath vybral " + nodes.size() + " vysledku " + strXpath);
				} else if (nodes.isEmpty()) {
					return null;
				}

				Node node = nodes.get(0);
				if (qname.equals(XPathConstants.NODE)) {
					return nodes.get(0);
				} else if (qname.equals(XPathConstants.NUMBER)) {
					return NumberFunction.evaluate(node, navigator);
				} else if (qname.equals(XPathConstants.STRING)) {
					return StringFunction.evaluate(node, navigator);
				} else if (qname.equals(XPathConstants.BOOLEAN)) {
					return BooleanFunction.evaluate(node, navigator);
				} else {
					throw new IllegalArgumentException("Neplatny XPath 1.0 NodeSet data type " + qname);
				}
			} catch (JaxenException jx) {
				throw new NonSolvableException(jx);
			}
		}
	*/

}
