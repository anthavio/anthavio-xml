/**
 * 
 */
package net.anthavio.xml;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathVariableResolver;

/**
 * @author vanek
 *
 */
public class JaxpXpathFactory extends JaxpAbstractFactory<XPathFactory> {

	public static final String XPATH_FACTORY_JAXP = "com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl";
	public static final String XPATH_FACTORY_XALAN = "org.apache.xpath.jaxp.XPathFactoryImpl";
	public static final String XPATH_FACTORY_SAXON = "net.sf.saxon.xpath.XPathFactoryImpl";

	public enum XpathImplementation {
		JDK(XPATH_FACTORY_JAXP), XALAN(XPATH_FACTORY_XALAN), SAXON(XPATH_FACTORY_SAXON);

		private final String factoryClassName;

		private XpathImplementation(String factoryClassName) {
			this.factoryClassName = factoryClassName;
		}

		public String getFactoryClassName() {
			return factoryClassName;
		}
	}

	private HashMap<String, Boolean> factoryFeatures;

	private XPathVariableResolver variableResolver;

	private XPathFunctionResolver functionResolver;

	private NamespaceContext namespaceContext;

	public JaxpXpathFactory() {
		super();
	}

	public JaxpXpathFactory(String factoryClassName) {
		super(factoryClassName);
	}

	public JaxpXpathFactory(XpathImplementation implementation) {
		super(implementation.getFactoryClassName());
	}

	public XPath newXPath() {
		XPath xPath = getFactory().newXPath();
		if (namespaceContext != null) {
			xPath.setNamespaceContext(namespaceContext);
		}
		return xPath;
	}

	@Override
	protected XPathFactory buildDefaultFactory() {
		return XPathFactory.newInstance();
	}

	@Override
	protected void configureFactoryInstance(XPathFactory factory) {
		if (functionResolver != null) {
			factory.setXPathFunctionResolver(functionResolver);
		}
		if (variableResolver != null) {
			factory.setXPathVariableResolver(variableResolver);
		}

		try {
			if (factoryFeatures != null) {
				Set<Entry<String, Boolean>> entrySet = factoryFeatures.entrySet();
				for (Entry<String, Boolean> entry : entrySet) {
					factory.setFeature(entry.getKey(), entry.getValue());
				}
			}
		} catch (XPathFactoryConfigurationException xcx) {
			throw new JaxpConfigException(xcx);
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

	public XPathVariableResolver getVariableResolver() {
		return variableResolver;
	}

	public void setVariableResolver(XPathVariableResolver variableResolver) {
		checkState();
		this.variableResolver = variableResolver;
	}

	public XPathFunctionResolver getFunctionResolver() {
		return functionResolver;
	}

	public void setFunctionResolver(XPathFunctionResolver functionResolver) {
		checkState();
		this.functionResolver = functionResolver;
	}

	//other setters

	public NamespaceContext getNamespaceContext() {
		return namespaceContext;
	}

	public void setNamespaceContext(NamespaceContext namespaceContext) {
		this.namespaceContext = namespaceContext;
	}

}
