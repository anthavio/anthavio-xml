/**
 * 
 */
package net.anthavio.xml.stax;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;

import net.anthavio.xml.JaxpAbstractFactory;



/**
 * @author vanek
 *
 * http://jaxp.java.net/1.4/JAXP-Compatibility.html#STAX
 */
public class JaxpStaxOutputFactory extends JaxpAbstractFactory<XMLOutputFactory> {

	public static final String STAX_OUTPUT_FACTORY_JAXP = "com.sun.xml.internal.stream.XMLOutputFactoryImpl";
	public static final String STAX_OUTPUT_FACTORY_WOODSTOX = "com.ctc.wstx.stax.WstxOutputFactory";
	public static final String STAX_OUTPUT_FACTORY_WEBLOGIC = "weblogic.xml.stax.XMLStreamOutputFactory";

	public enum StaxOutputImplementation {
		JDK(STAX_OUTPUT_FACTORY_JAXP), WOODSTOX(STAX_OUTPUT_FACTORY_WOODSTOX), WEBLOGIC(STAX_OUTPUT_FACTORY_WEBLOGIC);

		private final String factoryClassName;

		private StaxOutputImplementation(String factoryClassName) {
			this.factoryClassName = factoryClassName;
		}

		public String getFactoryClassName() {
			return factoryClassName;
		}
	}

	private HashMap<String, Object> staxOutputFactoryProperties;

	public JaxpStaxOutputFactory() {
		super();
	}

	public JaxpStaxOutputFactory(String factoryClassName) {
		super(factoryClassName);
	}

	public JaxpStaxOutputFactory(StaxOutputImplementation implementation) {
		super(implementation.getFactoryClassName());
	}

	@Override
	protected XMLOutputFactory buildDefaultFactory() {
		//XMLOutputFactory.newFactory() is added in stax-api 1.0.1 shipped with JDK 1.6.0.18
		//XMLOutputFactory.newInstance() does exactly the same as XMLOutputFactory.newFactory()
		return XMLOutputFactory.newInstance();
	}

	@Override
	protected void configureFactoryInstance(XMLOutputFactory factory) {
		if (staxOutputFactoryProperties != null) {
			Set<Entry<String, Object>> entrySet = staxOutputFactoryProperties.entrySet();
			for (Entry<String, Object> entry : entrySet) {
				factory.setProperty(entry.getKey(), entry.getValue());
			}
		}
	}

	public HashMap<String, Object> getStaxOutputFactoryProperties() {
		return staxOutputFactoryProperties;
	}

	public void setStaxOutputFactoryProperties(HashMap<String, Object> staxOutputFactoryProperties) {
		this.staxOutputFactoryProperties = staxOutputFactoryProperties;
	}

}
