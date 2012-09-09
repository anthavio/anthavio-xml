/**
 * 
 */
package com.anthavio.xml.stax;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;

import com.anthavio.xml.JaxpAbstractFactory;


/**
 * @author vanek
 *
 * http://jaxp.java.net/1.4/JAXP-Compatibility.html#STAX
 */
public class JaxpStaxInputFactory extends JaxpAbstractFactory<XMLInputFactory> {

	public static final String STAX_INPUT_FACTORY_JAXP = "com.sun.xml.internal.stream.XMLInputFactoryImpl";
	public static final String STAX_INPUT_FACTORY_WOODSTOX = "com.ctc.wstx.stax.WstxInputFactory";
	public static final String STAX_INPUT_FACTORY_WEBLOGIC = "weblogic.xml.stax.XMLStreamInputFactory";

	public enum StaxInputImplementation {
		JDK(STAX_INPUT_FACTORY_JAXP), WOODSTOX(STAX_INPUT_FACTORY_WOODSTOX), WEBLOGIC(STAX_INPUT_FACTORY_WEBLOGIC);

		private final String factoryClassName;

		private StaxInputImplementation(String factoryClassName) {
			this.factoryClassName = factoryClassName;
		}

		public String getFactoryClassName() {
			return factoryClassName;
		}
	}

	private HashMap<String, Object> factoryProperties;

	public JaxpStaxInputFactory(String factoryClassName) {
		super(factoryClassName);
	}

	public JaxpStaxInputFactory(StaxInputImplementation implementation) {
		super(implementation.getFactoryClassName());
	}

	public JaxpStaxInputFactory() {
		super();
	}

	@Override
	protected XMLInputFactory buildDefaultFactory() {
		//XMLInputFactory.newFactory() is added in stax-api 1.0.1 shipped with JDK 1.6.0.18
		//XMLInputFactory.newInstance() does exactly the same as XMLInputFactory.newFactory()
		return XMLInputFactory.newInstance();
	}

	@Override
	protected void configureFactoryInstance(XMLInputFactory factory) {
		if (factoryProperties != null) {
			Set<Entry<String, Object>> entrySet = factoryProperties.entrySet();
			for (Entry<String, Object> entry : entrySet) {
				factory.setProperty(entry.getKey(), entry.getValue());
			}
		}
	}

	public HashMap<String, Object> getFactoryProperties() {
		return factoryProperties;
	}

	public void setFactoryProperties(HashMap<String, Object> factoryProperties) {
		this.factoryProperties = factoryProperties;
	}

}
