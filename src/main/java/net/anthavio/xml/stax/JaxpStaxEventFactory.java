/**
 * 
 */
package net.anthavio.xml.stax;

import javax.xml.stream.XMLEventFactory;

import net.anthavio.xml.JaxpAbstractFactory;



/**
 * @author vanek
 *
 * http://jaxp.java.net/1.4/JAXP-Compatibility.html#STAX
 */
public class JaxpStaxEventFactory extends JaxpAbstractFactory<XMLEventFactory> {

	public static final String STAX_EVENT_FACTORY_JAXP = "com.sun.xml.internal.stream.events.XMLEventFactoryImpl";
	public static final String STAX_EVENT_FACTORY_WOODSTOX = "com.ctc.wstx.stax.WstxEventFactory";
	public static final String STAX_EVENT_FACTORY_WEBLOGIC = "weblogic.xml.stax.EventFactory";

	//public static final String STAX_EVENT_FACTORY_XERCES = "org.apache.xerces.stax.XMLEventFactoryImpl";

	public enum StaxEventImplementation {
		JDK(STAX_EVENT_FACTORY_JAXP), WOODSTOX(STAX_EVENT_FACTORY_WOODSTOX), WEBLOGIC(STAX_EVENT_FACTORY_WEBLOGIC);

		private final String factoryClassName;

		private StaxEventImplementation(String factoryClassName) {
			this.factoryClassName = factoryClassName;
		}

		public String getFactoryClassName() {
			return factoryClassName;
		}
	}

	public JaxpStaxEventFactory() {
		super();
	}

	public JaxpStaxEventFactory(String factoryClassName) {
		super(factoryClassName);
	}

	public JaxpStaxEventFactory(StaxEventImplementation implementation) {
		super(implementation.getFactoryClassName());
	}

	@Override
	protected XMLEventFactory buildDefaultFactory() {
		return XMLEventFactory.newInstance();
	}

	@Override
	protected void configureFactoryInstance(XMLEventFactory factory) {
		//nothing to set
	}

}
