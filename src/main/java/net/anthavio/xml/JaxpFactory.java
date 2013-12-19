/**
 * 
 */
package net.anthavio.xml;

import net.anthavio.xml.JaxpDomFactory.DomImplementation;
import net.anthavio.xml.JaxpSaxFactory.SaxImplementation;
import net.anthavio.xml.JaxpTraxFactory.TraxImplementation;
import net.anthavio.xml.JaxpXpathFactory.XpathImplementation;
import net.anthavio.xml.stax.JaxpStaxFactory;
import net.anthavio.xml.stax.JaxpStaxFactory.StaxImplementation;
import net.anthavio.xml.validation.JaxpSchemaFactory;
import net.anthavio.xml.validation.JaxpSchemaFactory.SchemaImplementation;

/**
 * @author vanek
 *
 */
public class JaxpFactory {

	public enum ParserImplementation {
		XERCES, JDK;
	}

	private final JaxpSaxFactory saxFactory;

	private final JaxpDomFactory domFactory;

	private final JaxpSchemaFactory schemaFactory;

	private final JaxpTraxFactory traxFactory;

	private final JaxpXpathFactory xpathFactory;

	private final JaxpStaxFactory staxFactory;

	public JaxpFactory(JaxpSaxFactory saxFactory, JaxpDomFactory domFactory, JaxpSchemaFactory schemaFactory,
			JaxpTraxFactory traxFactory, JaxpXpathFactory xpathFactory, JaxpStaxFactory staxFactory) {
		this.saxFactory = saxFactory;
		this.domFactory = domFactory;
		this.schemaFactory = schemaFactory;
		this.traxFactory = traxFactory;
		this.xpathFactory = xpathFactory;
		this.staxFactory = staxFactory;
	}

	public JaxpFactory(SaxImplementation saxImplementation, DomImplementation domImplementation,
			SchemaImplementation schemaImplementation, TraxImplementation traxImplementation,
			XpathImplementation xpathImplementation, StaxImplementation staxImplementation) {
		this.saxFactory = new JaxpSaxFactory(saxImplementation);
		this.domFactory = new JaxpDomFactory(domImplementation);
		this.schemaFactory = new JaxpSchemaFactory(schemaImplementation);
		this.traxFactory = new JaxpTraxFactory(traxImplementation);
		this.xpathFactory = new JaxpXpathFactory(xpathImplementation);
		this.staxFactory = new JaxpStaxFactory(staxImplementation);
	}

	public JaxpFactory(String saxFactoryClassName, String domFactoryClassName, String schemaFactoryClassName,
			String traxFactoryClassName, String xpathFactoryClassName, String staxInputFactoryClassName,
			String staxOutputFactoryClassName, String staxEventFactoryClassName) {
		this.saxFactory = new JaxpSaxFactory(saxFactoryClassName);
		this.domFactory = new JaxpDomFactory(domFactoryClassName);
		this.schemaFactory = new JaxpSchemaFactory(schemaFactoryClassName);
		this.traxFactory = new JaxpTraxFactory(traxFactoryClassName);
		this.xpathFactory = new JaxpXpathFactory(xpathFactoryClassName);
		this.staxFactory = new JaxpStaxFactory(staxInputFactoryClassName, staxOutputFactoryClassName,
				staxEventFactoryClassName);
	}

	public JaxpSaxFactory getSaxFactory() {
		return saxFactory;
	}

	public JaxpDomFactory getDomFactory() {
		return domFactory;
	}

	public JaxpSchemaFactory getSchemaFactory() {
		return schemaFactory;
	}

	public JaxpTraxFactory getTraxFactory() {
		return traxFactory;
	}

	public JaxpXpathFactory getXpathFactory() {
		return xpathFactory;
	}

	public JaxpStaxFactory getStaxFactory() {
		return staxFactory;
	}

}
