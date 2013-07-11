/**
 * 
 */
package com.anthavio.xml;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;

/**
 * @author vanek
 *
 */
public class JaxpTraxFactory extends JaxpAbstractFactory<TransformerFactory> {

	public static final String TRAX_FACTORY_JAXP = "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl";
	public static final String TRAX_FACTORY_XALAN = "org.apache.xalan.processor.TransformerFactoryImpl";
	public static final String TRAX_FACTORY_SAXON = "net.sf.saxon.TransformerFactoryImpl";

	public enum TraxImplementation {
		JDK(TRAX_FACTORY_JAXP), XALAN(TRAX_FACTORY_XALAN), SAXON(TRAX_FACTORY_SAXON);

		private final String factoryClassName;

		private TraxImplementation(String factoryClassName) {
			this.factoryClassName = factoryClassName;
		}

		public String getFactoryClassName() {
			return factoryClassName;
		}
	}

	private HashMap<String, Boolean> factoryFeatures;

	private HashMap<String, Object> factoryAttributes;

	private HashMap<String, Object> transformerParameters;

	private HashMap<String, String> outputProperties;

	private URIResolver uriResolver;

	public JaxpTraxFactory() {
		super();
	}

	public JaxpTraxFactory(String factoryClassName) {
		super(factoryClassName);
	}

	public JaxpTraxFactory(TraxImplementation implementation) {
		super(implementation.getFactoryClassName());
	}

	public Transformer newTransformer() {
		Transformer transformer;
		try {
			transformer = getFactory().newTransformer();
		} catch (TransformerConfigurationException tcx) {
			throw new JaxpConfigException(tcx);
		}

		if (transformerParameters != null) {
			Set<Entry<String, Object>> entrySet = transformerParameters.entrySet();
			for (Entry<String, Object> entry : entrySet) {
				transformer.setParameter(entry.getKey(), entry.getValue());
			}
		}
		if (outputProperties != null) {
			Set<Entry<String, String>> entrySet = outputProperties.entrySet();
			for (Entry<String, String> entry : entrySet) {
				transformer.setOutputProperty(entry.getKey(), entry.getValue());
			}
		}
		if (uriResolver != null) {
			transformer.setURIResolver(uriResolver);
		}
		return transformer;
	}

	@Override
	protected TransformerFactory buildDefaultFactory() {
		return TransformerFactory.newInstance();
	}

	@Override
	protected void configureFactoryInstance(TransformerFactory factory) {
		try {
			if (factoryFeatures != null) {
				//XXX http://bdoughan.blogspot.com/2011/03/preventing-entity-expansion-attacks-in.html
				//documentFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
				Set<Entry<String, Boolean>> entrySet = factoryFeatures.entrySet();
				for (Entry<String, Boolean> entry : entrySet) {
					factory.setFeature(entry.getKey(), entry.getValue());
				}
			}

			if (factoryAttributes != null) {
				Set<Entry<String, Object>> entrySet = factoryAttributes.entrySet();
				for (Entry<String, Object> entry : entrySet) {
					factory.setAttribute(entry.getKey(), entry.getValue());
				}
			}
		} catch (TransformerConfigurationException tcx) {
			throw new JaxpConfigException(tcx);
		}
	}

	//checked factory setters

	public HashMap<String, Boolean> getFactoryFeatures() {
		return factoryFeatures;
	}

	public void setFactoryFeatures(HashMap<String, Boolean> factoryFeatures) {
		checkState();
		this.factoryFeatures = factoryFeatures;
	}

	public HashMap<String, Object> getFactoryAttributes() {
		return factoryAttributes;
	}

	public void setFactoryAttributes(HashMap<String, Object> factoryAttributes) {
		checkState();
		this.factoryAttributes = factoryAttributes;
	}

	public HashMap<String, Object> getTransformerParameters() {
		return transformerParameters;
	}

	public void setTransformerParameters(HashMap<String, Object> transformerParameters) {
		this.transformerParameters = transformerParameters;
	}

	public HashMap<String, String> getOutputProperties() {
		return outputProperties;
	}

	public void setOutputProperties(HashMap<String, String> outputProperties) {
		this.outputProperties = outputProperties;
	}

	public URIResolver getUriResolver() {
		return uriResolver;
	}

	public void setUriResolver(URIResolver uriResolver) {
		this.uriResolver = uriResolver;
	}

}
