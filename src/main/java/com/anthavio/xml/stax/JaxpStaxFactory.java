/**
 * 
 */
package com.anthavio.xml.stax;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import com.anthavio.NotSupportedException;
import com.anthavio.xml.stax.JaxpStaxEventFactory.StaxEventImplementation;
import com.anthavio.xml.stax.JaxpStaxInputFactory.StaxInputImplementation;
import com.anthavio.xml.stax.JaxpStaxOutputFactory.StaxOutputImplementation;

/**
 * @author vanek
 *
 * http://jaxp.java.net/1.4/JAXP-Compatibility.html#STAX
 */
public class JaxpStaxFactory {

	public enum StaxImplementation {
		JDK, WOODSTOX, WEBLOGIC;
	}

	private final JaxpStaxInputFactory inputFactory;

	private final JaxpStaxOutputFactory outputFactory;

	private final JaxpStaxEventFactory eventFactory;

	public JaxpStaxFactory() {
		this.inputFactory = new JaxpStaxInputFactory();
		this.outputFactory = new JaxpStaxOutputFactory();
		this.eventFactory = new JaxpStaxEventFactory();
	}

	public JaxpStaxFactory(String staxInputFactoryClassName, String staxOutputFactoryClassName,
			String staxEventFactoryClassName) {
		this.inputFactory = new JaxpStaxInputFactory(staxInputFactoryClassName);
		this.outputFactory = new JaxpStaxOutputFactory(staxOutputFactoryClassName);
		this.eventFactory = new JaxpStaxEventFactory(staxEventFactoryClassName);
	}

	public JaxpStaxFactory(StaxImplementation implementation) {
		switch (implementation) {
		case JDK:
			this.inputFactory = new JaxpStaxInputFactory(StaxInputImplementation.JDK);
			this.outputFactory = new JaxpStaxOutputFactory(StaxOutputImplementation.JDK);
			this.eventFactory = new JaxpStaxEventFactory(StaxEventImplementation.JDK);
			break;
		case WOODSTOX:
			this.inputFactory = new JaxpStaxInputFactory(StaxInputImplementation.WOODSTOX);
			this.outputFactory = new JaxpStaxOutputFactory(StaxOutputImplementation.WOODSTOX);
			this.eventFactory = new JaxpStaxEventFactory(StaxEventImplementation.WOODSTOX);
			break;
		case WEBLOGIC:
			this.inputFactory = new JaxpStaxInputFactory(StaxInputImplementation.WEBLOGIC);
			this.outputFactory = new JaxpStaxOutputFactory(StaxOutputImplementation.WEBLOGIC);
			this.eventFactory = new JaxpStaxEventFactory(StaxEventImplementation.WEBLOGIC);
			break;
		default:
			throw new NotSupportedException(implementation);
		}
	}

	public XMLOutputFactory getXMLOutputFactory() {
		return outputFactory.getFactory();
	}

	public XMLInputFactory getXMLInputFactory() {
		return inputFactory.getFactory();
	}

	public XMLEventFactory getXMLEventFactory() {
		return eventFactory.getFactory();
	}

}
