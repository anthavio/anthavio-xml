/**
 * 
 */
package net.anthavio.xml.validation;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.validation.Schema;

/**
 * @author vanek
 *
 */
public class XmlSchemaCollection {

	private final boolean eagerLoad;

	private final URL url;

	private Map<String, URL> urlMap = new HashMap<String, URL>();

	private Map<String, Schema> schemaMap = new HashMap<String, Schema>();

	public XmlSchemaCollection(URL url, boolean eagerLoad) {
		this.eagerLoad = eagerLoad;
		this.url = url;
		if (eagerLoad) {
			schemaMap = XmlSchemaLoader.loadSchemas(url);
		} else {
			urlMap = XmlSchemaLoader.loadUrls(url);
		}
	}

	public Schema getSchema(String schemaName) {
		Schema schema;
		if (eagerLoad) {
			schema = schemaMap.get(schemaName);
			if (schema == null) {
				throw new IllegalArgumentException("Schema not found " + schemaName);
			}
		} else {
			schema = schemaMap.get(schemaName);
			if (schema == null) {
				URL url = urlMap.get(schemaName);
				if (url == null) {
					throw new IllegalArgumentException("Schema not found " + schemaName);
				}
				schema = XmlSchemaLoader.load(url);
				schemaMap.put(schemaName, schema);
			}
		}
		return schema;
	}

	@Override
	public String toString() {
		if (eagerLoad) {
			return "XmlSchemaCollection [url=" + url + ", schemas=" + schemaMap.keySet() + "]";
		} else {
			return "XmlSchemaCollection [url=" + url + ", urls=" + urlMap.keySet() + ", schemas=" + schemaMap.keySet() + "]";
		}
	}

}
