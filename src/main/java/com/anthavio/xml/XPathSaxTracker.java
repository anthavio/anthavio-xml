package com.anthavio.xml;

import java.util.Stack;

import javax.xml.stream.Location;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * @author vanek
 * 
 * Sax filter and Stax reader/writer tracking xpath of processed xml
 * {@link #getXPath()} {@link #getLocation()}
 */
public class XPathSaxTracker extends XMLFilterImpl implements XPathTracker {

	private final Stack<XpathHolder> histograms = new Stack<XpathHolder>();

	private Location location;

	/**
	 * org.xml.sax.ContentHandler (write)
	 * 
	 * @param parentSaxHandler Parent ContentHandler to write SAX Events into
	 */
	public XPathSaxTracker(ContentHandler parentSaxHandler) {
		setContentHandler(parentSaxHandler);
	}

	/**
	 * org.xml.sax.helpers.XMLFilterImpl (read)
	 * 
	 * @param parentSaxReader Parent XMLReader to read SAX Events from
	 */
	public XPathSaxTracker(XMLReader parentSaxReader) {
		super(parentSaxReader);
	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		histograms.clear();
		histograms.push(new XpathHolder());
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		int idx = qName.indexOf(':');
		String prefix;
		if (idx != -1) {
			prefix = qName.substring(0, idx);
		} else {
			prefix = qName;
		}
		histograms.peek().update(uri, localName, prefix);
		histograms.push(new XpathHolder());
		//histograms.push(new Histogram(uri, localName, prefix));

		super.startElement(uri, localName, qName, atts);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);
		histograms.pop();
	}

	public String getXPath() {
		StringBuilder buf = new StringBuilder();
		for (XpathHolder h : histograms) {
			h.appendPath(buf);
		}
		return buf.toString();
	}

	public Location getLocation() {
		return this.location;
	}

}
