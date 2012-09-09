/**
 * 
 */
package com.anthavio.xml.validation;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Node;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

/**
 * @author vanek
 *
 * Stores java.xml.bind.ValidationEvent and org.xml.sax.SAXParseException error data
 */
public class ValidationEventImpl implements ValidationEvent {

	private final Throwable exception;

	private final String message;

	private final int severity;

	private final String xpath;

	private final ValidationEventLocator locator;

	/**
	 * org.xml.sax
	 */
	public ValidationEventImpl(SAXParseException sx, int severity, String xpath) {
		this.exception = sx;
		this.message = sx.getMessage();
		this.severity = severity;
		this.xpath = xpath;
		this.locator = new ValidationEventLocatorImpl(sx);
	}

	/**
	 * org.xml.sax
	 */
	public ValidationEventImpl(TransformerException tx, int severity, String xpath) {
		this.exception = tx;
		this.message = tx.getMessage();
		this.severity = severity;
		this.xpath = xpath;
		this.locator = new ValidationEventLocatorImpl(tx);
	}

	/**
	 * java.xml.bind
	 */
	public ValidationEventImpl(Throwable linkedException, String message, int severity, ValidationEventLocator locator,
			String xpath) {
		this.exception = linkedException;
		this.message = message;
		this.severity = severity;
		this.xpath = xpath;
		this.locator = new ValidationEventLocatorImpl(locator);
	}

	public Throwable getLinkedException() {
		return exception;
	}

	public String getMessage() {
		return message;
	}

	public int getSeverity() {
		return severity;
	}

	public String getXpath() {
		return xpath;
	}

	public ValidationEventLocator getLocator() {
		return locator;
	}

	@Override
	public String toString() {
		String severity;
		switch (getSeverity()) {
		case WARNING:
			severity = "WARNING";
			break;
		case ERROR:
			severity = "ERROR";
			break;
		case FATAL_ERROR:
			severity = "FATAL";
			break;
		default:
			severity = String.valueOf(getSeverity());
			break;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(severity);
		if (!locator.toString().equals("")) {
			sb.append(", ").append(locator);
		}
		if (xpath != null) {
			sb.append(", ").append("XPath: ").append(xpath);
		}
		sb.append(", ").append("Message: ").append(message);
		return sb.toString();
	}

	@Override
	public boolean equals(Object that) {
		return EqualsBuilder.reflectionEquals(this, that);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
}

class ValidationEventLocatorImpl implements ValidationEventLocator {

	private int lineNumber = -1;

	private int columnNumber = -1;

	private int offset = -1;

	private Node node;

	private Object object;

	private URL url;

	public ValidationEventLocatorImpl(SAXParseException sx) {
		this.url = toURL(sx.getSystemId());
		this.columnNumber = sx.getColumnNumber();
		this.lineNumber = sx.getLineNumber();
	}

	public ValidationEventLocatorImpl(TransformerException tx) {
		SourceLocator locator = tx.getLocator();
		if (locator != null) {
			this.url = toURL(locator.getSystemId());
			this.columnNumber = locator.getColumnNumber();
			this.lineNumber = locator.getLineNumber();
		}
	}

	public ValidationEventLocatorImpl(Locator locator) {
		this.url = toURL(locator.getSystemId());
		this.columnNumber = locator.getColumnNumber();
		this.lineNumber = locator.getLineNumber();
	}

	public ValidationEventLocatorImpl(ValidationEventLocator locator) {
		this.lineNumber = locator.getLineNumber();
		this.columnNumber = locator.getColumnNumber();
		this.offset = locator.getOffset();
		this.node = locator.getNode();
		this.object = locator.getObject();
		this.url = locator.getURL();
	}

	public ValidationEventLocatorImpl(int lineNumber, int columnNumber, int offset, Node node, Object object, URL url) {
		this.lineNumber = lineNumber;
		this.columnNumber = columnNumber;
		this.offset = offset;
		this.node = node;
		this.object = object;
		this.url = url;
	}

	public int getColumnNumber() {
		return columnNumber;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public Node getNode() {
		return node;
	}

	public Object getObject() {
		return object;
	}

	public int getOffset() {
		return offset;
	}

	public URL getURL() {
		return url;
	}

	private static URL toURL(String systemId) {
		try {
			return new URL(systemId);
		} catch (MalformedURLException e) {
			return null; // for now
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (lineNumber != -1) {
			sb.append("Line: ").append(lineNumber);
		}
		if (columnNumber != -1) {
			sb.append(", Column: ").append(columnNumber);
		}
		if (offset != -1) {
			sb.append(", Offset: ").append(offset);
		}
		if (node != null) {
			sb.append(", Node: ").append(node);
		}
		if (object != null) {
			sb.append(", Object: ").append(object);
		}
		if (url != null) {
			sb.append(", Url: ").append(url);
		}
		return sb.toString();
	}

}