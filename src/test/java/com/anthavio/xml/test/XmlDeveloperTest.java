package com.anthavio.xml.test;

import java.io.StringReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * 
 * @author martin.vanek
 *
 */
public class XmlDeveloperTest {

	static String xml = "<x><y id='zzz'>http://www.xxx.com?pname=pvalue&another=else</y></x>";

	public static void main(String[] args) {

		DefaultHandler handler = new DefaultHandler() {

			boolean bscript;

			public String value;

			@Override
			public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes)
					throws SAXException {
				String id = attributes.getValue("id");
				if (qName.equalsIgnoreCase("y") && "zzz".equals(id)) {
					this.bscript = true;
				}
			}

			@Override
			public void characters(char ch[], int start, int length) throws SAXException {
				if (this.bscript) {
					this.value = new String(ch, start, length);
					System.out.println(this.value);
				}
			}
		};
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://xml.org/sax/features/external-general-entities", true);
			factory.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();

			MyHandler myHandler = new MyHandler();
			reader.setContentHandler(myHandler);
			reader.setErrorHandler(myHandler);
			reader.setProperty("http://xml.org/sax/properties/lexical-handler", myHandler);
			reader.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
			reader.parse(new InputSource(new StringReader(xml)));
			if (true) {
				return;
			}

			XMLFilterEntityImpl xmlFilterEntityImpl = new XMLFilterEntityImpl(reader);
			xmlFilterEntityImpl.setErrorHandler(new MyErrorHandler());
			xmlFilterEntityImpl.setProperty("http://xml.org/sax/properties/lexical-handler", xmlFilterEntityImpl);
			//reader.setProperty("http://xml.org/sax/properties/lexical-handler", new XMLFilterEntityImpl(reader));
			xmlFilterEntityImpl.setFeature("http://xml.org/sax/features/external-general-entities", false);
			xmlFilterEntityImpl.setContentHandler(handler);
			xmlFilterEntityImpl.parse(new InputSource(new StringReader(xml)));

			//System.out.println(handler.value);
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

}

class MyHandler extends DefaultHandler implements LexicalHandler {

	private String currentEntity = null;

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		System.out.println(e);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String content = new String(ch, start, length);
		if (this.currentEntity != null) {
			content = "&" + this.currentEntity + content;
			this.currentEntity = null;
		}
		System.out.println(content);
	}

	@Override
	public void startEntity(String name) throws SAXException {
		this.currentEntity = name;
	}

	@Override
	public void endEntity(String name) throws SAXException {
	}

	@Override
	public void startDTD(String name, String publicId, String systemId) throws SAXException {
	}

	@Override
	public void endDTD() throws SAXException {
	}

	@Override
	public void startCDATA() throws SAXException {
	}

	@Override
	public void endCDATA() throws SAXException {
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
	}
}

class XMLFilterEntityImpl extends XMLFilterImpl implements LexicalHandler {

	private String currentEntity = null;

	public XMLFilterEntityImpl(XMLReader reader) throws SAXNotRecognizedException, SAXNotSupportedException {
		super(reader);
		setProperty("http://xml.org/sax/properties/lexical-handler", this);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (this.currentEntity == null) {
			System.out.println("characters");
			super.characters(ch, start, length);
			return;
		} else {
			String content = new String(ch, start, length);
			String entity = "&" + this.currentEntity + content;
			System.out.println("entity");
			super.characters(entity.toCharArray(), 0, entity.length());
		}

		this.currentEntity = null;
	}

	@Override
	public void startEntity(String name) throws SAXException {
		System.out.println("startEntity " + name);
		this.currentEntity = name;
	}

	@Override
	public void endEntity(String name) throws SAXException {
	}

	@Override
	public void startDTD(String name, String publicId, String systemId) throws SAXException {
	}

	@Override
	public void endDTD() throws SAXException {
	}

	@Override
	public void startCDATA() throws SAXException {
	}

	@Override
	public void endCDATA() throws SAXException {
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
	}
}

class MyLexicalHandler implements LexicalHandler {

	@Override
	public void startDTD(String name, String publicId, String systemId) throws SAXException {
		System.out.println("startDTD");
	}

	@Override
	public void endDTD() throws SAXException {
		System.out.println("endDTD");
	}

	@Override
	public void startEntity(String name) throws SAXException {
		System.out.println("startEntity " + name);
	}

	@Override
	public void endEntity(String name) throws SAXException {
		System.out.println("endEntity " + name);
	}

	@Override
	public void startCDATA() throws SAXException {
		System.out.println("startCDATA");
	}

	@Override
	public void endCDATA() throws SAXException {
		System.out.println("endCDATA");

	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		System.out.println("comment");
	}

}

class MyErrorHandler implements ErrorHandler {

	@Override
	public void warning(SAXParseException exception) throws SAXException {
		System.out.println("warning " + exception);
	}

	@Override
	public void error(SAXParseException exception) throws SAXException {
		System.out.println("error " + exception);

	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		System.out.println("fatalError " + exception);
	}

}