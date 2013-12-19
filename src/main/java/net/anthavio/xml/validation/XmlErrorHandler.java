package net.anthavio.xml.validation;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import net.anthavio.xml.XPathTracker;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * @author vanek
 *
 */
public class XmlErrorHandler implements ValidationEventHandler, ErrorHandler, ErrorListener {

	private final boolean failFast;

	private XPathTracker xpathTracker;

	private final List<ValidationEventImpl> warnings = new ArrayList<ValidationEventImpl>();

	private final List<ValidationEventImpl> errors = new ArrayList<ValidationEventImpl>();

	public XmlErrorHandler(XPathTracker xpathTracker, boolean failFast) {
		this.xpathTracker = xpathTracker;
		this.failFast = failFast;
	}

	public XmlErrorHandler(XPathTracker xpathTracker) {
		this.xpathTracker = xpathTracker;
		this.failFast = true;
	}

	public XmlErrorHandler() {
		this.xpathTracker = null;
		this.failFast = true;
	}

	public XmlErrorHandler(boolean failFast) {
		this.xpathTracker = null;
		this.failFast = failFast;
	}

	public void setXPathTracker(XPathTracker xpathTracker) {
		this.xpathTracker = xpathTracker;
	}

	public boolean hasErrors() {
		return errors.size() != 0;
	}

	public List<ValidationEventImpl> getWarnings() {
		return warnings;
	}

	public List<ValidationEventImpl> getErrors() {
		return errors;
	}

	public List<ValidationEventImpl> getEvents() {
		List<ValidationEventImpl> ret = new ArrayList<ValidationEventImpl>(errors.size() + warnings.size());
		ret.addAll(errors);
		ret.addAll(warnings);
		return ret;
	}

	public void reset() {
		errors.clear();
		warnings.clear();
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	/**
	 * java.xml.bind.ValidationEventHandler
	 */
	public boolean handleEvent(ValidationEvent ve) {
		String xpath = (xpathTracker == null) ? null : xpathTracker.getXPath();
		ValidationEventImpl event = new ValidationEventImpl(ve.getLinkedException(), ve.getMessage(), ve.getSeverity(),
				ve.getLocator(), xpath);
		if (event.getSeverity() == ValidationEvent.WARNING) {
			warnings.add(event);
			return true; //continue on warning
		} else {
			errors.add(event);
			return !failFast;
		}
	}

	public ValidationEventImpl addSaxException(SAXParseException sx, int level) {
		String xpath = (xpathTracker == null) ? null : xpathTracker.getXPath();
		ValidationEventImpl event = new ValidationEventImpl(sx, level, xpath);
		errors.add(event);
		return event;
	}

	/**
	 * org.xml.sax.ErrorHandler
	 */
	public void error(SAXParseException sx) throws SAXException {
		addSaxException(sx, ValidationEvent.ERROR);
		if (failFast) {
			throw sx;
		}
	}

	/**
	 * org.xml.sax.ErrorHandler
	 */
	public void fatalError(SAXParseException sx) throws SAXException {
		addSaxException(sx, ValidationEvent.FATAL_ERROR);
		if (failFast) {
			throw sx;
		}
	}

	/**
	 * org.xml.sax.ErrorHandler
	 */
	public void warning(SAXParseException sx) throws SAXException {
		String xpath = (xpathTracker == null) ? null : xpathTracker.getXPath();
		ValidationEventImpl event = new ValidationEventImpl(sx, ValidationEvent.WARNING, xpath);
		warnings.add(event);
	}

	/**
	 * javax.xml.transform.ErrorListener
	 */
	public void warning(TransformerException tx) throws TransformerException {
		String xpath = (xpathTracker == null) ? null : xpathTracker.getXPath();
		ValidationEventImpl event = new ValidationEventImpl(tx, ValidationEvent.WARNING, xpath);
		warnings.add(event);
	}

	/**
	 * javax.xml.transform.ErrorListener
	 */
	public void error(TransformerException tx) throws TransformerException {
		String xpath = (xpathTracker == null) ? null : xpathTracker.getXPath();
		ValidationEventImpl event = new ValidationEventImpl(tx, ValidationEvent.ERROR, xpath);
		errors.add(event);
		if (failFast) {
			throw tx;
		}
	}

	/**
	 * javax.xml.transform.ErrorListener
	 */
	public void fatalError(TransformerException tx) throws TransformerException {
		String xpath = (xpathTracker == null) ? null : xpathTracker.getXPath();
		ValidationEventImpl event = new ValidationEventImpl(tx, ValidationEvent.FATAL_ERROR, xpath);
		errors.add(event);
		if (failFast) {
			throw tx;
		}
	}

}