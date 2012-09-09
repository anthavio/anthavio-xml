/**
 * 
 */
package com.anthavio.xml;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

/**
 * @author vanek
 * 
 * RuntimeException wrapper JAXP xml parser konfiguracnich vyjimek
 */
public class JaxpConfigException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public JaxpConfigException(String message) {
		super(message);
	}

	public JaxpConfigException(ParserConfigurationException x) {
		super(x);
	}

	public JaxpConfigException(SAXException x) {
		super(x);
	}

	public JaxpConfigException(TransformerConfigurationException x) {
		super(x);
	}

	public JaxpConfigException(JAXBException x) {
		super(x);
	}

	public JaxpConfigException(Throwable t) {
		super(t);
	}
}
