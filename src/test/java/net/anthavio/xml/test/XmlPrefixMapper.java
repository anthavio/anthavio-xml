/**
 * 
 */
package net.anthavio.xml.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;

import net.anthavio.xml.XmlNamespaceContext;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;


/**
 * @author vanek
 *
 */
public class XmlPrefixMapper extends NamespacePrefixMapper implements NamespaceContext {

	private XmlNamespaceContext xmlNamespaceContext;

	public XmlPrefixMapper(HashMap<String, String> uri2prefix) {
		this.xmlNamespaceContext = new XmlNamespaceContext(uri2prefix);
	}

	@Override
	public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
		String prefix = xmlNamespaceContext.getPrefix(namespaceUri);

		if (prefix != null) {
			if (requirePrefix) {
				return prefix;
			} else {
				return prefix;
			}
		} else {
			if (requirePrefix) {
				return suggestion;
			} else {
				return "";
			}
		}
		// return prefix != null && requirePrefix ? prefix : null;
	}

	public List<String> getKnownNamespaceURIs() {
		return xmlNamespaceContext.getKnownNamespaceURIs();
	}

	public String getNamespaceURI(String prefix) {
		return xmlNamespaceContext.getNamespaceURI(prefix);
	}

	public String getPrefix(String namespaceURI) {
		return xmlNamespaceContext.getPrefix(namespaceURI);
	}

	public Iterator<?> getPrefixes(String namespaceUri) {
		return xmlNamespaceContext.getPrefixes(namespaceUri);
	}
}
