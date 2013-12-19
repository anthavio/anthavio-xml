package net.anthavio.xml.validation;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import net.anthavio.util.ResourceUtil;
import net.anthavio.xml.JaxpSaxFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * @author vanek
 * 
 * Bug v xercesu znemoznuje naloadovat vice schemat najednou pomoci SchemaFactory.newSchema(Source[] sources)
 * ktere maji stejne targetNamespace. Chybne cacheuje a nahraje pouze prvni z nich.
 * https://issues.apache.org/jira/browse/XERCESJ-1130
 */
public class XmlSchemaLoader {

	private static JaxpSchemaFactory jaxpSchemaFactory;

	private static JaxpSaxFactory jaxpSaxFactory;
	static {
		jaxpSaxFactory = new JaxpSaxFactory();
		jaxpSaxFactory.setNamespaceAware(true);
		//JDK 5 JAXP parser
		jaxpSaxFactory.addFactoryFeature("http://xml.org/sax/features/namespace-prefixes", true);

		jaxpSchemaFactory = new JaxpSchemaFactory();
	}

	private final Schema schema;

	private final String name;

	public XmlSchemaLoader(String name, Schema schema) {
		this.name = name;
		this.schema = schema;
	}

	public XmlSchemaLoader(String name, URL url) {
		this(name, load(new URL[] { url }));
	}

	public XmlSchemaLoader(URL first, URL... urls) {
		if (first == null && (urls == null || urls.length == 0)) {
			throw new IllegalArgumentException("At least on url must be specified");
		}
		URL[] urls2 = new URL[urls.length + 1];
		urls2[0] = first;
		System.arraycopy(urls, 0, urls2, 1, urls.length);
		this.schema = load(urls2);
		StringBuilder sbName = new StringBuilder();
		for (int i = 0; i < urls.length; ++i) {
			String file = urls[0].getFile();
			int idxPrefix = file.lastIndexOf('/');
			if (idxPrefix == -1) {
				idxPrefix = 0;
			}
			int idxSuffix = file.lastIndexOf('.');
			if (idxSuffix == -1) {
				idxSuffix = file.length();
			}
			String name = file.substring(idxPrefix, idxSuffix);
			sbName.append(name);
			if (i > 0) {
				sbName.append("|");
			}
		}
		this.name = sbName.toString();
	}

	public Schema getSchema() {
		return schema;
	}

	public String getName() {
		return name;
	}

	public static Map<String, URL> loadUrls(URL baseUrl) {
		List<URL> urlList = ResourceUtil.list(baseUrl, ".*xsd$");
		Map<String, URL> urlMap = new HashMap<String, URL>(urlList.size());
		for (URL schemaUrl : urlList) {
			String schemaName = buildSchemaName(baseUrl, schemaUrl);
			urlMap.put(schemaName, schemaUrl);
		}
		return urlMap;
	}

	public static Map<String, Schema> loadSchemas(URL baseUrl) {
		return loadSchemas(baseUrl, jaxpSchemaFactory);
	}

	public static Map<String, Schema> loadSchemas(URL baseUrl, JaxpSchemaFactory schemaFactory) {
		List<URL> urlList = ResourceUtil.list(baseUrl, ".*xsd$");
		Map<String, Schema> schemaMap = new HashMap<String, Schema>(urlList.size());
		for (URL schemaUrl : urlList) {
			String schemaName = buildSchemaName(baseUrl, schemaUrl);
			Schema schema = load(schemaUrl, schemaFactory);
			schemaMap.put(schemaName, schema);
		}
		return schemaMap;
	}

	private static String buildSchemaName(URL baseUrl, URL schemaUrl) {
		String strBaseUrl = baseUrl.toString();
		int fromIdx = strBaseUrl.length() + 1;//remove url prefix
		int endIdx = schemaUrl.toString().lastIndexOf('.');//remove .xsd suffix
		String schemaName = schemaUrl.toString().substring(fromIdx, endIdx);
		return schemaName;
	}

	public static Schema load(String resource) {
		return load(resource, jaxpSchemaFactory);
	}

	public static Schema load(String resource, JaxpSchemaFactory schemaFactory) {
		URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
		if (url == null) {
			throw new IllegalArgumentException("Resource not found " + resource);
		}
		return load(url, schemaFactory);
	}

	public static Schema load(URL url) {
		return load(url, jaxpSchemaFactory);
	}

	public static Schema load(URL url, JaxpSchemaFactory schemaFactory) {
		if (url == null) {
			throw new IllegalArgumentException("Null url");
		}
		XMLReader xmlReader = jaxpSaxFactory.getXMLReader();
		try {
			InputSource inputSource = new InputSource(url.openStream());
			inputSource.setSystemId(getSystemId(url));
			return schemaFactory.getFactory().newSchema(new SAXSource(xmlReader, inputSource));
		} catch (Exception x) {
			throw new IllegalArgumentException("Failed to load schema from " + url, x);
		}
	}

	public static Schema load(URL[] urls) {
		Source[] schemaSources = new Source[urls.length];
		XMLReader xmlReader = jaxpSaxFactory.getXMLReader();
		for (int i = 0; i < urls.length; i++) {
			InputSource inputSource;
			try {
				inputSource = new InputSource(urls[i].openStream());
			} catch (IOException iox) {
				throw new IllegalArgumentException("Failed to load schema from " + urls[i], iox);
			}
			inputSource.setSystemId(getSystemId(urls[i]));
			schemaSources[i] = new SAXSource(xmlReader, inputSource);
		}
		SchemaFactory schemaFactory = jaxpSchemaFactory.getFactory();
		//schemaFactory.setResourceResolver(new ResourceResolver());
		try {
			return schemaFactory.newSchema(schemaSources);
		} catch (SAXException saxx) {
			throw new IllegalArgumentException("Failed to load schema", saxx);
		}

	}

	public static String getSystemId(URL url) {
		try {
			return new URI(url.toExternalForm()).toString();
		} catch (URISyntaxException e) {
			return null;
		}
	}

}
